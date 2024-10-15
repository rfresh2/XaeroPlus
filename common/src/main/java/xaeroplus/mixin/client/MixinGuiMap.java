package xaeroplus.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.common.graphics.shader.MinimapShaders;
import xaero.hud.HudSession;
import xaero.map.MapProcessor;
import xaero.map.WorldMap;
import xaero.map.animation.SlowingAnimation;
import xaero.map.element.HoveredMapElementHolder;
import xaero.map.element.MapElementRenderHandler;
import xaero.map.graphics.MapRenderHelper;
import xaero.map.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider;
import xaero.map.gui.*;
import xaero.map.gui.dropdown.rightclick.RightClickOption;
import xaero.map.misc.Misc;
import xaero.map.misc.OptimizedMath;
import xaero.map.mods.SupportMods;
import xaero.map.world.MapDimension;
import xaeroplus.Globals;
import xaeroplus.XaeroPlus;
import xaeroplus.settings.Settings;
import xaeroplus.util.BaritoneExecutor;
import xaeroplus.util.BaritoneHelper;
import xaeroplus.util.ChunkUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static net.minecraft.world.level.Level.*;
import static org.lwjgl.glfw.GLFW.*;
import static xaeroplus.Globals.getCurrentDimensionId;
import static xaeroplus.util.ChunkUtils.getPlayerX;
import static xaeroplus.util.ChunkUtils.getPlayerZ;

@Mixin(value = GuiMap.class, remap = false)
public abstract class MixinGuiMap extends ScreenBase implements IRightClickableElement {
    @Unique private static boolean follow = false;
    @Unique boolean pan;
    @Unique double panMouseStartX;
    @Unique double panMouseStartY;
    @Unique Button coordinateGotoButton;
    @Unique EditBox xTextEntryField;
    @Unique EditBox zTextEntryField;
    @Unique Button followButton;
    @Unique Button switchToNetherButton;
    @Unique Button switchToOverworldButton;
    @Unique Button switchToEndButton;
    @Unique List<Button> guiMapButtonTempList = new ArrayList<>();
    @Unique ResourceLocation xpGuiTextures = ResourceLocation.fromNamespaceAndPath("xaeroplus", "gui/xpgui.png");
    @Shadow private double cameraX = 0.0;
    @Shadow private double cameraZ = 0.0;
    @Shadow private int[] cameraDestination = null;
    @Shadow private SlowingAnimation cameraDestinationAnimX = null;
    @Shadow private SlowingAnimation cameraDestinationAnimZ = null;
    @Shadow private double scale;
    @Shadow private double prevPlayerDimDiv;
    @Shadow private MapProcessor mapProcessor;
    @Shadow private Button exportButton;
    @Shadow private Button claimsButton;
    @Shadow private Button zoomInButton;
    @Shadow private Button zoomOutButton;
    @Shadow private Button keybindingsButton;
    @Shadow private Button dimensionToggleButton;
    @Shadow private int rightClickX;
    @Shadow private int rightClickY;
    @Shadow private int rightClickZ;
    @Shadow private int mouseBlockPosX;
    @Shadow private int mouseBlockPosZ;
    @Shadow private static double destScale;
    @Shadow private MapTileSelection mapTileSelection;

    protected MixinGuiMap(final Screen parent, final Screen escape, final Component titleIn) {
        super(parent, escape, titleIn);
    }

    @Shadow public abstract <T extends GuiEventListener & Renderable & NarratableEntry> T addButton(final T guiEventListener);
    @Shadow public abstract <T extends GuiEventListener & NarratableEntry> T addWidget(final T guiEventListener);

    @ModifyExpressionValue(method = "changeZoom",
        at = @At(
            value = "CONSTANT",
            args = "doubleValue=0.0625"))
    public double customMinZoom(final double original) {
        return Settings.REGISTRY.worldMapMinZoomSetting.get() / 10.0f;
    }

