package xaeroplus.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
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
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
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
import xaero.map.mods.SupportMods;
import xaero.map.world.MapDimension;
import xaeroplus.Globals;
import xaeroplus.XaeroPlus;
import xaeroplus.feature.drawing.DrawingColorCyclerButton;
import xaeroplus.feature.render.line.Line;
import xaeroplus.feature.render.text.Text;
import xaeroplus.module.ModuleManager;
import xaeroplus.module.impl.*;
import xaeroplus.settings.Settings;
import xaeroplus.util.*;

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
    @Unique Button startDrawingButton;
    @Unique Button drawLineSegmentButton;
    @Unique Button drawInfiniteLineButton;
    @Unique Button drawHighlightsButton;
    @Unique Button drawTextButton;
    @Unique Button drawColorCyclerButton;
    @Unique boolean drawing = false;
    @Unique BlockPos drawInProgressPos = null;
    @Unique boolean drawingLeftClickDown = false;
    @Unique boolean drawingRightClickDown = false;
    @Unique boolean drawTextEntryActive = false;
    @Unique DrawingMode drawingMode = DrawingMode.LINE_SEGMENT;
    @Unique EditBox drawTextEntryField;
    @Unique List<Button> guiMapButtonTempList = new ArrayList<>();
    @Unique ResourceLocation xpGuiTextures = ResourceLocation.fromNamespaceAndPath("xaeroplus", "gui/xpgui.png");
    @Shadow private double cameraX = 0.0;
    @Shadow private double cameraZ = 0.0;
    @Shadow private int[] cameraDestination = null;
    @Shadow private SlowingAnimation cameraDestinationAnimX = null;
    @Shadow private SlowingAnimation cameraDestinationAnimZ = null;
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
    @Shadow private double scale;

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

    @Unique
    private Component xaeroPlus$prefix(Component component) {
        return Component.literal("[XP] ").append(component);
    }

    @Unique
    private Component xaeroPlus$keybindPrefix(Component component, KeyMapping bind) {
        return Component.empty()
            .append(Component.literal(Misc.getKeyName(bind) + " ").withStyle(ChatFormatting.DARK_GREEN))
            .append(component);
    }

    @Inject(method = "init", at = @At(value = "RETURN"), remap = true)
    public void customInitGui(CallbackInfo ci) {
        // left side
        followButton = new GuiTexturedButton(
            0, this.dimensionToggleButton.getY() - 20, 20, 20, this.follow ? 133 : 149, 16, 16, 16,
            WorldMap.guiTextures,
            this::onFollowButton,
            () -> new CursorBox(xaeroPlus$prefix(Component.translatable("xaeroplus.gui.world_map.toggle_follow_mode")
                .append(" " + I18n.get(this.follow ? "xaeroplus.gui.off" : "xaeroplus.gui.on")))),
            256, 256);
        addButton(followButton);
        coordinateGotoButton = new GuiTexturedButton(
            0, followButton.getY() - 20 , 20, 20, 229, 16, 16, 16,
            WorldMap.guiTextures,
            this::onGotoCoordinatesButton,
            () -> new CursorBox(xaeroPlus$prefix(Component.translatable("xaeroplus.gui.world_map.go_to_coordinates"))),
            256, 256);
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
        startDrawingButton = new GuiTexturedButton(
            0, this.coordinateGotoButton.getY() - 20, 20, 20, 47, 0, 16, 16,
            this.xpGuiTextures,
            (button -> onToggleDrawingButton()),
            () -> new CursorBox(
                xaeroPlus$keybindPrefix(xaeroPlus$prefix(
                    Component.translatable("xaeroplus.gui.world_map.start_drawing")
                ), Settings.REGISTRY.worldMapToggleDrawingKeybindSetting.getKeyBinding()
            )),
            256, 256);
        addButton(startDrawingButton);
        drawLineSegmentButton = new GuiTexturedButton(
            startDrawingButton.getX() + 16, startDrawingButton.getY(), 20, 20, 65, 0, 16, 16,
            this.xpGuiTextures,
            button -> setDrawingMode(DrawingMode.LINE_SEGMENT),
            () -> new CursorBox(xaeroPlus$prefix(Component.translatable("xaeroplus.gui.world_map.draw_line_segment"))),
            256, 256);
        drawLineSegmentButton.visible = false;
        drawInfiniteLineButton = new GuiTexturedButton(
            startDrawingButton.getX() + 16, drawLineSegmentButton.getY() + 20, 20, 20, 101, 0, 16, 16,
            this.xpGuiTextures,
            button -> setDrawingMode(DrawingMode.INFINITE_LINE),
            () -> new CursorBox(xaeroPlus$prefix(Component.translatable("xaeroplus.gui.world_map.draw_infinite_line"))),
            256, 256);
        drawInfiniteLineButton.visible = false;
        drawHighlightsButton = new GuiTexturedButton(
            startDrawingButton.getX() + 16, drawInfiniteLineButton.getY() + 20, 20, 20, 82, 0, 16, 16,
            this.xpGuiTextures,
            button -> setDrawingMode(DrawingMode.HIGHLIGHT),
            () -> new CursorBox(xaeroPlus$prefix(Component.translatable("xaeroplus.gui.world_map.draw_highlights"))),
            256, 256);
        drawHighlightsButton.visible = false;
        drawTextButton = new GuiTexturedButton(
            startDrawingButton.getX() + 16, drawHighlightsButton.getY() + 20, 20, 20, 118, 0, 16, 16,
            this.xpGuiTextures,
            button -> setDrawingMode(DrawingMode.TEXT),
            () -> new CursorBox(xaeroPlus$prefix(Component.translatable("xaeroplus.gui.world_map.draw_text"))),
            256, 256);
        drawTextButton.visible = false;
        drawColorCyclerButton = new DrawingColorCyclerButton(
            startDrawingButton.getX() + 16, drawTextButton.getY() + 20,
            () -> new CursorBox(xaeroPlus$prefix(Component.translatable("xaeroplus.gui.world_map.draw_color"))),
            ModuleManager.getModule(Drawing.class).getDrawingColorCycler());
        drawColorCyclerButton.visible = false;
        drawTextEntryField = new EditBox(Minecraft.getInstance().font, 0, 0, 150, 20, Component.nullToEmpty("Text:"));
        drawTextEntryField.setVisible(false);
        drawTextEntryField.setCursorPosition(0);
        drawTextEntryField.setHint(Component.literal("Text:").withStyle(ChatFormatting.DARK_GRAY));
        // right side
        if (!SupportMods.pac()) {  // remove useless button when pac is not installed
            this.removeWidget(this.claimsButton);
            this.exportButton.setY(this.claimsButton.getY());
            this.keybindingsButton.setY(this.claimsButton.getY() - 20);
            this.zoomOutButton.setY(this.keybindingsButton.getY() - 20);
            this.zoomInButton.setY(this.zoomOutButton.getY() - 20);
        }
        switchToEndButton = new GuiTexturedButton(
            this.width - 20, zoomInButton.getY() - 20, 20, 20, 31, 0, 16, 16,
            this.xpGuiTextures,
            (button -> onSwitchDimensionButton(END)),
            () -> new CursorBox(
                xaeroPlus$keybindPrefix(xaeroPlus$prefix(
                    Component.translatable("xaeroplus.keybind.switch_to_end")
                    ), Settings.REGISTRY.switchToEndSetting.getKeyBinding()
                )),
            256, 256
        );
        switchToOverworldButton = new GuiTexturedButton(
            this.width - 20, this.switchToEndButton.getY() - 20, 20, 20, 16, 0, 16, 16,
            this.xpGuiTextures,
            (button -> onSwitchDimensionButton(OVERWORLD)),
            () -> new CursorBox(
                xaeroPlus$keybindPrefix(xaeroPlus$prefix(
                    Component.translatable("xaeroplus.keybind.switch_to_overworld")
                    ), Settings.REGISTRY.switchToOverworldSetting.getKeyBinding()
                )),
            256, 256
        );
        switchToNetherButton = new GuiTexturedButton(
            this.width - 20, this.switchToOverworldButton.getY() - 20, 20, 20, 0, 0, 16, 16,
            this.xpGuiTextures,
            (button -> onSwitchDimensionButton(NETHER)),
            () -> new CursorBox(
                xaeroPlus$keybindPrefix(xaeroPlus$prefix(
                    Component.translatable("xaeroplus.keybind.switch_to_nether")
                    ), Settings.REGISTRY.switchToNetherSetting.getKeyBinding()
                )),
            256, 256
            );
        addButton(switchToEndButton);
        addButton(switchToOverworldButton);
        addButton(switchToNetherButton);
        pan = false;
        drawing = false;
    }

    @Unique
    private void setDrawingMode(DrawingMode drawingMode) {
        drawInProgressPos = null;
        ModuleManager.getModule(Drawing.class).removeInProgressLine();
        drawingLeftClickDown = false;
        drawingRightClickDown = false;
        drawTextEntryActive = false;
        this.drawingMode = drawingMode;
    }

    @Unique
    private void onToggleDrawingButton() {
        var prevDrawing = drawing;
        this.init(Minecraft.getInstance(), width, height);
        drawing = !prevDrawing;
        if (drawing) {
            addButton(drawLineSegmentButton);
            addButton(drawInfiniteLineButton);
            addButton(drawHighlightsButton);
            addButton(drawTextButton);
            addButton(drawColorCyclerButton);
            drawLineSegmentButton.visible = true;
            drawInfiniteLineButton.visible = true;
            drawHighlightsButton.visible = true;
            drawTextButton.visible = true;
            drawColorCyclerButton.visible = true;
        } else {
            xaeroPlus$stopDrawing();
        }
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
    public void toggleRadarWhileDimensionSwitched(final CallbackInfo ci, @Local(name = "currentFutureDim") MapDimension currentFutureDim) {
        if (!Settings.REGISTRY.radarWhileDimensionSwitchedSetting.get()) {
            WorldMap.settings.minimapRadar = currentFutureDim.getDimId() == ChunkUtils.getActualDimension();
        }
    }

    @Redirect(method = "render", at = @At(value = "FIELD", target = "Lxaero/map/gui/GuiMap;cameraX:D", opcode = Opcodes.PUTFIELD, ordinal = 1), remap = true)
    public void fixDimensionSwitchCameraCoordsX(GuiMap owner, double value, @Local(name = "playerDimDiv") double playerDimDiv) {
        this.cameraX *= prevPlayerDimDiv / playerDimDiv;
    }

    @Redirect(method = "render", at = @At(value = "FIELD", target = "Lxaero/map/gui/GuiMap;cameraZ:D", opcode = Opcodes.PUTFIELD, ordinal = 1), remap = true)
    public void fixDimensionSwitchCameraCoordsZ(GuiMap owner, double value, @Local(name = "playerDimDiv") double playerDimDiv) {
        this.cameraZ *= prevPlayerDimDiv / playerDimDiv;
    }

    @Inject(method = "render", at = @At(value = "FIELD", target = "Lxaero/map/gui/GuiMap;lastStartTime:J", opcode = Opcodes.PUTFIELD, ordinal = 0, shift = At.Shift.AFTER), remap = true)
    public void injectFollowMode(final CallbackInfo ci) {
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

    @WrapOperation(method = "render",
        slice = @Slice(
            from = @At(
                value = "FIELD",
                target = "Lxaero/map/gui/GuiMap;prevLoadingLeaves:Z",
                opcode = Opcodes.PUTFIELD
            ),
            to = @At(
                value = "INVOKE",
                target = "Lxaero/map/graphics/ImprovedFramebuffer;bindDefaultFramebuffer(Lnet/minecraft/client/Minecraft;)V"
            )
        ),
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;endBatch()V",
            ordinal = 0
        ),
        remap = true)
    public void drawWorldMapFeatures(final MultiBufferSource.BufferSource instance, final Operation<Void> original,
                                     @Local(name = "flooredCameraX") int flooredCameraX,
                                     @Local(name = "flooredCameraZ") int flooredCameraZ,
                                     @Local(name = "matrixStack") PoseStack matrixStack,
                                     @Local(name = "renderTypeBuffers") MultiBufferSource.BufferSource renderTypeBuffers,
                                     @Local(name = "fboScale") double fboScale
    ) {
        original.call(instance);
        if (Minecraft.getInstance().options.hideGui) return;
        Globals.drawManager.drawWorldMapFeatures(
            flooredCameraX,
            flooredCameraZ,
            matrixStack,
            fboScale,
            renderTypeBuffers
        );
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
        target = "Lxaero/map/element/MapElementRenderHandler;render(Lxaero/map/gui/GuiMap;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lxaero/map/graphics/renderer/multitexture/MultiTextureRenderTypeRendererProvider;DDIIDDDDDFZLxaero/map/element/HoveredMapElementHolder;Lnet/minecraft/client/Minecraft;F)Lxaero/map/element/HoveredMapElementHolder;"
    ), remap = true)
    public HoveredMapElementHolder<?, ?> hideMapElementsOnF1(MapElementRenderHandler handler, GuiMap mapScreen, MultiBufferSource.BufferSource renderTypeBuffers, MultiTextureRenderTypeRendererProvider rendererProvider, double cameraX, double cameraZ, int width, int height, double screenSizeBasedScale, double scale, double playerDimDiv, double mouseX, double mouseZ, float brightness, boolean cave, HoveredMapElementHolder<?, ?> oldHovered, Minecraft mc, float partialTicks, Operation<HoveredMapElementHolder<?, ?>> original) {
        if (!Minecraft.getInstance().options.hideGui) {
            return original.call(handler, mapScreen, renderTypeBuffers, rendererProvider, cameraX, cameraZ, width, height, screenSizeBasedScale, scale, playerDimDiv, mouseX, mouseZ, brightness, cave, oldHovered, mc, partialTicks);
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
            target = "Lxaero/map/graphics/MapRenderHelper;drawCenteredStringWithBackground(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIFFFF)V",
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

    @WrapWithCondition(method = "render", at = @At(
        value = "INVOKE",
        target = "Lxaero/map/graphics/MapRenderHelper;drawCenteredStringWithBackground(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIFFFF)V"
    ), remap = true)
    public boolean hideRenderedStringsOnF1(final GuiGraphics guiGraphics, final Font font, final String string, final int x, final int y, final int color, final float bgRed, final float bgGreen, final float bgBlue, final float bgAlpha) {
        return !Minecraft.getInstance().options.hideGui;
    }

    @WrapWithCondition(method = "render", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/ResourceLocation;IIFFIIII)V"
    ), remap = true)
    public boolean hideCompassOnF1(final GuiGraphics instance, final RenderPipeline renderPipeline, final ResourceLocation arg, final int i, final int j, final float f, final float g, final int k, final int l, final int m, final int n) {
        return !Minecraft.getInstance().options.hideGui;
    }

    @Inject(method = "render", at = @At(
        value = "INVOKE",
        target = "Lxaero/map/gui/ScreenBase;render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"
    ), remap = true)
    public void hideButtonsOnF1(final CallbackInfo ci) {
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
        if (mc.screen != null && mc.screen.getClass().equals(GuiMap.class)) {
            if (xTextEntryField.isVisible() && zTextEntryField.isVisible()) {
                xTextEntryField.render(guiGraphics, scaledMouseX, scaledMouseY, partialTicks);
                zTextEntryField.render(guiGraphics, scaledMouseX, scaledMouseY, partialTicks);
            }
            if (drawing && drawTextEntryActive && drawingMode == DrawingMode.TEXT && drawTextEntryField.visible) {
                drawTextEntryField.render(guiGraphics, scaledMouseX, scaledMouseY, partialTicks);
            }
        }
    }

    @WrapWithCondition(method = "render", at = @At(
        value = "INVOKE",
        target = "Lxaero/map/graphics/MapRenderHelper;drawCenteredStringWithBackground(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIFFFF)V"
    ), remap = true)
    public boolean hideMoreRenderedStringsOnF1(final GuiGraphics guiGraphics, final Font font, final Component text, final int x, final int y, final int color, final float bgRed, final float bgGreen, final float bgBlue, final float bgAlpha) {
        return !Minecraft.getInstance().options.hideGui;
    }

    @Inject(method = "onDimensionToggleButton", at = @At(value = "RETURN"))
    public void onDimensionToggleAfter(final Button b, final CallbackInfo ci) {
        if (!Settings.REGISTRY.radarWhileDimensionSwitchedSetting.get()) {
            WorldMap.settings.minimapRadar = mapProcessor.getMapWorld().getFutureDimensionId() == ChunkUtils.getActualDimension();
        }
    }

    // todo: mixin on mouseClicked to close coord entry fields when clicking on something else

    @Inject(method = "tick", at = @At("RETURN"), remap = true)
    public void onTick(final CallbackInfo ci) {
        if (!drawing) return;
        switch (drawingMode) {
            case LINE_SEGMENT -> {
                startDrawingButton.setFocused(false);
                drawLineSegmentButton.setFocused(true);
                drawInfiniteLineButton.setFocused(false);
                drawHighlightsButton.setFocused(false);
                drawTextButton.setFocused(false);
                drawColorCyclerButton.setFocused(false);
            }
            case INFINITE_LINE -> {
                startDrawingButton.setFocused(false);
                drawLineSegmentButton.setFocused(false);
                drawInfiniteLineButton.setFocused(true);
                drawHighlightsButton.setFocused(false);
                drawTextButton.setFocused(false);
                drawColorCyclerButton.setFocused(false);
            } case HIGHLIGHT -> {
                startDrawingButton.setFocused(false);
                drawLineSegmentButton.setFocused(false);
                drawInfiniteLineButton.setFocused(false);
                drawHighlightsButton.setFocused(true);
                drawTextButton.setFocused(false);
                drawColorCyclerButton.setFocused(false);
            } case TEXT -> {
                startDrawingButton.setFocused(false);
                drawLineSegmentButton.setFocused(false);
                drawInfiniteLineButton.setFocused(false);
                drawHighlightsButton.setFocused(false);
                drawTextButton.setFocused(true);
                drawColorCyclerButton.setFocused(false);
                if (drawTextEntryActive) {
                    drawTextEntryField.setEditable(true);
                    drawTextEntryField.setFocused(true);
                    setFocused(drawTextEntryField);
                }
            }
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    public void updateInProgressLine(CallbackInfo ci) {
        if (drawing) {
            switch (drawingMode) {
                case LINE_SEGMENT, INFINITE_LINE -> {
                    if (drawInProgressPos == null) {
                        ModuleManager.getModule(Drawing.class).removeInProgressLine();
                    } else {
                        var inProgress = ModuleManager.getModule(Drawing.class).snap(drawInProgressPos.getX(), drawInProgressPos.getZ(), mouseBlockPosX, mouseBlockPosZ, destScale);
                        ModuleManager.getModule(Drawing.class).setInProgressLine(inProgress, drawingMode);
                    }
                }
                case HIGHLIGHT -> {
                    ModuleManager.getModule(Drawing.class).removeInProgressLine();
                    if (drawingLeftClickDown) {
                        ModuleManager.getModule(Drawing.class).addHighlight(ChunkUtils.posToChunkPos(mouseBlockPosX), ChunkUtils.posToChunkPos(mouseBlockPosZ));
                    }
                }
            }
            if (drawingRightClickDown) {
                ModuleManager.getModule(Drawing.class).removeHighlight(ChunkUtils.posToChunkPos(mouseBlockPosX), ChunkUtils.posToChunkPos(mouseBlockPosZ));
                ModuleManager.getModule(Drawing.class).removeLine(mouseBlockPosX, mouseBlockPosZ);
                ModuleManager.getModule(Drawing.class).removeText(mouseBlockPosX, mouseBlockPosZ, getFboScale());
            }
        }
    }

    @Unique
    private float getFboScale() {
        float fboScale;
        if (this.scale >= 1.0) {
            fboScale = (float) Math.max(1.0, Math.floor(this.scale));
        } else {
            fboScale = (float) this.scale;
        }
        return fboScale;
    }

    @Inject(method = "render", at = @At(
        value = "INVOKE",
        target = "Lxaero/map/mods/SupportXaeroMinimap;getSubWorldNameToRender()Ljava/lang/String;"
    ))
    public void renderDrawingStatusText(final GuiGraphics guiGraphics, final int scaledMouseX, final int scaledMouseY, final float partialTicks, final CallbackInfo ci) {
        if (!drawing) return;
        MapRenderHelper.drawCenteredStringWithBackground(
            guiGraphics, Minecraft.getInstance().font,
            "[XP] " + I18n.get("xaeroplus.gui.world_map.drawing_mode"),
            this.width / 2,
            24,
            -1,
            0.0F, 0.0F, 0.0F, 0.4F
        );
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true, remap = true)
    public void cancelClicksWhileDrawing(final double par1, final double par2, final int par3, final CallbackInfoReturnable<Boolean> cir) {
        if (!drawing) return;
        boolean toReturn = super.mouseClicked(par1, par2, par3);
        if (toReturn) {
            cir.setReturnValue(true);
            return;
        }
        if (par3 == 0) { // start drawing on left click
            drawingLeftClickDown = true;
            switch (drawingMode) {
                case LINE_SEGMENT, INFINITE_LINE, TEXT -> {
                    if (drawInProgressPos == null) {
                        drawInProgressPos = new BlockPos(mouseBlockPosX, 0, mouseBlockPosZ);
                    }
                }
            }
            if (drawingMode == DrawingMode.TEXT && !drawTextEntryActive) {
                drawTextEntryActive = true;
                drawTextEntryField.setX(Mth.clamp((int) par1 - (drawTextEntryField.getWidth() / 2), 5, width - drawTextEntryField.getWidth() - 5));
                drawTextEntryField.setY(Mth.clamp((int) par2 - (drawTextEntryField.getHeight() / 2), 5, height - drawTextEntryField.getHeight() - 5));
                addWidget(drawTextEntryField);
                drawTextEntryField.setVisible(true);
                drawTextEntryField.setCursorPosition(0);
                drawTextEntryField.setHint(Component.literal("Text:").withStyle(ChatFormatting.DARK_GRAY));
                setFocused(drawTextEntryField);
            }
            cir.setReturnValue(true);
        } else if (par3 == 1) {
            drawingRightClickDown = true;
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true, remap = true)
    public void drawingClickReleasedHandler(final double par1, final double par2, final int par3, final CallbackInfoReturnable<Boolean> cir) {
        if (!drawing) return;
        boolean toReturn = super.mouseReleased(par1, par2, par3);
        if (toReturn) {
            cir.setReturnValue(true);
            return;
        }
        if (par3 == 0) { // start drawing on left click
            switch (drawingMode) {
                case LINE_SEGMENT, INFINITE_LINE -> {
                    if (drawInProgressPos != null) {
                        Line line;
                        line = ModuleManager.getModule(Drawing.class).snap(drawInProgressPos.getX(), drawInProgressPos.getZ(), mouseBlockPosX, mouseBlockPosZ, destScale);
                        switch (drawingMode) {
                            case LINE_SEGMENT -> ModuleManager.getModule(Drawing.class).addLine(line);
                            case INFINITE_LINE -> ModuleManager.getModule(Drawing.class).addInfiniteLine(line);
                        }
                        drawInProgressPos = null;
                    }
                }
            }
            drawingLeftClickDown = false;
            cir.setReturnValue(true);
        } else if (par3 == 1) { // clear drawing on right click
            drawingRightClickDown = false;
            if (drawInProgressPos != null) return;
            ModuleManager.getModule(Drawing.class).removeLine(mouseBlockPosX, mouseBlockPosZ);
            ModuleManager.getModule(Drawing.class).removeText(mouseBlockPosX, mouseBlockPosZ, getFboScale());
            cir.setReturnValue(true);
        }
    }

    @Unique
    private void xaeroPlus$stopDrawing() {
        drawing = false;
        drawInProgressPos = null;
        ModuleManager.getModule(Drawing.class).removeInProgressLine();
        drawingLeftClickDown = false;
        drawingRightClickDown = false;
        drawTextEntryActive = false;
        removeWidget(drawLineSegmentButton);
        removeWidget(drawInfiniteLineButton);
        removeWidget(drawHighlightsButton);
        removeWidget(drawTextButton);
        removeWidget(drawColorCyclerButton);
        removeWidget(drawTextEntryField);
        drawLineSegmentButton.visible = false;
        drawInfiniteLineButton.visible = false;
        drawHighlightsButton.visible = false;
        drawTextButton.visible = false;
        drawColorCyclerButton.visible = false;
        drawTextEntryField.visible = false;
        this.init(Minecraft.getInstance(), width, height);
    }

    @Inject(method = "onInputPress", at = @At("HEAD"))
    public void panMouseButtonClick(final InputConstants.Type type, final int code, final CallbackInfoReturnable<Boolean> cir) {
        if (type != InputConstants.Type.MOUSE) return;
        if (code != GLFW_MOUSE_BUTTON_MIDDLE) return;
        pan = true;
        var mc = Minecraft.getInstance();
        panMouseStartX = Misc.getMouseX(mc, true);
        panMouseStartY = Misc.getMouseY(mc, true);
    }

    @Inject(method = "onInputRelease", at = @At("HEAD"), cancellable = true)
    public void panMouseButtonRelease(final InputConstants.Type type, final int code, final CallbackInfoReturnable<Boolean> cir) {
        if (drawing) {
            if (type == InputConstants.Type.KEYSYM && code == GLFW_KEY_ESCAPE) {
                xaeroPlus$stopDrawing();
                cir.setReturnValue(true);
                return;
            }
            if (drawTextEntryActive) {
                if (type == InputConstants.Type.KEYSYM && code == GLFW_KEY_ENTER) {
                    String value = drawTextEntryField.getValue();
                    if (!value.isEmpty()) {
                        var text = new Text(value, drawInProgressPos.getX(), drawInProgressPos.getZ(), ColorHelper.getColor(255, 255, 255, 255), 1f);
                        ModuleManager.getModule(Drawing.class).addText(text);
                        xaeroPlus$stopDrawing();
                        cir.setReturnValue(true);
                        return;
                    }
                }
            }
        }
        if (type != InputConstants.Type.MOUSE) return;
        if (code != GLFW_MOUSE_BUTTON_MIDDLE) return;
        pan = false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return !drawing;
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
        final CallbackInfo ci
    ) {
        MapTileSelection selection = this.mapTileSelection;
        if (selection == null) return;
        var sideLen = Math.abs(selection.getRight() - selection.getLeft())+1;
        var heightLen = Math.abs(selection.getBottom() - selection.getTop())+1;
        if (sideLen <= 1 && heightLen <= 1) return;
        // todo: it'd be better if we could render this directly on the highlight
        //  but we need a function for map -> screen coordinates translation
        MapRenderHelper.drawCenteredStringWithBackground(guiGraphics, font, sideLen + " x " + heightLen, scaledMouseX, scaledMouseY - font.lineHeight, -1, 0.0f, 0.0f, 0.0f, 0.4f);
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
        if (Settings.REGISTRY.worldMapToggleDrawingKeybindSetting.getKeyBinding().matches(code, scanCode)) {
            onToggleDrawingButton();
            cir.setReturnValue(true);
            return;
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
        boolean tileSelPresent = this.mapTileSelection != null;
        final int delHighlightMinX = tileSelPresent ? mapTileSelection.getStartX() : rightClickX;
        final int delHighlightMaxX = tileSelPresent ? mapTileSelection.getEndX() : rightClickX;
        final int delHighlightMinZ = tileSelPresent ? mapTileSelection.getStartZ() : rightClickZ;
        final int delHighlightMaxZ = tileSelPresent ? mapTileSelection.getEndZ() : rightClickZ;
        options.add(index++, new RightClickOption("xaeroplus.gui.world_map.delete_highlights", options.size(), this) {
            @Override
            public void onAction(final Screen screen) {
                var dim = Globals.getCurrentDimensionId();
                for (int x = delHighlightMinX; x <= delHighlightMaxX; x++) {
                    for (int z = delHighlightMinZ; z <= delHighlightMaxZ; z++) {
                        var breadcrumbs = ModuleManager.getModule(Breadcrumbs.class);
                        if (breadcrumbs.isEnabled()) {
                            breadcrumbs.breadcrumbsCache.get().removeHighlight(x, z, dim);
                        }
                        var liquidNewChunks = ModuleManager.getModule(LiquidNewChunks.class);
                        if (liquidNewChunks.isEnabled()) {
                            liquidNewChunks.newChunksCache.get().removeHighlight(x, z, dim);
                            liquidNewChunks.inverseNewChunksCache.get().removeHighlight(x, z, dim);
                        }
                        var oldbiomes = ModuleManager.getModule(OldBiomes.class);
                        if (oldbiomes.isEnabled()) {
                            oldbiomes.oldBiomesCache.get().removeHighlight(x, z, dim);
                        }
                        var oldChunks = ModuleManager.getModule(OldChunks.class);
                        if (oldChunks.isEnabled()) {
                            oldChunks.oldChunksCache.get().removeHighlight(x, z, dim);
                            oldChunks.modernChunksCache.get().removeHighlight(x, z, dim);
                        }
                        var paletteNewChunks = ModuleManager.getModule(PaletteNewChunks.class);
                        if (paletteNewChunks.isEnabled()) {
                            paletteNewChunks.newChunksCache.get().removeHighlight(x, z, dim);
                            paletteNewChunks.newChunksInverseCache.get().removeHighlight(x, z, dim);
                        }
                        var portals = ModuleManager.getModule(Portals.class);
                        if (portals.isEnabled()) {
                            portals.portalsCache.get().removeHighlight(x, z, dim);
                        }
                        var lavaColumns = ModuleManager.getModule(LavaColumns.class);
                        if (lavaColumns.isEnabled()) {
                            lavaColumns.lavaColumnsCache.get().removeHighlight(x, z, dim);
                        }
                        ModuleManager.getModule(Drawing.class).removeHighlight(x, z);
                        ModuleManager.getModule(Drawing.class).removeLine(ChunkUtils.chunkCoordToCoord(x), ChunkUtils.chunkCoordToCoord(z));
                        ModuleManager.getModule(Drawing.class).removeText(ChunkUtils.chunkCoordToCoord(x), ChunkUtils.chunkCoordToCoord(z), 1);
                    }
                }
            }
        });

        if (Settings.REGISTRY.disableWaypointSharing.get()) {
            options.removeIf(option -> ((AccessorRightClickOption) option).getName().equals("gui.xaero_right_click_map_share_location"));
        }

        if (!Settings.REGISTRY.showCoordsInRightClickOptions.get()) {
            options.removeIf(option -> {
                var name = ((AccessorRightClickOption) option).getName();
                return name.startsWith("C: (") || name.startsWith("X: ");
            });
        }
    }

    @Unique
    public void onFollowButton(final Button b) {
        follow = !follow;
        this.init(Minecraft.getInstance(), width, height);
    }

    @Unique
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
                xTextEntryField.visible = false;
                zTextEntryField.visible = false;
            }
        } else {
            this.init(Minecraft.getInstance(), width, height);
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
