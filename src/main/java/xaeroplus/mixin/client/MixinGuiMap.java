package xaeroplus.mixin.client;

import baritone.api.BaritoneAPI;
import baritone.api.pathing.goals.GoalXZ;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.common.XaeroMinimapSession;
import xaero.common.misc.OptimizedMath;
import xaero.map.WorldMap;
import xaero.map.animation.SlowingAnimation;
import xaero.map.element.HoveredMapElementHolder;
import xaero.map.element.MapElementRenderHandler;
import xaero.map.gui.*;
import xaero.map.gui.dropdown.rightclick.RightClickOption;
import xaero.map.misc.Misc;
import xaeroplus.XaeroPlus;
import xaeroplus.module.ModuleManager;
import xaeroplus.module.impl.NewChunks;
import xaeroplus.module.impl.PortalSkipDetection;
import xaeroplus.module.impl.Portals;
import xaeroplus.settings.XaeroPlusSettingRegistry;
import xaeroplus.util.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static java.util.Arrays.asList;
import static java.util.Objects.isNull;
import static org.lwjgl.opengl.GL11.GL_LINE_LOOP;
import static xaero.map.gui.GuiMap.setupTextureMatricesAndTextures;
import static xaeroplus.util.ChunkUtils.getPlayerX;
import static xaeroplus.util.ChunkUtils.getPlayerZ;
import static xaeroplus.util.Globals.FOLLOW;
import static xaeroplus.util.Globals.getCurrentDimensionId;

@Mixin(value = GuiMap.class, remap = false)
public abstract class MixinGuiMap extends ScreenBase implements IRightClickableElement {

    // todo: make this gui a popout widget like waypoints
    GuiButton coordinateGotoButton;
    GuiTextField xTextEntryField;
    GuiTextField zTextEntryField;
    GuiButton followButton;
    GuiButton switchToNetherButton;
    GuiButton switchToOverworldButton;
    GuiButton switchToEndButton;

    protected MixinGuiMap(GuiScreen parent, GuiScreen escape) {
        super(parent, escape);
    }
    @Shadow
    private double cameraX;
    @Shadow
    private double cameraZ;
    @Shadow
    private int[] cameraDestination;
    @Shadow
    private SlowingAnimation cameraDestinationAnimX;
    @Shadow
    private SlowingAnimation cameraDestinationAnimZ;
    @Shadow
    private double scale;
    @Shadow
    private static double destScale;
    @Shadow
    private int lastZoomMethod;
    @Shadow
    private double prevPlayerDimDiv;
    @Shadow
    private GuiButton zoomInButton;
    @Shadow
    private GuiButton dimensionToggleButton;
    @Shadow
    public abstract void setFocused(GuiTextField field);
    @Shadow
    protected abstract void closeDropdowns();
    @Shadow
    public abstract void addGuiButton(GuiButton b);
    @Shadow
    private int rightClickX;
    @Shadow
    private int rightClickZ;


    @ModifyExpressionValue(method = "changeZoom",
        at = @At(
            value = "CONSTANT",
            args = "doubleValue=0.0625"))
    public double customMinZoom(final double original) {
        return XaeroPlusSettingRegistry.worldMapMinZoomSetting.getValue() / 10.0f;
    }