    @Inject(method = "init", at = @At(value = "RETURN"), remap = true)
    public void customInitGui(CallbackInfo ci) {
        // left side
        followButton = new GuiTexturedButton(0, this.dimensionToggleButton.getY() - 20, 20, 20, this.follow ? 133 : 149, 16, 16, 16,
                                             WorldMap.guiTextures,
                                             this::onFollowButton,
                                             () -> new CursorBox(Component.translatable("xaeroplus.gui.world_map.toggle_follow_mode")
                                                                         .append(" " + I18n.get(this.follow ? "xaeroplus.gui.off" : "xaeroplus.gui.on"))));
        addButton(followButton);
        coordinateGotoButton = new GuiTexturedButton(0, followButton.getY() - 20 , 20, 20, 229, 16, 16, 16,
                                                     WorldMap.guiTextures,
                                                     this::onGotoCoordinatesButton,
                                                     () -> new CursorBox(Component.translatable("xaeroplus.gui.world_map.go_to_coordinates")));
        addButton(coordinateGotoButton);
        xTextEntryField = new EditBox(Minecraft.getInstance().font, 20, coordinateGotoButton.getY() - 10, 50, 20, Component.nullToEmpty("X:"));
        xTextEntryField.setVisible(false);
        xTextEntryField.setCursorPosition(0);
        xTextEntryField.setHint(Component.literal("X:").withStyle(ChatFormatting.DARK_GRAY));
        zTextEntryField = new EditBox(Minecraft.getInstance().font, 20, xTextEntryField.getY() + 20, 50, 20, Component.nullToEmpty("Z:"));
        zTextEntryField.setVisible(false);
        zTextEntryField.setCursorPosition(0);
        zTextEntryField.setHint(Component.literal("Z:").withStyle(ChatFormatting.DARK_GRAY));
        this.addWidget(xTextEntryField);
        this.addWidget(zTextEntryField);
        // right side
        if (!SupportMods.pac()) {  // remove useless button when pac is not installed
            this.removeWidget(this.claimsButton);
            this.exportButton.setY(this.claimsButton.getY());
            this.keybindingsButton.setY(this.claimsButton.getY() - 20);
            this.zoomOutButton.setY(this.keybindingsButton.getY() - 20);
            this.zoomInButton.setY(this.zoomOutButton.getY() - 20);
        }
        switchToEndButton = new GuiTexturedButton(this.width - 20, zoomInButton.getY() - 20, 20, 20, 31, 0, 16, 16,
                                                  this.xpGuiTextures,
                                                  (button -> onSwitchDimensionButton(END)),
                                                  () -> new CursorBox(Component.translatable("xaeroplus.keybind.switch_to_end")));
        switchToOverworldButton = new GuiTexturedButton(this.width - 20, this.switchToEndButton.getY() - 20, 20, 20, 16, 0, 16, 16,
                                                        this.xpGuiTextures,
                                                        (button -> onSwitchDimensionButton(OVERWORLD)),
                                                        () -> new CursorBox(Component.translatable("xaeroplus.keybind.switch_to_overworld")));
        switchToNetherButton = new GuiTexturedButton(this.width - 20, this.switchToOverworldButton.getY() - 20, 20, 20, 0, 0, 16, 16,
                                                     this.xpGuiTextures,
                                                     (button -> onSwitchDimensionButton(NETHER)),
                                                     () -> new CursorBox(Component.translatable("xaeroplus.keybind.switch_to_nether")));
        addButton(switchToEndButton);
        addButton(switchToOverworldButton);
        addButton(switchToNetherButton);
        pan = false;
    }

