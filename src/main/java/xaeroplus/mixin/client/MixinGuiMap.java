package xaeroplus.mixin.client;

import baritone.api.BaritoneAPI;
import baritone.api.pathing.goals.GoalXZ;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.common.XaeroMinimapSession;
import xaero.common.graphics.shader.MinimapShaders;
import xaero.map.MapProcessor;
import xaero.map.WorldMap;
import xaero.map.animation.SlowingAnimation;
import xaero.map.element.HoveredMapElementHolder;
import xaero.map.element.MapElementRenderHandler;
import xaero.map.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider;
import xaero.map.gui.*;
import xaero.map.gui.dropdown.rightclick.RightClickOption;
import xaero.map.misc.OptimizedMath;
import xaero.map.mods.SupportMods;
import xaero.map.world.MapDimension;
import xaeroplus.Globals;
import xaeroplus.XaeroPlus;
import xaeroplus.mixin.client.mc.AccessorGameOptions;
import xaeroplus.settings.XaeroPlusSettingRegistry;
import xaeroplus.util.BaritoneHelper;
import xaeroplus.util.ChunkUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.Objects.isNull;
import static net.minecraft.world.World.*;
import static xaeroplus.Globals.FOLLOW;
import static xaeroplus.util.ChunkUtils.getPlayerX;
import static xaeroplus.util.ChunkUtils.getPlayerZ;

@Mixin(value = GuiMap.class, remap = false)
public abstract class MixinGuiMap extends ScreenBase implements IRightClickableElement {

    ButtonWidget coordinateGotoButton;
    TextFieldWidget xTextEntryField;
    TextFieldWidget zTextEntryField;
    ButtonWidget followButton;
    ButtonWidget switchToNetherButton;
    ButtonWidget switchToOverworldButton;
    ButtonWidget switchToEndButton;
    @Shadow
    private double cameraX = 0.0;
    @Shadow
    private double cameraZ = 0.0;
    @Shadow
    private int[] cameraDestination = null;
    @Shadow
    private SlowingAnimation cameraDestinationAnimX = null;
    @Shadow
    private SlowingAnimation cameraDestinationAnimZ = null;
    @Shadow
    private double scale;
    @Shadow
    private double prevPlayerDimDiv;
    @Shadow
    private MapProcessor mapProcessor;
    @Shadow
    private ButtonWidget exportButton;
    @Shadow
    private ButtonWidget claimsButton;
    @Shadow
    private ButtonWidget zoomInButton;
    @Shadow
    private ButtonWidget zoomOutButton;
    @Shadow
    private ButtonWidget keybindingsButton;
    @Shadow
    private ButtonWidget dimensionToggleButton;
    @Shadow
    private int rightClickX;
    @Shadow
    private int rightClickZ;

    protected MixinGuiMap(final Screen parent, final Screen escape, final Text titleIn) {
        super(parent, escape, titleIn);
    }

    @Shadow public abstract <T extends Element & Drawable & Selectable> T addButton(final T guiEventListener);

    @Shadow public abstract <T extends Element & Selectable> T addSelectableChild(final T guiEventListener);

    @ModifyConstant(method = "changeZoom", constant = @Constant(doubleValue = 0.0625))
    public double customMinZoom(final double original) {
        return XaeroPlusSettingRegistry.worldMapMinZoomSetting.getValue() / 10.0f;
    }