    @Inject(method = "initGui()V", at = @At(value = "TAIL"), remap = true)
    public void customInitGui(CallbackInfo ci) {
        // left side
        followButton = new GuiTexturedButton(0, this.dimensionToggleButton.y - 20 , 20, 20, FOLLOW ? 133 : 149, 16, 16, 16, WorldMap.guiTextures, new Consumer<GuiButton>() {
            @Override
            public void accept(GuiButton guiButton) {
                onFollowButton(guiButton);
            }
        }, () -> new CursorBox(new TextComponentTranslation("gui.world_map.toggle_follow_mode").appendText(" " + I18n.format(FOLLOW ? "gui.xaeroplus.off" : "gui.xaeroplus.on"))));
        addGuiButton(followButton);
        coordinateGotoButton = new GuiTexturedButton(0, followButton.y - 20 , 20, 20, 229, 16, 16, 16, WorldMap.guiTextures, new Consumer<GuiButton>() {
            @Override
            public void accept(GuiButton guiButton) {
                onGotoCoordinatesButton(guiButton);
            }
        }, () -> new CursorBox(new TextComponentTranslation("gui.world_map.go_to_coordinates")));
        addGuiButton(coordinateGotoButton);
        xTextEntryField = new GuiTextField(2, mc.fontRenderer, 20, coordinateGotoButton.y - 10, 50, 20);
        xTextEntryField.setVisible(false);
        zTextEntryField = new GuiTextField(1, mc.fontRenderer, 20, xTextEntryField.y + 20, 50, 20);
        zTextEntryField.setVisible(false);
        // right side
        this.switchToEndButton = new GuiTexturedButton(
            this.width - 20, zoomInButton.y - 20, 20, 20, 31, 0, 16, 16, Globals.xpGuiTextures, new Consumer<GuiButton>() {
            @Override
            public void accept(final GuiButton button) {
                onSwitchDimensionButton(1);
            }
        }, () -> new CursorBox(new TextComponentTranslation("setting.keybinds.switch_to_end")));
        this.switchToOverworldButton = new GuiTexturedButton(
            this.width - 20, this.switchToEndButton.y - 20, 20, 20, 16, 0, 16, 16, Globals.xpGuiTextures, new Consumer<GuiButton>() {
            @Override
            public void accept(final GuiButton button) {
                onSwitchDimensionButton(0);
            }
        }, () -> new CursorBox(new TextComponentTranslation("setting.keybinds.switch_to_overworld")));
        this.switchToNetherButton = new GuiTexturedButton(
            this.width - 20, this.switchToOverworldButton.y - 20, 20, 20, 0, 0, 16, 16, Globals.xpGuiTextures, new Consumer<GuiButton>() {
            @Override
            public void accept(final GuiButton button) {
                onSwitchDimensionButton(-1);
            }
        }, () -> new CursorBox(new TextComponentTranslation("setting.keybinds.switch_to_nether")));
        addGuiButton(switchToNetherButton);
        addGuiButton(switchToOverworldButton);
        addGuiButton(switchToEndButton);
    }

    @Inject(method = "onGuiClosed", at = @At(value = "RETURN"), remap = true)
    public void onGuiClosed(final CallbackInfo ci) {
        if (!XaeroPlusSettingRegistry.persistMapDimensionSwitchSetting.getValue()) {
            try {
                int actualDimension = ChunkUtils.getActualDimension();
                if (Globals.getCurrentDimensionId() != actualDimension) {
                    Globals.switchToDimension(actualDimension);
                }
            } catch (final Exception e) {
                XaeroPlus.LOGGER.error("Failed to switch back to original dimension", e);
            }
        }
    }

    @Redirect(method = "drawScreen", at = @At(
        value = "FIELD",
        target = "Lxaero/map/gui/GuiMap;cameraX:D",
        opcode = Opcodes.PUTFIELD,
        ordinal = 1
    ), remap = true)
    public void fixDimensionSwitchCameraCoordsX(GuiMap owner, double value , @Local(name = "playerDimDiv") double playerDimDiv) {
        this.cameraX *= prevPlayerDimDiv / playerDimDiv;
    }

    @Redirect(method = "drawScreen", at = @At(
        value = "FIELD",
        target = "Lxaero/map/gui/GuiMap;cameraZ:D",
        opcode = Opcodes.PUTFIELD,
        ordinal = 1
    ), remap = true)
    public void fixDimensionSwitchCameraCoordsZ(GuiMap owner, double value , @Local(name = "playerDimDiv") double playerDimDiv) {
        this.cameraZ *= prevPlayerDimDiv / playerDimDiv;
    }

    @Inject(method = "drawScreen", at = @At(
        value = "FIELD",
        target = "Lxaero/map/gui/GuiMap;lastStartTime:J",
        opcode = Opcodes.PUTFIELD,
        ordinal = 0
    ), remap = true)
    public void injectFollowMode(final int scaledMouseX, final int scaledMouseY, final float partialTicks, final CallbackInfo ci) {
        if (FOLLOW && isNull(this.cameraDestination) && isNull(this.cameraDestinationAnimX) && isNull(this.cameraDestinationAnimZ)) {
            this.cameraDestination = new int[]{(int) getPlayerX(), (int) getPlayerZ()};
        }
    }

