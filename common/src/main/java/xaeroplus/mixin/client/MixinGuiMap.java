package xaeroplus.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
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
import xaero.map.misc.Misc;
import xaero.map.misc.OptimizedMath;
import xaero.map.mods.SupportMods;
import xaero.map.world.MapDimension;
import xaeroplus.Globals;
import xaeroplus.XaeroPlus;
import xaeroplus.settings.XaeroPlusSettingRegistry;
import xaeroplus.util.BaritoneExecutor;
import xaeroplus.util.BaritoneHelper;
import xaeroplus.util.ChunkUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.Objects.isNull;
import static net.minecraft.world.level.Level.*;
import static xaeroplus.Globals.getCurrentDimensionId;
import static xaeroplus.util.ChunkUtils.getPlayerX;
import static xaeroplus.util.ChunkUtils.getPlayerZ;

@Mixin(value = GuiMap.class, remap = false)
public abstract class MixinGuiMap extends ScreenBase implements IRightClickableElement {
    @Unique private static boolean follow = false;
    @Unique Button coordinateGotoButton;
    @Unique EditBox xTextEntryField;
    @Unique EditBox zTextEntryField;
    @Unique Button followButton;
    @Unique Button switchToNetherButton;
    @Unique Button switchToOverworldButton;
    @Unique Button switchToEndButton;
    @Unique List<Button> guiMapButtonTempList = new ArrayList<>();
    @Unique ResourceLocation xpGuiTextures = new ResourceLocation("xaeroplus", "gui/xpgui.png");
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
    @Shadow private int rightClickZ;

    protected MixinGuiMap(final Screen parent, final Screen escape, final Component titleIn) {
        super(parent, escape, titleIn);
    }

    @Shadow public abstract <T extends GuiEventListener & Renderable & NarratableEntry> T addButton(final T guiEventListener);

    @Shadow public abstract <T extends GuiEventListener & NarratableEntry> T addWidget(final T guiEventListener);

    @Shadow private int mouseBlockPosX;

    @Shadow private int mouseBlockPosZ;

    @ModifyExpressionValue(method = "changeZoom",
        at = @At(
            value = "CONSTANT",
            args = "doubleValue=0.0625"))
    public double customMinZoom(final double original) {
        return XaeroPlusSettingRegistry.worldMapMinZoomSetting.getValue() / 10.0f;
    }