    @Inject(method = "init", at = @At(value = "RETURN"), remap = true)
    public void customInitGui(CallbackInfo ci) {
        // left side
        followButton = new GuiTexturedButton(0, this.dimensionToggleButton.getY() - 20, 20, 20, FOLLOW ? 133 : 149, 16, 16, 16,
                                             WorldMap.guiTextures,
                                             this::onFollowButton,
                                             () -> new CursorBox(Text.translatable("gui.world_map.toggle_follow_mode")
                                                                         .append(" " + I18n.translate(FOLLOW ? "gui.xaeroplus.off" : "gui.xaeroplus.on"))));
        addButton(followButton);
        coordinateGotoButton = new GuiTexturedButton(0, followButton.getY() - 20 , 20, 20, 229, 16, 16, 16,
                                                     WorldMap.guiTextures,
                                                     this::onGotoCoordinatesButton,
                                                     () -> new CursorBox(Text.translatable("gui.world_map.go_to_coordinates")));
        addButton(coordinateGotoButton);
        xTextEntryField = new TextFieldWidget(MinecraftClient.getInstance().textRenderer, 20, coordinateGotoButton.getY() - 10, 50, 20, Text.of("X:"));
        xTextEntryField.setVisible(false);
        xTextEntryField.setSelectionStart(0);
        xTextEntryField.setPlaceholder(Text.literal("X:").formatted(Formatting.DARK_GRAY));
        zTextEntryField = new TextFieldWidget(MinecraftClient.getInstance().textRenderer, 20, xTextEntryField.getY() + 20, 50, 20, Text.of("Z:"));
        zTextEntryField.setVisible(false);
        zTextEntryField.setSelectionStart(0);
        zTextEntryField.setPlaceholder(Text.literal("Z:").formatted(Formatting.DARK_GRAY));
        this.addSelectableChild(xTextEntryField);
        this.addSelectableChild(zTextEntryField);
        // right side
        if (!SupportMods.pac()) {  // remove useless button when pac is not installed
            this.remove(this.claimsButton);
            this.exportButton.setY(this.claimsButton.getY());
            this.keybindingsButton.setY(this.claimsButton.getY() - 20);
            this.zoomOutButton.setY(this.keybindingsButton.getY() - 20);
            this.zoomInButton.setY(this.zoomOutButton.getY() - 20);
        }
        switchToEndButton = new GuiTexturedButton(this.width - 20, zoomInButton.getY() - 20, 20, 20, 31, 0, 16, 16,
                                                  Globals.xpGuiTextures,
                                                  (button -> onSwitchDimensionButton(END)),
                                                  () -> new CursorBox(Text.translatable("setting.keybinds.switch_to_end")));
        switchToOverworldButton = new GuiTexturedButton(this.width - 20, this.switchToEndButton.getY() - 20, 20, 20, 16, 0, 16, 16,
                                                        Globals.xpGuiTextures,
                                                        (button -> onSwitchDimensionButton(OVERWORLD)),
                                                        () -> new CursorBox(Text.translatable("setting.keybinds.switch_to_overworld")));
        switchToNetherButton = new GuiTexturedButton(this.width - 20, this.switchToOverworldButton.getY() - 20, 20, 20, 0, 0, 16, 16,
                                                     Globals.xpGuiTextures,
                                                     (button -> onSwitchDimensionButton(NETHER)),
                                                     () -> new CursorBox(Text.translatable("setting.keybinds.switch_to_nether")));
        addButton(switchToEndButton);
        addButton(switchToOverworldButton);
        addButton(switchToNetherButton);
    }

    @Override
    protected void onExit(Screen screen) {
        if (!XaeroPlusSettingRegistry.persistMapDimensionSwitchSetting.getValue()) {
            try {
                var actualDimension = ChunkUtils.getActualDimension();
                if (Globals.getCurrentDimensionId() != actualDimension) {
                    Globals.switchToDimension(actualDimension);
                    if (!XaeroPlusSettingRegistry.radarWhileDimensionSwitchedSetting.getValue()) {
                        WorldMap.settings.minimapRadar = true;
                    }
                }
            } catch (final Exception e) {
                XaeroPlus.LOGGER.error("Failed to switch back to original dimension", e);
            }
        }
        super.onExit(screen);
    }

    @Inject(method = "render", at = @At(
        value = "INVOKE",
        target = "Lxaero/map/gui/GuiMap;init(Lnet/minecraft/client/MinecraftClient;II)V",
        ordinal = 0,
        shift = At.Shift.AFTER
    ), remap = true)
    public void toggleRadarWhileDimensionSwitched(final DrawContext guiGraphics, final int scaledMouseX, final int scaledMouseY, final float partialTicks, final CallbackInfo ci, @Local(name = "currentFutureDim") MapDimension currentFutureDim) {
        if (!XaeroPlusSettingRegistry.radarWhileDimensionSwitchedSetting.getValue()) {
            if (currentFutureDim.getDimId() != ChunkUtils.getActualDimension())
                WorldMap.settings.minimapRadar = false;
            else WorldMap.settings.minimapRadar = true;
        }
    }