    @ModifyExpressionValue(method = "drawScreen", at = @At(
        value = "FIELD",
        target = "Lxaero/map/settings/ModSettings;debug:Z",
        opcode = Opcodes.GETFIELD
    ), remap = true) // multiple field accesses
    public boolean hideDebugRenderingOnF1(boolean original) {
        return original && !mc.gameSettings.hideGUI;
    }

    @WrapWithCondition(method = "drawScreen", at = @At(
        value = "INVOKE",
        target = "Lxaero/map/graphics/MapRenderHelper;renderDynamicHighlight(IIIIIIFFFFFFFF)V"
    ))
    public boolean hideHighlightsOnF1(int flooredCameraX, int flooredCameraZ, int leftX, int rightX, int topZ, int bottomZ, float sideR, float sideG, float sideB, float sideA, float centerR, float centerG, float centerB, float centerA) {
        return !mc.gameSettings.hideGUI;
    }

    @WrapOperation(method = "drawScreen", at = @At(
        value = "INVOKE",
        target = "Lxaero/map/element/MapElementRenderHandler;render(Lxaero/map/gui/GuiMap;DDIIDDDDDFZLxaero/map/element/HoveredMapElementHolder;Lnet/minecraft/client/Minecraft;FLnet/minecraft/client/gui/ScaledResolution;)Lxaero/map/element/HoveredMapElementHolder;"
    ), remap = true)
    public HoveredMapElementHolder<?, ?> hideMapElementsOnF1(final MapElementRenderHandler instance, GuiMap mapScreen, double cameraX, double cameraZ, int width, int height, double screenSizeBasedScale, double scale, double playerDimDiv, double mouseX, double mouseZ, float brightness, boolean cave, HoveredMapElementHolder<?, ?> oldHovered, Minecraft mc, float partialTicks, ScaledResolution scaledRes, final Operation<HoveredMapElementHolder<?, ?>> original) {
        if (!mc.gameSettings.hideGUI) {
            return original.call(instance, mapScreen, cameraX, cameraZ, width, height, screenSizeBasedScale, scale, playerDimDiv, mouseX, mouseZ, brightness, cave, oldHovered, mc, partialTicks, scaledRes);
        } else {
            return null;
        }
    }

    @ModifyExpressionValue(method = "drawScreen", at = @At(
        value = "FIELD",
        target = "Lxaero/map/settings/ModSettings;footsteps:Z",
        opcode = Opcodes.GETFIELD
    ), remap = true)
    public boolean hideFootstepsOnF1(boolean original) {
        return original && !mc.gameSettings.hideGUI;
    }

    @ModifyExpressionValue(method = "drawScreen", at = @At(
        value = "FIELD",
        target = "Lxaero/map/settings/ModSettings;renderArrow:Z",
        opcode = Opcodes.GETFIELD
    ), remap = true)
    public boolean hideArrowOnF1(boolean original) {
        return original && !mc.gameSettings.hideGUI;
    }

    @WrapWithCondition(method = "drawScreen", at = @At(
        value = "INVOKE",
        target = "Lxaero/map/graphics/MapRenderHelper;drawCenteredStringWithBackground(Lnet/minecraft/client/gui/FontRenderer;Ljava/lang/String;IIIFFFF)V"
    ), remap = true)
    public boolean hideRenderedStringsOnF1(FontRenderer font, String string, int x, int y, int color, float bgRed, float bgGreen, float bgBlue, float bgAlpha) {
        return !mc.gameSettings.hideGUI;
    }

    @WrapWithCondition(method = "drawScreen", at = @At(
        value = "INVOKE",
        target = "Lxaero/map/graphics/MapRenderHelper;drawCenteredStringWithBackground(Lnet/minecraft/client/gui/FontRenderer;Lnet/minecraft/util/text/ITextComponent;IIIFFFF)V"
    ), remap = true)
    public boolean hideMoreRenderedStringsOnF1(FontRenderer font, ITextComponent text, int x, int y, int color, float bgRed, float bgGreen, float bgBlue, float bgAlpha) {
        return !mc.gameSettings.hideGUI;
    }