    @Inject(method = "init", at = @At(value = "RETURN"), remap = true)
    public void customInitGui(CallbackInfo ci) {
        // left side
        followButton = new GuiTexturedButton(0, this.dimensionToggleButton.getY() - 20, 20, 20, this.follow ? 133 : 149, 16, 16, 16,
                                             WorldMap.guiTextures,
                                             this::onFollowButton,
                                             () -> new CursorBox(Component.translatable("gui.world_map.toggle_follow_mode")
                                                                         .append(" " + I18n.get(this.follow ? "gui.xaeroplus.off" : "gui.xaeroplus.on"))));
        addButton(followButton);
        coordinateGotoButton = new GuiTexturedButton(0, followButton.getY() - 20 , 20, 20, 229, 16, 16, 16,
                                                     WorldMap.guiTextures,
                                                     this::onGotoCoordinatesButton,
                                                     () -> new CursorBox(Component.translatable("gui.world_map.go_to_coordinates")));
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
                                                  () -> new CursorBox(Component.translatable("setting.keybinds.switch_to_end")));
        switchToOverworldButton = new GuiTexturedButton(this.width - 20, this.switchToEndButton.getY() - 20, 20, 20, 16, 0, 16, 16,
                                                        this.xpGuiTextures,
                                                        (button -> onSwitchDimensionButton(OVERWORLD)),
                                                        () -> new CursorBox(Component.translatable("setting.keybinds.switch_to_overworld")));
        switchToNetherButton = new GuiTexturedButton(this.width - 20, this.switchToOverworldButton.getY() - 20, 20, 20, 0, 0, 16, 16,
                                                     this.xpGuiTextures,
                                                     (button -> onSwitchDimensionButton(NETHER)),
                                                     () -> new CursorBox(Component.translatable("setting.keybinds.switch_to_nether")));
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
        target = "Lxaero/map/gui/GuiMap;init(Lnet/minecraft/client/Minecraft;II)V",
        ordinal = 0,
        shift = At.Shift.AFTER
    ), remap = true)
    public void toggleRadarWhileDimensionSwitched(final GuiGraphics guiGraphics, final int scaledMouseX, final int scaledMouseY, final float partialTicks, final CallbackInfo ci, @Local(name = "currentFutureDim") MapDimension currentFutureDim) {
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
    public void injectFollowMode(final GuiGraphics guiGraphics, final int scaledMouseX, final int scaledMouseY, final float partialTicks, final CallbackInfo ci) {
        if (this.follow && isNull(this.cameraDestination) && isNull(this.cameraDestinationAnimX) && isNull(this.cameraDestinationAnimZ)) {
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

    // todo: re-benchmark region load perf impact
//    @ModifyArg(method = "render", at = @At(
//        value = "INVOKE",
//        target = "Lxaero/map/misc/Misc;addToListOfSmallest(ILjava/util/List;Ljava/lang/Comparable;)V"),
//        index = 0, remap = true)
//    public int increaseRegionBuffer(final int i, final List list, final Comparable element) {
//        return 100;
//    }

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
            shift = At.Shift.BEFORE,
            ordinal = 0
        ),
        remap = true)
    public void drawWorldMapFeatures(final GuiGraphics guiGraphics, final int scaledMouseX, final int scaledMouseY, final float partialTicks, final CallbackInfo ci,
                                     @Local(name = "minRegX") int minRegX,
                                     @Local(name = "maxRegX") int maxRegX,
                                     @Local(name = "minRegZ") int minRegZ,
                                     @Local(name = "maxRegZ") int maxRegZ,
                                     @Local(name = "textureLevel") int textureLevel,
                                     @Local(name = "flooredCameraX") int flooredCameraX,
                                     @Local(name = "flooredCameraZ") int flooredCameraZ,
                                     @Local(name = "matrixStack") PoseStack matrixStack,
                                     @Local(name = "overlayBuffer") VertexConsumer overlayBuffer) {
        if (Minecraft.getInstance().options.hideGui) return;
        final int leveledSideInRegions = 1 << textureLevel;
        final int minX = minRegX * leveledSideInRegions;
        final int maxX = (maxRegX + 1) * leveledSideInRegions;
        final int minZ = minRegZ * leveledSideInRegions;
        final int maxZ = (maxRegZ + 1) * leveledSideInRegions;
        Globals.drawManager.drawWorldMapFeatures(
            minX - 1,
            maxX + 1,
            minZ - 1,
            maxZ + 1,
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
        if (!XaeroPlusSettingRegistry.crossDimensionCursorCoordinates.getValue()) return original;
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
        shift = At.Shift.BEFORE
    ), remap = true)
    public void showRenderDistanceWorldMap(final GuiGraphics guiGraphics, final int scaledMouseX, final int scaledMouseY, final float partialTicks, final CallbackInfo ci,
                                           @Local(name = "flooredCameraX") int flooredCameraX,
                                           @Local(name = "flooredCameraZ") int flooredCameraZ,
                                           @Local(name = "renderTypeBuffers") MultiBufferSource.BufferSource renderTypeBuffers,
                                           @Local(name = "matrixStack") PoseStack matrixStack) {
        Minecraft mc = Minecraft.getInstance();
        if (XaeroPlusSettingRegistry.showRenderDistanceWorldMapSetting.getValue() && !mc.options.hideGui) {
            if (mc.level.dimension() == Globals.getCurrentDimensionId()) {
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
                final int x0 = chunkLeftX - flooredCameraX;
                final int x1 = chunkRightX - flooredCameraX;
                final int z0 = chunkTopZ - flooredCameraZ;
                final int z1 = chunkBottomZ - flooredCameraZ;
                VertexConsumer lineBufferBuilder = renderTypeBuffers.getBuffer(xaero.common.graphics.CustomRenderTypes.MAP_LINES);
                PoseStack.Pose matrices = matrixStack.last();
                MinimapShaders.FRAMEBUFFER_LINES.setFrameSize(mc.getWindow().getWidth(),
                                                              mc.getWindow().getHeight());

                float settingWidth = (float) XaeroMinimapSession.getCurrentSession()
                    .getModMain()
                    .getSettings().chunkGridLineWidth;
                float lineScale = (float) Math.max(1.0, Math.min(settingWidth * scale, settingWidth));
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
        target = "Lxaero/map/graphics/MapRenderHelper;restoreDefaultShaderBlendState()V",
        shift = At.Shift.BEFORE
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
        if (!XaeroPlusSettingRegistry.radarWhileDimensionSwitchedSetting.getValue()) {
            if (mapProcessor.getMapWorld().getFutureDimensionId() != ChunkUtils.getActualDimension())
                WorldMap.settings.minimapRadar = false;
            else WorldMap.settings.minimapRadar = true;
        }
    }

    // todo: mixin on mouseClicked to close coord entry fields when clicking on something else

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true, remap = true)
    public void onInputPress(final int code, final int scanCode, final int modifiers, final CallbackInfoReturnable<Boolean> cir) {
        if (code == 290) {
            Minecraft.getInstance().options.hideGui = !Minecraft.getInstance().options.hideGui;
            cir.setReturnValue(true);
            return;
        }
        if ((xTextEntryField.isVisible() && zTextEntryField.isVisible()) && (xTextEntryField.isFocused() || zTextEntryField.isFocused())) {
            if (code == 257) {
                onGotoCoordinatesButton(null);
                cir.setReturnValue(true);
                return;
            }
        }
        if (BaritoneHelper.isBaritonePresent()) {
            if (XaeroPlusSettingRegistry.worldMapBaritoneGoalHereKeybindSetting.getKeyBinding().matches(code, scanCode)) {
                BaritoneExecutor.goal(mouseBlockPosX, mouseBlockPosZ);
                cir.setReturnValue(true);
            } else if (XaeroPlusSettingRegistry.worldMapBaritonePathHereKeybindSetting.getKeyBinding().matches(code, scanCode)) {
                BaritoneExecutor.path(mouseBlockPosX, mouseBlockPosZ);
                cir.setReturnValue(true);
            } else if (BaritoneHelper.isBaritoneElytraPresent() && XaeroPlusSettingRegistry.worldMapBaritoneElytraHereKeybindSetting.getKeyBinding().matches(code, scanCode)) {
                BaritoneExecutor.elytra(mouseBlockPosX, mouseBlockPosZ);
                cir.setReturnValue(true);
            }
        }
    }

    @Inject(method = "getRightClickOptions", at = @At(value = "RETURN"), remap = false)
    public void getRightClickOptionsInject(final CallbackInfoReturnable<ArrayList<RightClickOption>> cir) {
        if (BaritoneHelper.isBaritonePresent()) {
            final ArrayList<RightClickOption> options = cir.getReturnValue();
            int goalX = rightClickX;
            int goalZ = rightClickZ;
            options.addAll(3, asList(
                    new RightClickOption("gui.world_map.baritone_goal_here", options.size(), this) {
                        @Override
                        public void onAction(Screen screen) {
                            BaritoneExecutor.goal(goalX, goalZ);
                        }
                    }.setNameFormatArgs(Misc.getKeyName(XaeroPlusSettingRegistry.worldMapBaritoneGoalHereKeybindSetting.getKeyBinding())),
                    new RightClickOption("gui.world_map.baritone_path_here", options.size(), this) {
                        @Override
                        public void onAction(Screen screen) {
                            BaritoneExecutor.path(goalX, goalZ);
                        }
                    }.setNameFormatArgs(Misc.getKeyName(XaeroPlusSettingRegistry.worldMapBaritonePathHereKeybindSetting.getKeyBinding()))
            ));
            if (BaritoneHelper.isBaritoneElytraPresent()) {
                options.addAll(5, asList(
                    new RightClickOption("gui.world_map.baritone_elytra_here", options.size(), this) {
                        @Override
                        public void onAction(Screen screen) {
                            BaritoneExecutor.elytra(goalX, goalZ);
                        }
                    }.setNameFormatArgs(Misc.getKeyName(XaeroPlusSettingRegistry.worldMapBaritoneElytraHereKeybindSetting.getKeyBinding()))
                ));
            }
        }

        if (XaeroPlusSettingRegistry.disableWaypointSharing.getValue()) {
            cir.getReturnValue().removeIf(option -> ((AccessorRightClickOption) option).getName().equals("gui.xaero_right_click_map_share_location"));
        }
    }

    @Unique
    public void addColoredLineToExistingBuffer(
            PoseStack.Pose matrices, VertexConsumer vertexBuffer, float x1, float y1, float x2, float y2, float r, float g, float b, float a
    ) {
        vertexBuffer.vertex(matrices.pose(), x1, y1, 0.0F).color(r, g, b, a).normal(matrices, x2 - x1, y2 - y1, 0.0F).endVertex();
        vertexBuffer.vertex(matrices.pose(), x2, y2, 0.0F).color(r, g, b, a).normal(matrices, x2 - x1, y2 - y1, 0.0F).endVertex();
    }

    public void onFollowButton(final Button b) {
        this.follow = !this.follow;
        this.init(Minecraft.getInstance(), width, height);
    }

    public void onGotoCoordinatesButton(final Button b) {
        if (xTextEntryField.isVisible() && zTextEntryField.isVisible()) {
            try {
                int x = Integer.parseInt(xTextEntryField.getValue());
                int z = Integer.parseInt(zTextEntryField.getValue());
                cameraX = x;
                cameraZ = z;
                this.follow = false;
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