    @Redirect(method = "render", at = @At(value = "FIELD", target = "Lxaero/map/gui/GuiMap;cameraX:D", opcode = Opcodes.PUTFIELD, ordinal = 1), remap = true)
    public void fixDimensionSwitchCameraCoordsX(GuiMap owner, double value , @Local(name = "playerDimDiv") double playerDimDiv) {
        this.cameraX *= prevPlayerDimDiv / playerDimDiv;
    }

    @Redirect(method = "render", at = @At(value = "FIELD", target = "Lxaero/map/gui/GuiMap;cameraZ:D", opcode = Opcodes.PUTFIELD, ordinal = 1), remap = true)
    public void fixDimensionSwitchCameraCoordsZ(GuiMap owner, double value, @Local(name = "playerDimDiv") double playerDimDiv) {
        this.cameraZ *= prevPlayerDimDiv / playerDimDiv;
    }

    @Inject(method = "render", at = @At(value = "FIELD", target = "Lxaero/map/gui/GuiMap;lastStartTime:J", opcode = Opcodes.PUTFIELD, ordinal = 0, shift = At.Shift.AFTER), remap = true)
    public void injectFollowMode(final DrawContext guiGraphics, final int scaledMouseX, final int scaledMouseY, final float partialTicks, final CallbackInfo ci) {
        if (FOLLOW && isNull(this.cameraDestination) && isNull(this.cameraDestinationAnimX) && isNull(this.cameraDestinationAnimZ)) {
            this.cameraDestination = new int[]{(int) getPlayerX(), (int) getPlayerZ()};
        }
    }

    @ModifyExpressionValue(method = "render", at = @At(
        value = "FIELD",
        target = "Lxaero/map/settings/ModSettings;debug:Z",
        opcode = Opcodes.GETFIELD
    ), remap = true) // multiple field accesses
    public boolean hideDebugRenderingOnF1(boolean original) {
        return original && !MinecraftClient.getInstance().options.hudHidden;
    }

    // todo: re-benchmark region load perf impact
//    @ModifyArg(method = "render", at = @At(
//        value = "INVOKE",
//        target = "Lxaero/map/misc/Misc;addToListOfSmallest(ILjava/util/List;Ljava/lang/Comparable;)V"),
//        index = 0, remap = true)
//    public int increaseRegionBuffer(final int i, final List list, final Comparable element) {
//        return 100;
//    }

    @Inject(method = "render", at = @At(
        value = "INVOKE",
        target = "Lxaero/map/region/LeveledRegion;loadingAnimation()Z",
        shift = At.Shift.BEFORE
    ), remap = true)
    public void drawWorldMapFeatures(final DrawContext guiGraphics, final int scaledMouseX, final int scaledMouseY, final float partialTicks, final CallbackInfo ci,
                                     @Local(name = "leafRegionMinX") int leafRegionMinX,
                                     @Local(name = "leafRegionMinZ") int leafRegionMinZ,
                                     @Local(name = "leveledSideInRegions") int leveledSideInRegions,
                                     @Local(name = "flooredCameraX") int flooredCameraX,
                                     @Local(name = "flooredCameraZ") int flooredCameraZ,
                                     @Local(name = "matrixStack") MatrixStack matrixStack,
                                     @Local(name = "overlayBuffer") VertexConsumer overlayBuffer) {
        if (!MinecraftClient.getInstance().options.hudHidden)
            Globals.drawManager.drawWorldMapFeatures(leafRegionMinX,
                                                     leafRegionMinZ,
                                                     leveledSideInRegions,
                                                     flooredCameraX,
                                                     flooredCameraZ,
                                                     matrixStack,
                                                     overlayBuffer);
    }

//    @ModifyConstant(method = "render", constant = @Constant(intValue = 16, ordinal = 0))
//    public int increaseRequestedRegionsLimit(final int original) {
//        return 64;
//    }
//
    // todo: re-benchmark region load perf impact
//    @Inject(method = "render", at = @At(
//        value = "FIELD",
//        target = "Lxaero/map/settings/ModSettings;pauseRequests:Z",
//        opcode = Opcodes.GETFIELD,
//        shift = At.Shift.BY,
//        by = 4,
//        ordinal = 0
//    ))
//    public void increaseRequestedRegions(final DrawContext guiGraphics, final int scaledMouseX, final int scaledMouseY, final float partialTicks, final CallbackInfo ci,
//                                         @Local(name = "toRequest") LocalIntRef toRequest) {
//        toRequest.set(8);
//    }