    @WrapWithCondition(method = "drawScreen", at = @At(
        value = "INVOKE",
        target = "Lxaero/map/gui/GuiMap;drawTexturedModalRect(IIIIII)V"
    ), remap = true)
    public boolean hideCompassOnF1(GuiMap instance, int x, int y, int textureX, int textureY, int width, int height) {
        return !mc.gameSettings.hideGUI;
    }

    @Inject(method = "drawScreen", at = @At(
        value = "INVOKE",
        target = "Lxaero/map/gui/ScreenBase;drawScreen(IIF)V"
    ), remap = true)
    public void hideButtonsOnF1(int mouseX, int mouseY, float partial, CallbackInfo ci) {
        if (mc.gameSettings.hideGUI) {
            List<GuiButton> buttonList = this.buttonList;
            if (!buttonList.isEmpty()) {
                Globals.guiMapButtonTempList.clear();
                Globals.guiMapButtonTempList.addAll(buttonList);
                xTextEntryField.setVisible(false);
                zTextEntryField.setVisible(false);
                buttonList.clear();
            }
        } else {
            if (!Globals.guiMapButtonTempList.isEmpty()) {
                buttonList.addAll(Globals.guiMapButtonTempList);
                Globals.guiMapButtonTempList.clear();
            }
        }
    }

    @Inject(method = "drawScreen",
        at = @At(
            value = "FIELD",
            target = "Lxaero/map/settings/ModSettings;debug:Z",
            ordinal = 2
        ), remap = true)
    public void drawWorldMapFeatures(int scaledMouseX, int scaledMouseY, float partialTicks, CallbackInfo ci,
                                     @Local(name = "leafRegionMinX") int leafRegionMinX,
                                     @Local(name = "leafRegionMinZ") int leafRegionMinZ,
                                     @Local(name = "leveledSideInRegions") int leveledSideInRegions,
                                     @Local(name = "flooredCameraX") int flooredCameraX,
                                     @Local(name = "flooredCameraZ") int flooredCameraZ,
                                     @Local(name = "brightness") float brightness
    ){
        GuiMap.restoreTextureStates();
        if (XaeroPlusSettingRegistry.newChunksEnabledSetting.getValue() && !mc.gameSettings.hideGUI) {
            final NewChunks newChunks = ModuleManager.getModule(NewChunks.class);
            GuiHelper.drawHighlightAtChunkPosList(newChunks.getNewChunksInRegion(leafRegionMinX, leafRegionMinZ, leveledSideInRegions, getCurrentDimensionId()),
                                                  flooredCameraX,
                                                  flooredCameraZ,
                                                  newChunks.getNewChunksColor());
        }
        if (XaeroPlusSettingRegistry.portalSkipDetectionEnabledSetting.getValue() && !mc.gameSettings.hideGUI && XaeroPlusSettingRegistry.newChunksEnabledSetting.getValue()) {
            final PortalSkipDetection portalSkipDetection = ModuleManager.getModule(PortalSkipDetection.class);
            GuiHelper.drawHighlightAtChunkPosList(portalSkipDetection.getPortalSkipChunksInRegion(leafRegionMinX, leafRegionMinZ, leveledSideInRegions),
                                                  flooredCameraX,
                                                  flooredCameraZ,
                                                  portalSkipDetection.getPortalSkipChunksColor());
        }
        if (XaeroPlusSettingRegistry.portalsEnabledSetting.getValue() && !mc.gameSettings.hideGUI) {
            final Portals portals = ModuleManager.getModule(Portals.class);
            GuiHelper.drawHighlightAtChunkPosList(portals.getPortalsInRegion(leafRegionMinX, leafRegionMinZ, leveledSideInRegions, getCurrentDimensionId()),
                                                  flooredCameraX,
                                                  flooredCameraZ,
                                                  portals.getPortalsColor());
        }
        final boolean isDimensionSwitched = Globals.getCurrentDimensionId() != mc.player.dimension;
        if (XaeroPlusSettingRegistry.wdlEnabledSetting.getValue()
            && !mc.gameSettings.hideGUI
            && WDLHelper.isWdlPresent()
            && WDLHelper.isDownloading()
            && !isDimensionSwitched) {

            GuiHelper.drawHighlightAtChunkPosList(WDLHelper.getSavedChunksInRegion(leafRegionMinX, leafRegionMinZ, leveledSideInRegions),
                                                  flooredCameraX,
                                                  flooredCameraZ,
                                                  WDLHelper.getWdlColor());
        }
        GlStateManager.disableBlend();
        setupTextureMatricesAndTextures(brightness);
    }