    @Override
    protected void onExit(Screen screen) {
        if (!Settings.REGISTRY.persistMapDimensionSwitchSetting.get()) {
            try {
                var actualDimension = ChunkUtils.getActualDimension();
                if (Globals.getCurrentDimensionId() != actualDimension) {
                    Globals.switchToDimension(actualDimension);
                    if (!Settings.REGISTRY.radarWhileDimensionSwitchedSetting.get()) {
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
        target = "Lxaero/map/gui/GuiMap;init(Lnet/minecraft/client/Minecraft;II)V",
        ordinal = 0,
        shift = At.Shift.AFTER
    ), remap = true)
    public void toggleRadarWhileDimensionSwitched(final GuiGraphics guiGraphics, final int scaledMouseX, final int scaledMouseY, final float partialTicks, final CallbackInfo ci, @Local(name = "currentFutureDim") MapDimension currentFutureDim) {
        if (!Settings.REGISTRY.radarWhileDimensionSwitchedSetting.get()) {
            WorldMap.settings.minimapRadar = currentFutureDim.getDimId() == ChunkUtils.getActualDimension();
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
    public void injectFollowMode(final GuiGraphics guiGraphics, final int scaledMouseX, final int scaledMouseY, final float partialTicks, final CallbackInfo ci) {
        if (follow && isNull(this.cameraDestination) && isNull(this.cameraDestinationAnimX) && isNull(this.cameraDestinationAnimZ)) {
            this.cameraDestination = new int[]{(int) getPlayerX(), (int) getPlayerZ()};
        }
    }

    @ModifyExpressionValue(method = "render", at = @At(
        value = "FIELD",
        target = "Lxaero/map/settings/ModSettings;debug:Z",
        opcode = Opcodes.GETFIELD
    ), remap = true) // multiple field accesses
    public boolean hideDebugRenderingOnF1(boolean original) {
        return original && !Minecraft.getInstance().options.hideGui;
    }

    @Inject(method = "render",
        slice = @Slice(
            from = @At(
                value = "FIELD",
                target = "Lxaero/map/gui/GuiMap;prevLoadingLeaves:Z",
                opcode = Opcodes.PUTFIELD
            )
        ),
        at = @At(
            value = "INVOKE",
            target = "Lxaero/map/graphics/MapRenderHelper;renderDynamicHighlight(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIIIIIFFFFFFFF)V",
            ordinal = 0
        ),
        remap = true)
    public void drawWorldMapFeatures(final GuiGraphics guiGraphics, final int scaledMouseX, final int scaledMouseY, final float partialTicks, final CallbackInfo ci,
                                     @Local(name = "leftBorder") double leftBorder,
                                     @Local(name = "rightBorder") double rightBorder,
                                     @Local(name = "topBorder") double topBorder,
                                     @Local(name = "bottomBorder") double bottomBorder,
                                     @Local(name = "flooredCameraX") int flooredCameraX,
                                     @Local(name = "flooredCameraZ") int flooredCameraZ,
                                     @Local(name = "matrixStack") PoseStack matrixStack,
                                     @Local(name = "overlayBuffer") VertexConsumer overlayBuffer) {
        if (Minecraft.getInstance().options.hideGui) return;
        Globals.drawManager.drawWorldMapFeatures(
            leftBorder,
            rightBorder,
            topBorder,
            bottomBorder,
            flooredCameraX,
            flooredCameraZ,
            matrixStack,
            overlayBuffer);
    }

    @WrapWithCondition(method = "render", at = @At(
        value = "INVOKE",
        target = "Lxaero/map/graphics/MapRenderHelper;renderDynamicHighlight(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIIIIIFFFFFFFF)V"
    ), remap = true)
    public boolean hideHighlightsOnF1(final PoseStack matrixStack, final VertexConsumer overlayBuffer, final int flooredCameraX, final int flooredCameraZ, final int leftX, final int rightX, final int topZ, final int bottomZ, final float sideR, final float sideG, final float sideB, final float sideA, final float centerR, final float centerG, final float centerB, final float centerA) {
        return !Minecraft.getInstance().options.hideGui;
    }

    @WrapOperation(method = "render", at = @At(
        value = "INVOKE",
        target = "Lxaero/map/element/MapElementRenderHandler;render(Lxaero/map/gui/GuiMap;Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lxaero/map/graphics/renderer/multitexture/MultiTextureRenderTypeRendererProvider;DDIIDDDDDFZLxaero/map/element/HoveredMapElementHolder;Lnet/minecraft/client/Minecraft;F)Lxaero/map/element/HoveredMapElementHolder;"
    ), remap = true)
    public HoveredMapElementHolder<?, ?> hideMapElementsOnF1(MapElementRenderHandler handler, GuiMap mapScreen, GuiGraphics guiGraphics, MultiBufferSource.BufferSource renderTypeBuffers, MultiTextureRenderTypeRendererProvider rendererProvider, double cameraX, double cameraZ, int width, int height, double screenSizeBasedScale, double scale, double playerDimDiv, double mouseX, double mouseZ, float brightness, boolean cave, HoveredMapElementHolder<?, ?> oldHovered, Minecraft mc, float partialTicks, Operation<HoveredMapElementHolder<?, ?>> original) {
        if (!Minecraft.getInstance().options.hideGui) {
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
        return original && !Minecraft.getInstance().options.hideGui;
    }

    @ModifyExpressionValue(method = "render", at = @At(
        value = "FIELD",
        target = "Lxaero/map/settings/ModSettings;renderArrow:Z",
        opcode = Opcodes.GETFIELD
    ), remap = true)
    public boolean hideArrowOnF1(boolean original) {
        return original && !Minecraft.getInstance().options.hideGui;
    }

    @ModifyArg(method = "render",
        slice = @Slice(
            from = @At(
                value = "FIELD",
                opcode = Opcodes.GETFIELD,
                target = "Lxaero/map/settings/ModSettings;coordinates:Z"
            )
        ),
        at = @At(
            value = "INVOKE",
            target = "Lxaero/map/graphics/MapRenderHelper;drawCenteredStringWithBackground(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIFFFFLcom/mojang/blaze3d/vertex/VertexConsumer;)V",
            ordinal = 0
    ), index = 2)
    public String renderCrossDimensionCursorCoordinates(final String original) {
        if (!Settings.REGISTRY.crossDimensionCursorCoordinates.get()) return original;
        ResourceKey<Level> dim = getCurrentDimensionId();
        if (!(dim == OVERWORLD || dim == NETHER)) return original;
        double dimDiv = dim == NETHER
            ? 0.125 // nether -> overworld
            : 8; // overworld -> nether
        int x = (int) (mouseBlockPosX / dimDiv);
        int z = (int) (mouseBlockPosZ / dimDiv);
        return original + " [" + x + ", " + z + "]";
    }

    @Inject(method = "render", at = @At(
        value = "FIELD",
        target = "Lxaero/map/settings/ModSettings;renderArrow:Z",
        opcode = Opcodes.GETFIELD,
        ordinal = 0
    ), remap = true)
    public void showRenderDistanceWorldMap(final GuiGraphics guiGraphics, final int scaledMouseX, final int scaledMouseY, final float partialTicks, final CallbackInfo ci,
                                           @Local(name = "renderTypeBuffers") MultiBufferSource.BufferSource renderTypeBuffers,
                                           @Local(name = "matrixStack") PoseStack matrixStack) {
        Minecraft mc = Minecraft.getInstance();
        if (!Settings.REGISTRY.showRenderDistanceWorldMapSetting.get()
            || mc.options.hideGui
            || mc.level.dimension() != Globals.getCurrentDimensionId()) return;
        final int viewDistance = mc.options.serverRenderDistance;
        int width = viewDistance * 2 + 1;
        double playerX = getPlayerX();
        double playerZ = getPlayerZ();
        int xFloored = OptimizedMath.myFloor(playerX);
        int zFloored = OptimizedMath.myFloor(playerZ);
        int chunkLeftX = (xFloored >> 4) - (width / 2) << 4;
        int chunkRightX = (xFloored >> 4) + 1 + (width / 2) << 4;
        int chunkTopZ = (zFloored >> 4) - (width / 2) << 4;
        int chunkBottomZ = (zFloored >> 4) + 1 + (width / 2) << 4;
        float camX = (float) cameraX;
        float camZ = (float) cameraZ;
        final float x0 = chunkLeftX - camX;
        final float x1 = chunkRightX - camX;
        final float z0 = chunkTopZ - camZ;
        final float z1 = chunkBottomZ - camZ;
        VertexConsumer lineBufferBuilder = renderTypeBuffers.getBuffer(xaero.common.graphics.CustomRenderTypes.MAP_LINES);
        PoseStack.Pose matrices = matrixStack.last();
        MinimapShaders.FRAMEBUFFER_LINES.setFrameSize(mc.getWindow().getWidth(), mc.getWindow().getHeight());
        float settingWidth = (float) HudSession.getCurrentSession().getHudMod().getSettings().chunkGridLineWidth;
        float lineScale = (float) Math.max(1.0, Math.min(settingWidth * scale, settingWidth));
        RenderSystem.lineWidth(lineScale);
        float r = 1.0f;
        float g = 1.0f;
        float b = 0.0f;
        float a = 0.8f;
        addColoredLineToExistingBuffer(
            matrices, lineBufferBuilder,
            x0, z0, x1, z0,
            r, g, b, a
        );
        addColoredLineToExistingBuffer(
            matrices, lineBufferBuilder,
            x1, z0, x1, z1,
            r, g, b, a
        );
        addColoredLineToExistingBuffer(
            matrices, lineBufferBuilder,
            x1, z1, x0, z1,
            r, g, b, a
        );
        addColoredLineToExistingBuffer(
            matrices, lineBufferBuilder,
            x0, z0, x0, z1,
            r, g, b, a
        );
    }

    @Inject(method = "render", at = @At(
        value = "FIELD",
        target = "Lxaero/map/settings/ModSettings;renderArrow:Z",
        opcode = Opcodes.GETFIELD,
        ordinal = 0
    ), remap = true)
    public void showWorldBorderWorldMap(final GuiGraphics guiGraphics, final int scaledMouseX, final int scaledMouseY, final float partialTicks, final CallbackInfo ci,
                                           @Local(name = "renderTypeBuffers") MultiBufferSource.BufferSource renderTypeBuffers,
                                           @Local(name = "matrixStack") PoseStack matrixStack) {
        Minecraft mc = Minecraft.getInstance();
        if (!Settings.REGISTRY.showWorldBorderWorldMapSetting.get()
            || mc.options.hideGui
            || mc.level.dimension() != Globals.getCurrentDimensionId()) return;
        var worldBorder = mc.level.getWorldBorder();
        float wbMinX = (float) worldBorder.getMinX();
        float wbMinZ = (float) worldBorder.getMinZ();
        float wbMaxX = (float) worldBorder.getMaxX();
        float wbMaxZ = (float) worldBorder.getMaxZ();
        float camX = (float) cameraX;
        float camZ = (float) cameraZ;
        float x0 = wbMinX - camX;
        float z0 = wbMinZ - camZ;
        float x1 = wbMaxX - camX;
        float z1 = wbMaxZ - camZ;
        VertexConsumer lineBufferBuilder = renderTypeBuffers.getBuffer(xaero.common.graphics.CustomRenderTypes.MAP_LINES);
        PoseStack.Pose matrices = matrixStack.last();
        MinimapShaders.FRAMEBUFFER_LINES.setFrameSize(mc.getWindow().getWidth(), mc.getWindow().getHeight());
        float settingWidth = (float) HudSession.getCurrentSession().getHudMod().getSettings().chunkGridLineWidth;
        float lineScale = (float) Math.max(1.0, Math.min(settingWidth * scale, settingWidth));
        RenderSystem.lineWidth(lineScale);
        float r = 0.0f;
        float g = 1.0f;
        float b = 1.0f;
        float a = 0.8f;
        addColoredLineToExistingBuffer(
            matrices, lineBufferBuilder,
            x0, z0, x1, z0,
            r, g, b, a
        );
        addColoredLineToExistingBuffer(
            matrices, lineBufferBuilder,
            x1, z0, x1, z1,
            r, g, b, a
        );
        addColoredLineToExistingBuffer(
            matrices, lineBufferBuilder,
            x1, z1, x0, z1,
            r, g, b, a
        );
        addColoredLineToExistingBuffer(
            matrices, lineBufferBuilder,
            x0, z0, x0, z1,
            r, g, b, a
        );
    }

    @WrapWithCondition(method = "render", at = @At(
        value = "INVOKE",
        target = "Lxaero/map/graphics/MapRenderHelper;drawCenteredStringWithBackground(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIFFFFLcom/mojang/blaze3d/vertex/VertexConsumer;)V"
    ), remap = true)
    public boolean hideRenderedStringsOnF1(final GuiGraphics guiGraphics, final Font font, final String string, final int x, final int y, final int color, final float bgRed, final float bgGreen, final float bgBlue, final float bgAlpha, final VertexConsumer backgroundVertexBuffer) {
        return !Minecraft.getInstance().options.hideGui;
    }

    @WrapWithCondition(method = "render", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIIII)V"
    ), remap = true)
    public boolean hideCompassOnF1(final GuiGraphics instance, final ResourceLocation texture, final int x, final int y, final int u, final int v, final int width, final int height) {
        return !Minecraft.getInstance().options.hideGui;
    }

    @Inject(method = "render", at = @At(
        value = "INVOKE",
        target = "Lxaero/map/gui/ScreenBase;render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"
    ), remap = true)
    public void hideButtonsOnF1(final GuiGraphics guiGraphics, final int scaledMouseX, final int scaledMouseY, final float partialTicks, final CallbackInfo ci) {
        if (Minecraft.getInstance().options.hideGui) {
            List<Button> buttonList = getButtonList();
            if (!buttonList.isEmpty()) {
                this.guiMapButtonTempList.clear();
                this.guiMapButtonTempList.addAll(buttonList);
                xTextEntryField.setVisible(false);
                zTextEntryField.setVisible(false);
                clearButtons();
            }
        } else {
            if (!this.guiMapButtonTempList.isEmpty()) {
                clearButtons();
                this.guiMapButtonTempList.forEach(this::addButton);
                this.guiMapButtonTempList.clear();
            }
        }
    }

    @Inject(method = "render", at = @At(
        value = "INVOKE",
        target = "Lxaero/map/graphics/MapRenderHelper;restoreDefaultShaderBlendState()V"
    ), remap = true)
    public void renderCoordinatesGotoTextEntryFields(final GuiGraphics guiGraphics, final int scaledMouseX, final int scaledMouseY, final float partialTicks, final CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null && mc.screen.getClass().equals(GuiMap.class) && xTextEntryField.isVisible() && zTextEntryField.isVisible()) {
            xTextEntryField.render(guiGraphics, scaledMouseX, scaledMouseY, partialTicks);
            zTextEntryField.render(guiGraphics, scaledMouseX, scaledMouseY, partialTicks);
        }
    }

    @WrapWithCondition(method = "render", at = @At(
        value = "INVOKE",
        target = "Lxaero/map/graphics/MapRenderHelper;drawCenteredStringWithBackground(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIFFFFLcom/mojang/blaze3d/vertex/VertexConsumer;)V"
    ), remap = true)
    public boolean hideMoreRenderedStringsOnF1(final GuiGraphics guiGraphics, final Font font, final Component text, final int x, final int y, final int color, final float bgRed, final float bgGreen, final float bgBlue, final float bgAlpha, final VertexConsumer backgroundVertexBuffer) {
        return !Minecraft.getInstance().options.hideGui;
    }

    @Inject(method = "onDimensionToggleButton", at = @At(value = "RETURN"))
    public void onDimensionToggleAfter(final Button b, final CallbackInfo ci) {
        if (!Settings.REGISTRY.radarWhileDimensionSwitchedSetting.get()) {
            WorldMap.settings.minimapRadar = mapProcessor.getMapWorld().getFutureDimensionId() == ChunkUtils.getActualDimension();
        }
    }

    // todo: mixin on mouseClicked to close coord entry fields when clicking on something else

    @Inject(method = "onInputPress", at = @At("HEAD"))
    public void panMouseButtonClick(final InputConstants.Type type, final int code, final CallbackInfoReturnable<Boolean> cir) {
        if (type != InputConstants.Type.MOUSE) return;
        if (code != GLFW_MOUSE_BUTTON_MIDDLE) return;
        pan = true;
        var mc = Minecraft.getInstance();
        panMouseStartX = Misc.getMouseX(mc, true);
        panMouseStartY = Misc.getMouseY(mc, true);
    }

    @Inject(method = "onInputRelease", at = @At("HEAD"))
    public void panMouseButtonRelease(final InputConstants.Type type, final int code, final CallbackInfoReturnable<Boolean> cir) {
        if (type != InputConstants.Type.MOUSE) return;
        if (code != GLFW_MOUSE_BUTTON_MIDDLE) return;
        pan = false;
    }

    @Inject(method = "render", at = @At("HEAD"))
    public void panMapOnRender(final GuiGraphics guiGraphics, final int scaledMouseX, final int scaledMouseY, final float partialTicks, final CallbackInfo ci) {
        if (!pan) return;
        Minecraft mc = Minecraft.getInstance();
        double mouseX = Misc.getMouseX(mc, true);
        double mouseY = Misc.getMouseY(mc, true);
        double mouseDeltaX = mouseX - panMouseStartX;
        double mouseDeltaY = mouseY - panMouseStartY;
        double panDeltaX = (partialTicks * mouseDeltaX) / destScale;
        double panDeltaZ = (partialTicks * mouseDeltaY) / destScale;
        cameraX += panDeltaX;
        cameraZ += panDeltaZ;
    }

    @Inject(method = "render", at = @At(
        value = "FIELD",
        target = "Lxaero/map/settings/ModSettings;coordinates:Z",
        opcode = Opcodes.GETFIELD,
        ordinal = 0
    ), remap = true)
    public void renderTileSelectionSize(
        final GuiGraphics guiGraphics,
        final int scaledMouseX,
        final int scaledMouseY,
        final float partialTicks,
        final CallbackInfo ci,
        @Local(name = "backgroundVertexBuffer") VertexConsumer backgroundVertexBuffer
    ) {
        MapTileSelection selection = this.mapTileSelection;
        if (selection == null) return;
        var sideLen = Math.abs(selection.getRight() - selection.getLeft())+1;
        var heightLen = Math.abs(selection.getBottom() - selection.getTop())+1;
        if (sideLen <= 1 && heightLen <= 1) return;
        // todo: it'd be better if we could render this directly on the highlight
        //  but we need a function for map -> screen coordinates translation
        MapRenderHelper.drawCenteredStringWithBackground(guiGraphics, font, sideLen + " x " + heightLen, scaledMouseX, scaledMouseY - font.lineHeight, -1, 0.0f, 0.0f, 0.0f, 0.4f, backgroundVertexBuffer);
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true, remap = true)
    public void onInputPress(final int code, final int scanCode, final int modifiers, final CallbackInfoReturnable<Boolean> cir) {
        if (code == GLFW_KEY_F1) {
            Minecraft.getInstance().options.hideGui = !Minecraft.getInstance().options.hideGui;
            cir.setReturnValue(true);
            return;
        }
        if ((xTextEntryField.isVisible() && zTextEntryField.isVisible()) && (xTextEntryField.isFocused() || zTextEntryField.isFocused())) {
            if (code == GLFW_KEY_ENTER) {
                onGotoCoordinatesButton(null);
                cir.setReturnValue(true);
                return;
            }
        }
        if (BaritoneHelper.isBaritonePresent()) {
            if (Settings.REGISTRY.worldMapBaritoneGoalHereKeybindSetting.getKeyBinding().matches(code, scanCode)) {
                BaritoneExecutor.goal(mouseBlockPosX, mouseBlockPosZ);
                cir.setReturnValue(true);
            } else if (Settings.REGISTRY.worldMapBaritonePathHereKeybindSetting.getKeyBinding().matches(code, scanCode)) {
                BaritoneExecutor.path(mouseBlockPosX, mouseBlockPosZ);
                cir.setReturnValue(true);
            } else if (BaritoneHelper.isBaritoneElytraPresent() && Settings.REGISTRY.worldMapBaritoneElytraHereKeybindSetting.getKeyBinding().matches(code, scanCode)) {
                BaritoneExecutor.elytra(mouseBlockPosX, mouseBlockPosZ);
                cir.setReturnValue(true);
            }
        }
    }

    @Inject(method = "getRightClickOptions", at = @At(value = "RETURN"), remap = false)
    public void getRightClickOptionsInject(final CallbackInfoReturnable<ArrayList<RightClickOption>> cir) {
        final ArrayList<RightClickOption> options = cir.getReturnValue();
        int index = 3;
        options.add(index++, new RightClickOption("xaeroplus.gui.world_map.copy_coordinates", options.size(), this) {
            @Override
            public void onAction(final Screen screen) {
                Minecraft.getInstance().keyboardHandler.setClipboard(rightClickX + " " + rightClickY + " " + rightClickZ);
            }
        });
        if (BaritoneHelper.isBaritonePresent()) {
            int goalX = rightClickX;
            int goalZ = rightClickZ;
            options.add(index++, new RightClickOption("xaeroplus.gui.world_map.baritone_goal_here", options.size(), this) {
                        @Override
                        public void onAction(Screen screen) {
                            BaritoneExecutor.goal(goalX, goalZ);
                        }
                    }.setNameFormatArgs(Misc.getKeyName(Settings.REGISTRY.worldMapBaritoneGoalHereKeybindSetting.getKeyBinding())));
            options.add(index++, new RightClickOption("xaeroplus.gui.world_map.baritone_path_here", options.size(), this) {
                        @Override
                        public void onAction(Screen screen) {
                            BaritoneExecutor.path(goalX, goalZ);
                        }
                    }.setNameFormatArgs(Misc.getKeyName(Settings.REGISTRY.worldMapBaritonePathHereKeybindSetting.getKeyBinding())));
            if (BaritoneHelper.isBaritoneElytraPresent()) {
                options.add(index++, new RightClickOption("xaeroplus.gui.world_map.baritone_elytra_here", options.size(), this) {
                        @Override
                        public void onAction(Screen screen) {
                            BaritoneExecutor.elytra(goalX, goalZ);
                        }
                    }.setNameFormatArgs(Misc.getKeyName(Settings.REGISTRY.worldMapBaritoneElytraHereKeybindSetting.getKeyBinding())));
            }
        }

        if (Settings.REGISTRY.disableWaypointSharing.get()) {
            options.removeIf(option -> ((AccessorRightClickOption) option).getName().equals("gui.xaero_right_click_map_share_location"));
        }
    }

    @Unique
    public void addColoredLineToExistingBuffer(
            PoseStack.Pose matrices, VertexConsumer vertexBuffer, float x1, float y1, float x2, float y2, float r, float g, float b, float a
    ) {
        vertexBuffer.addVertex(matrices.pose(), x1, y1, 0.0F).setColor(r, g, b, a).setNormal(matrices, x2 - x1, y2 - y1, 0.0F);
        vertexBuffer.addVertex(matrices.pose(), x2, y2, 0.0F).setColor(r, g, b, a).setNormal(matrices, x2 - x1, y2 - y1, 0.0F);
    }

    @Unique
    public void onFollowButton(final Button b) {
        follow = !follow;
        this.init(Minecraft.getInstance(), width, height);
    }

    public void onGotoCoordinatesButton(final Button b) {
        if (xTextEntryField.isVisible() && zTextEntryField.isVisible()) {
            try {
                int x = Integer.parseInt(xTextEntryField.getValue());
                int z = Integer.parseInt(zTextEntryField.getValue());
                cameraX = x;
                cameraZ = z;
                follow = false;
                this.init(Minecraft.getInstance(), width, height);
            } catch (final NumberFormatException e) {
                xTextEntryField.setValue("");
                zTextEntryField.setValue("");
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

    @Unique
    private void onSwitchDimensionButton(final ResourceKey<Level> newDimId) {
        Globals.switchToDimension(newDimId);
    }

    @Unique
    public List<Button> getButtonList() {
        return children().stream()
                .filter(child -> child instanceof Button)
                .map(child -> (Button) child)
                .collect(Collectors.toList());
    }

    @Unique
    public void clearButtons() {
        getButtonList().forEach(this::removeWidget);
    }
}