    @WrapWithCondition(method = "render", at = @At(
        value = "INVOKE",
        target = "Lxaero/map/graphics/MapRenderHelper;renderDynamicHighlight(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;IIIIIIFFFFFFFF)V"
    ), remap = true)
    public boolean hideHighlightsOnF1(final MatrixStack matrixStack, final VertexConsumer overlayBuffer, final int flooredCameraX, final int flooredCameraZ, final int leftX, final int rightX, final int topZ, final int bottomZ, final float sideR, final float sideG, final float sideB, final float sideA, final float centerR, final float centerG, final float centerB, final float centerA) {
        return !MinecraftClient.getInstance().options.hudHidden;
    }

    @WrapOperation(method = "render", at = @At(
        value = "INVOKE",
        target = "Lxaero/map/element/MapElementRenderHandler;render(Lxaero/map/gui/GuiMap;Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lxaero/map/graphics/renderer/multitexture/MultiTextureRenderTypeRendererProvider;DDIIDDDDDFZLxaero/map/element/HoveredMapElementHolder;Lnet/minecraft/client/MinecraftClient;F)Lxaero/map/element/HoveredMapElementHolder;"
    ), remap = true)
    public HoveredMapElementHolder<?, ?> hideMapElementsOnF1(MapElementRenderHandler handler, GuiMap mapScreen, DrawContext guiGraphics, VertexConsumerProvider.Immediate renderTypeBuffers, MultiTextureRenderTypeRendererProvider rendererProvider, double cameraX, double cameraZ, int width, int height, double screenSizeBasedScale, double scale, double playerDimDiv, double mouseX, double mouseZ, float brightness, boolean cave, HoveredMapElementHolder<?, ?> oldHovered, MinecraftClient mc, float partialTicks, Operation<HoveredMapElementHolder<?, ?>> original) {
        if (!MinecraftClient.getInstance().options.hudHidden) {
            return original.call(handler, mapScreen, guiGraphics, renderTypeBuffers, rendererProvider, cameraX, cameraZ, width, height, screenSizeBasedScale, scale, playerDimDiv, mouseX, mouseZ, brightness, cave, oldHovered, mc, partialTicks);
        } else {
            return null;
        }
    }

    @ModifyExpressionValue(method = "render", at = @At(
        value = "FIELD",
        target = "Lxaero/map/settings/ModSettings;footsteps:Z",
        opcode = Opcodes.GETFIELD
    ), remap = true)
    public boolean hideFootstepsOnF1(boolean original) {
        return original && !MinecraftClient.getInstance().options.hudHidden;
    }

    @ModifyExpressionValue(method = "render", at = @At(
        value = "FIELD",
        target = "Lxaero/map/settings/ModSettings;renderArrow:Z",
        opcode = Opcodes.GETFIELD
    ), remap = true)
    public boolean hideArrowOnF1(boolean original) {
        return original && !MinecraftClient.getInstance().options.hudHidden;
    }