    @Inject(method = "drawScreen", at = @At(
        value = "FIELD",
        target = "Lxaero/map/settings/ModSettings;renderArrow:Z",
        opcode = Opcodes.GETFIELD,
        ordinal = 0
    ), remap = true)
    public void showRenderDistanceWorldMap(int scaledMouseX, int scaledMouseY, float partialTicks, CallbackInfo ci,
                                           @Local(name = "scaledPlayerX") double scaledPlayerX,
                                           @Local(name = "scaledPlayerZ") double scaledPlayerZ,
                                           @Local(name = "flooredCameraX") int flooredCameraX,
                                           @Local(name = "flooredCameraZ") int flooredCameraZ
    ){
        final boolean isDimensionSwitched = Globals.getCurrentDimensionId() != mc.player.dimension;
        if (XaeroPlusSettingRegistry.showRenderDistanceWorldMapSetting.getValue() && !mc.gameSettings.hideGUI && !isDimensionSwitched) {
            final int setting = (int) XaeroPlusSettingRegistry.assumedServerRenderDistanceSetting.getValue();
            int width = setting * 2 + 1;
            int xFloored = OptimizedMath.myFloor(scaledPlayerX);
            int zFloored = OptimizedMath.myFloor(scaledPlayerZ);
            int chunkLeftX = (xFloored >> 4) - (width / 2) << 4;
            int chunkRightX = (xFloored >> 4) + 1 + (width / 2) << 4;
            int chunkTopZ = (zFloored >> 4) - (width / 2) << 4;
            int chunkBottomZ = (zFloored >> 4) + 1 + (width / 2) << 4;
            final int x0 = chunkLeftX - flooredCameraX;
            final int x1 = chunkRightX - flooredCameraX;
            final int z0 = chunkTopZ - flooredCameraZ;
            final int z1 = chunkBottomZ - flooredCameraZ;

            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder vertexBuffer = tessellator.getBuffer();
            vertexBuffer.begin(GL_LINE_LOOP, DefaultVertexFormats.POSITION);
            GlStateManager.disableTexture2D();
            GlStateManager.enableBlend();
            // yellow
            GlStateManager.color(1.f, 1.f, 0.f, 0.8F);
            float settingWidth = (float) XaeroMinimapSession.getCurrentSession().getModMain().getSettings().chunkGridLineWidth;
            float lineScale = (float) Math.min(settingWidth * this.scale, settingWidth);
            GlStateManager.glLineWidth(lineScale);
            vertexBuffer.pos(x0, z0, 0.0).endVertex();
            vertexBuffer.pos(x1, z0, 0.0).endVertex();
            vertexBuffer.pos(x1, z1, 0.0).endVertex();
            vertexBuffer.pos(x0, z1, 0.0).endVertex();
            tessellator.draw();
            GlStateManager.enableTexture2D();
        }
    }


    @Inject(method = "drawScreen", at = @At(
        value = "TAIL"
    ))
    public void insertCoordinatesGoToButton(int scaledMouseX, int scaledMouseY, float partialTicks, CallbackInfo ci) {
        if (mc.currentScreen != null && mc.currentScreen.getClass().equals(GuiMap.class) && xTextEntryField.getVisible() && zTextEntryField.getVisible()) {
            if (xTextEntryField.getText().isEmpty() && !this.xTextEntryField.isFocused()) {
                Misc.setFieldText(xTextEntryField, "X:", -11184811);
            }
            xTextEntryField.drawTextBox();
            if (zTextEntryField.getText().isEmpty() && !this.zTextEntryField.isFocused()) {
                Misc.setFieldText(zTextEntryField, "Z:", -11184811);
            }
            zTextEntryField.drawTextBox();
        }
    }

    @Inject(method = "mouseClicked(III)V", at = @At(value = "TAIL"), remap = true)
    public void mouseClicked(int x, int y, int button, CallbackInfo ci) throws IOException {
        if (button == 0) {
            handleTextFieldClick(x, y, xTextEntryField);
            handleTextFieldClick(x, y, zTextEntryField);
            if (!xTextEntryField.isFocused() && !zTextEntryField.isFocused()) {
                xTextEntryField.setVisible(false);
                zTextEntryField.setVisible(false);
            }
        }
    }

    @Inject(method = "getRightClickOptions", at = @At(value = "RETURN"), remap = false)
    public void getRightClickOptionsInject(final CallbackInfoReturnable<ArrayList<RightClickOption>> cir) {
        if (BaritoneHelper.isBaritonePresent()) {
            final ArrayList<RightClickOption> options = cir.getReturnValue();
            options.addAll(3, asList(
                    new RightClickOption(I18n.format("gui.world_map.baritone_goal_here"), options.size(), this) {
                        @Override
                        public void onAction(GuiScreen screen) {
                            BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoal(new GoalXZ(rightClickX, rightClickZ));
                        }
                    },
                    new RightClickOption(I18n.format("gui.world_map.baritone_path_here"), options.size(), this) {
                        @Override
                        public void onAction(GuiScreen screen) {
                            BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalXZ(rightClickX, rightClickZ));
                        }
                    }
            ));
        }
    }

    private void handleTextFieldClick(int x, int y, GuiTextField guiTextField) {
        if ((x >= guiTextField.x && x <= guiTextField.x + guiTextField.width) && (y >= guiTextField.y && y <= guiTextField.y + xTextEntryField.height)) {
            guiTextField.setFocused(true);
            onTextFieldFocus(guiTextField);
            this.setFocused(guiTextField);
            guiTextField.mouseClicked(x, y, 0);
        } else {
            guiTextField.setFocused(false);
        }
    }

    @Inject(method = "keyTyped(CI)V", at = @At(value = "INVOKE", target = "Lxaero/map/gui/ScreenBase;keyTyped(CI)V", shift = At.Shift.AFTER), remap = true, cancellable = true)
    public void keyTyped(char typedChar, int keyCode, CallbackInfo ci) throws IOException {
        if (keyCode == 59) {
            mc.gameSettings.hideGUI = !mc.gameSettings.hideGUI;
            ci.cancel();
        } else if (keyCode == 15) {
            if (xTextEntryField.isFocused()) {
                xTextEntryField.setFocused(false);
                zTextEntryField.setFocused(true);
                this.setFocused(zTextEntryField);
                this.onTextFieldFocus(zTextEntryField);
                ci.cancel();
            } else if (zTextEntryField.isFocused()) {
                zTextEntryField.setFocused(false);
                xTextEntryField.setFocused(true);
                this.setFocused(xTextEntryField);
                this.onTextFieldFocus(xTextEntryField);
                ci.cancel();
            }
        } else if (keyCode == 28) {
            if (xTextEntryField.isFocused() || zTextEntryField.isFocused()) {
                this.onGotoCoordinatesButton(null);
                ci.cancel();
            }
        }
    }

    public void onGotoCoordinatesButton(final GuiButton b) {
        if (xTextEntryField.getVisible() && zTextEntryField.getVisible()) {
            try {
                int x = Integer.parseInt(xTextEntryField.getText());
                int z = Integer.parseInt(zTextEntryField.getText());
                cameraX = x;
                cameraZ = z;
                FOLLOW = false;
                this.setWorldAndResolution(this.mc, width, height);
            } catch (final NumberFormatException e) {
                xTextEntryField.setText("");
                zTextEntryField.setText("");
                WorldMap.LOGGER.warn("Go to coordinates failed" , e);
            }
        } else {
            xTextEntryField.setVisible(true);
            zTextEntryField.setVisible(true);
            xTextEntryField.setFocused(true);
            this.setFocused(xTextEntryField);
            this.onTextFieldFocus(xTextEntryField);
        }
    }

    private void onTextFieldFocus(GuiTextField guiTextField) {
        if (guiTextField.getText().startsWith("X:") || guiTextField.getText().startsWith("Z:")) {
            guiTextField.setText("");
            guiTextField.setTextColor(14737632);
        }
    }

    public void onFollowButton(final GuiButton b) {
        FOLLOW = !FOLLOW;
        this.setWorldAndResolution(this.mc, width, height);
    }

    private void onSwitchDimensionButton(final int newDimId) {
        Globals.switchToDimension(newDimId);
    }
}