    @Inject(method = "render", at = @At(
        value = "FIELD",
        target = "Lxaero/map/settings/ModSettings;renderArrow:Z",
        opcode = Opcodes.GETFIELD,
        shift = At.Shift.BEFORE
    ), remap = true)
    public void showRenderDistanceWorldMap(final DrawContext guiGraphics, final int scaledMouseX, final int scaledMouseY, final float partialTicks, final CallbackInfo ci,
                                           @Local(name = "flooredCameraX") int flooredCameraX,
                                           @Local(name = "flooredCameraZ") int flooredCameraZ,
                                           @Local(name = "renderTypeBuffers") VertexConsumerProvider.Immediate renderTypeBuffers,
                                           @Local(name = "matrixStack") MatrixStack matrixStack) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (XaeroPlusSettingRegistry.showRenderDistanceWorldMapSetting.getValue() && !mc.options.hudHidden) {
            if (mc.world.getRegistryKey() == Globals.getCurrentDimensionId()) {
                final int viewDistance = ((AccessorGameOptions) mc.options).getServerViewDistance();
                int width = viewDistance * 2 + 1;
                double playerX = getPlayerX();
                double playerZ = getPlayerZ();
                int xFloored = OptimizedMath.myFloor(playerX);
                int zFloored = OptimizedMath.myFloor(playerZ);
                int chunkLeftX = (xFloored >> 4) - (width / 2) << 4;
                int chunkRightX = (xFloored >> 4) + 1 + (width / 2) << 4;
                int chunkTopZ = (zFloored >> 4) - (width / 2) << 4;
                int chunkBottomZ = (zFloored >> 4) + 1 + (width / 2) << 4;
                final int x0 = chunkLeftX - flooredCameraX;
                final int x1 = chunkRightX - flooredCameraX;
                final int z0 = chunkTopZ - flooredCameraZ;
                final int z1 = chunkBottomZ - flooredCameraZ;
                VertexConsumer lineBufferBuilder = renderTypeBuffers.getBuffer(xaero.common.graphics.CustomRenderTypes.MAP_LINES);
                MatrixStack.Entry matrices = matrixStack.peek();
                MinimapShaders.FRAMEBUFFER_LINES.setFrameSize(mc.getWindow().getFramebufferWidth(),
                                                              mc.getWindow().getFramebufferHeight());

                float settingWidth = (float) XaeroMinimapSession.getCurrentSession()
                    .getModMain()
                    .getSettings().chunkGridLineWidth;
                float lineScale = (float) Math.min(settingWidth * this.scale, settingWidth);
                RenderSystem.lineWidth(lineScale);

                // todo: horizontal lines seem to have a smaller width here for some reason
                //  also there's some jittering to the position noticeable when you zoom in
                addColoredLineToExistingBuffer(
                    matrices,
                    lineBufferBuilder,
                    x0,
                    z0,
                    x1,
                    z0,
                    1.0f,
                    1.0f,
                    0.0f,
                    0.8f
                );
                addColoredLineToExistingBuffer(
                    matrices,
                    lineBufferBuilder,
                    x1,
                    z0,
                    x1,
                    z1,
                    1.0f,
                    1.0f,
                    0.0f,
                    0.8f
                );
                addColoredLineToExistingBuffer(
                    matrices,
                    lineBufferBuilder,
                    x1,
                    z1,
                    x0,
                    z1,
                    1.0f,
                    1.0f,
                    0.0f,
                    0.8f
                );
                addColoredLineToExistingBuffer(
                    matrices,
                    lineBufferBuilder,
                    x0,
                    z0,
                    x0,
                    z1,
                    1.0f,
                    1.0f,
                    0.0f,
                    0.8f
                );
            }
        }
    }

    @WrapWithCondition(method = "render", at = @At(
        value = "INVOKE",
        target = "Lxaero/map/graphics/MapRenderHelper;drawCenteredStringWithBackground(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;IIIFFFFLnet/minecraft/client/render/VertexConsumer;)V"
    ), remap = true)
    public boolean hideRenderedStringsOnF1(final DrawContext guiGraphics, final TextRenderer font, final Text text, final int x, final int y, final int color, final float bgRed, final float bgGreen, final float bgBlue, final float bgAlpha, final VertexConsumer backgroundVertexBuffer) {
        return !MinecraftClient.getInstance().options.hudHidden;
    }

    @WrapWithCondition(method = "render", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/client/gui/DrawContext;drawTexture(Lnet/minecraft/util/Identifier;IIIIII)V"
    ), remap = true)
    public boolean hideCompassOnF1(final DrawContext instance, final Identifier texture, final int x, final int y, final int u, final int v, final int width, final int height) {
        return !MinecraftClient.getInstance().options.hudHidden;
    }

    @Inject(method = "render", at = @At(
        value = "INVOKE",
        target = "Lxaero/map/gui/ScreenBase;render(Lnet/minecraft/client/gui/DrawContext;IIF)V"
    ), remap = true)
    public void hideButtonsOnF1(final DrawContext guiGraphics, final int scaledMouseX, final int scaledMouseY, final float partialTicks, final CallbackInfo ci) {
        if (MinecraftClient.getInstance().options.hudHidden) {
            List<ButtonWidget> buttonList = getButtonList();
            if (!buttonList.isEmpty()) {
                Globals.guiMapButtonTempList.clear();
                Globals.guiMapButtonTempList.addAll(buttonList);
                xTextEntryField.setVisible(false);
                zTextEntryField.setVisible(false);
                clearButtons();
            }
        } else {
            if (!Globals.guiMapButtonTempList.isEmpty()) {
                clearButtons();
                Globals.guiMapButtonTempList.forEach(this::addButton);
                Globals.guiMapButtonTempList.clear();
            }
        }
    }

    @Inject(method = "render", at = @At(
        value = "INVOKE",
        target = "Lxaero/map/graphics/MapRenderHelper;restoreDefaultShaderBlendState()V",
        shift = At.Shift.BEFORE
    ), remap = true)
    public void renderCoordinatesGotoTextEntryFields(final DrawContext guiGraphics, final int scaledMouseX, final int scaledMouseY, final float partialTicks, final CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.currentScreen != null && mc.currentScreen.getClass().equals(GuiMap.class) && xTextEntryField.isVisible() && zTextEntryField.isVisible()) {
            xTextEntryField.render(guiGraphics, scaledMouseX, scaledMouseY, partialTicks);
            zTextEntryField.render(guiGraphics, scaledMouseX, scaledMouseY, partialTicks);
        }
    }

    @WrapWithCondition(method = "render", at = @At(
        value = "INVOKE",
        target = "Lxaero/map/graphics/MapRenderHelper;drawCenteredStringWithBackground(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;IIIFFFFLnet/minecraft/client/render/VertexConsumer;)V"
    ), remap = true)
    public boolean hideMoreRenderedStringsOnF1(final DrawContext guiGraphics, final TextRenderer font, final String text, final int x, final int y, final int color, final float bgRed, final float bgGreen, final float bgBlue, final float bgAlpha, final VertexConsumer backgroundVertexBuffer) {
        return !MinecraftClient.getInstance().options.hudHidden;
    }

    @Inject(method = "onDimensionToggleButton", at = @At(value = "RETURN"))
    public void onDimensionToggleAfter(final ButtonWidget b, final CallbackInfo ci) {
        if (!XaeroPlusSettingRegistry.radarWhileDimensionSwitchedSetting.getValue()) {
            if (mapProcessor.getMapWorld().getFutureDimensionId() != ChunkUtils.getActualDimension())
                WorldMap.settings.minimapRadar = false;
            else WorldMap.settings.minimapRadar = true;
        }
    }

    @Inject(method = "tick", at = @At("RETURN"), remap = true)
    public void onTick(final CallbackInfo ci) {
        xTextEntryField.tick();
        zTextEntryField.tick();
    }

    // todo: mixin on mouseClicked to close coord entry fields when clicking on something else

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true, remap = true)
    public void onInputPress(final int code, final int scanCode, final int modifiers, final CallbackInfoReturnable<Boolean> cir) {
        if (code == 290) {
            MinecraftClient.getInstance().options.hudHidden = !MinecraftClient.getInstance().options.hudHidden;
            cir.setReturnValue(true);
            return;
        }
        if ((xTextEntryField.isVisible() && zTextEntryField.isVisible()) && (xTextEntryField.isFocused() || zTextEntryField.isFocused())) {
            if (code == 257) {
                onGotoCoordinatesButton(null);
                cir.setReturnValue(true);
            }
        }
    }

    @Inject(method = "getRightClickOptions", at = @At(value = "RETURN"), remap = false)
    public void getRightClickOptionsInject(final CallbackInfoReturnable<ArrayList<RightClickOption>> cir) {
        if (BaritoneHelper.isBaritonePresent()) {
            RegistryKey<World> customDim = Globals.getCurrentDimensionId();
            RegistryKey<World> actualDim = ChunkUtils.getActualDimension();
            double customDimDiv = 1.0;
            if (customDim != actualDim) {
                if (customDim == NETHER && actualDim == OVERWORLD) {
                    customDimDiv = 8;
                } else if (customDim == OVERWORLD && actualDim == NETHER) {
                    customDimDiv = 0.125;
                }
            }
            int goalX = (int) (rightClickX * customDimDiv);
            int goalZ = (int) (rightClickZ * customDimDiv);
            final ArrayList<RightClickOption> options = cir.getReturnValue();
            options.addAll(3, asList(
                    new RightClickOption(I18n.translate("gui.world_map.baritone_goal_here"), options.size(), this) {
                        @Override
                        public void onAction(Screen screen) {
                            BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoal(new GoalXZ(goalX, goalZ));
                        }
                    },
                    new RightClickOption(I18n.translate("gui.world_map.baritone_path_here"), options.size(), this) {
                        @Override
                        public void onAction(Screen screen) {
                            BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalXZ(goalX, goalZ));
                        }
                    }
            ));
            if (BaritoneHelper.isBaritoneElytraPresent()) {
                options.addAll(5, asList(
                    new RightClickOption(I18n.translate("gui.world_map.baritone_elytra_here"), options.size(), this) {
                        @Override
                        public void onAction(Screen screen) {
                            BaritoneAPI.getProvider().getPrimaryBaritone().getElytraProcess().pathTo(new GoalXZ(goalX, goalZ));
                        }
                    }
                ));
            }
        }

        if (XaeroPlusSettingRegistry.disableWaypointSharing.getValue()) {
            cir.getReturnValue().removeIf(option -> ((AccessorRightClickOption) option).getName().equals("gui.xaero_right_click_map_share_location"));
        }
    }

    @Unique
    public void addColoredLineToExistingBuffer(
            MatrixStack.Entry matrices, VertexConsumer vertexBuffer, float x1, float y1, float x2, float y2, float r, float g, float b, float a
    ) {
        vertexBuffer.vertex(matrices.getPositionMatrix(), x1, y1, 0.0F).color(r, g, b, a).normal(matrices.getNormalMatrix(), x2 - x1, y2 - y1, 0.0F).next();
        vertexBuffer.vertex(matrices.getPositionMatrix(), x2, y2, 0.0F).color(r, g, b, a).normal(matrices.getNormalMatrix(), x2 - x1, y2 - y1, 0.0F).next();
    }

    public void onFollowButton(final ButtonWidget b) {
        FOLLOW = !FOLLOW;
        this.init(MinecraftClient.getInstance(), width, height);
    }

    public void onGotoCoordinatesButton(final ButtonWidget b) {
        if (xTextEntryField.isVisible() && zTextEntryField.isVisible()) {
            try {
                int x = Integer.parseInt(xTextEntryField.getText());
                int z = Integer.parseInt(zTextEntryField.getText());
                cameraX = x;
                cameraZ = z;
                FOLLOW = false;
                this.init(MinecraftClient.getInstance(), width, height);
            } catch (final NumberFormatException e) {
                xTextEntryField.setText("");
                zTextEntryField.setText("");
                WorldMap.LOGGER.warn("Go to coordinates failed" , e);
            }
        } else {
            xTextEntryField.setVisible(true);
            zTextEntryField.setVisible(true);
            // todo: this isn't setting the entry field active and available to type in for some reason
            this.setFocused(xTextEntryField);
            xTextEntryField.setEditable(true);
            xTextEntryField.setFocused(true);
        }
    }

    private void onSwitchDimensionButton(final RegistryKey<World> newDimId) {
        Globals.switchToDimension(newDimId);
    }

    @Unique
    public List<ButtonWidget> getButtonList() {
        return children().stream()
                .filter(child -> child instanceof ButtonWidget)
                .map(child -> (ButtonWidget) child)
                .collect(Collectors.toList());
    }

    @Unique
    public void clearButtons() {
        getButtonList().forEach(this::remove);
    }
}
