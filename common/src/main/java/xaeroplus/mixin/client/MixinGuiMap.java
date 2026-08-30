package xaeroplus.mixin.client;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.shaders.BlendMode;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
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
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import org.joml.Matrix4f;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.lib.client.config.ClientConfigManager;
import xaero.lib.client.controls.util.KeyMappingUtils;
import xaero.lib.client.graphics.shader.LibShaders;
import xaero.lib.client.gui.ScreenBase;
import xaero.lib.client.gui.widget.Tooltip;
import xaero.lib.common.config.option.ConfigOption;
import xaero.map.MapProcessor;
import xaero.map.WorldMap;
import xaero.map.animation.SlowingAnimation;
import xaero.map.common.config.option.WorldMapProfiledConfigOptions;
import xaero.map.config.util.WorldMapClientConfigUtils;
import xaero.map.graphics.MapRenderHelper;
import xaero.map.graphics.renderer.multitexture.MultiTextureRenderTypeRenderer;
import xaero.map.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider;
import xaero.map.gui.GuiMap;
import xaero.map.gui.GuiTexturedButton;
import xaero.map.gui.IRightClickableElement;
import xaero.map.gui.MapTileSelection;
import xaero.map.gui.dropdown.rightclick.RightClickOption;
import xaero.map.misc.Misc;
import xaero.map.mods.SupportMods;
import xaero.map.world.MapDimension;
import xaeroplus.Globals;
import xaeroplus.XaeroPlus;
import xaeroplus.feature.drawing.ColorPickerWidget;
import xaeroplus.feature.drawing.DrawingColorPickerButton;
import xaeroplus.feature.extensions.CustomWorldMapShader;
import xaeroplus.feature.render.line.Line;
import xaeroplus.feature.render.text.Text;
import xaeroplus.module.ModuleManager;
import xaeroplus.module.impl.*;
import xaeroplus.settings.Settings;
import xaeroplus.util.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;

import static net.minecraft.world.level.Level.*;
import static org.lwjgl.glfw.GLFW.*;
import static xaeroplus.Globals.getCurrentDimensionId;

@Mixin(value = GuiMap.class, remap = false)
public abstract class MixinGuiMap extends ScreenBase implements IRightClickableElement {
    @Unique boolean pan;
    @Unique double panMouseStartX;
    @Unique double panMouseStartY;
    @Unique Button switchToNetherButton;
    @Unique Button switchToOverworldButton;
    @Unique Button switchToEndButton;
    @Unique Button startDrawingButton;
    @Unique Button drawLineSegmentButton;
    @Unique Button drawInfiniteLineButton;
    @Unique Button drawHighlightsButton;
    @Unique Button drawEllipseButton;
    @Unique Button drawTextButton;
    @Unique DrawingColorPickerButton drawColorPickerButton;
    @Unique ColorPickerWidget drawColorPicker;
    @Unique Button drawMeasurementToolButton;
    @Unique Button exitButton;
    @Unique boolean drawing = false;
    @Unique BlockPos drawInProgressPos = null;
    @Unique boolean drawingLeftClickDown = false;
    @Unique boolean drawingRightClickDown = false;
    @Unique boolean drawTextEntryActive = false;
    @Unique DrawingMode drawingMode = DrawingMode.LINE_SEGMENT;
    @Unique boolean colorPickerActive = false;
    @Unique EditBox drawTextEntryField;
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
    @Shadow private Button attachedCameraButton;
    @Shadow private int rightClickX;
    @Shadow private int rightClickY;
    @Shadow private int rightClickZ;
    @Shadow private int mouseBlockPosX;
    @Shadow private int mouseBlockPosY;
    @Shadow private int mouseBlockPosZ;
    @Shadow private static double destScale;
    @Shadow private MapTileSelection mapTileSelection;
    @Shadow private double scale;
    @Shadow public static boolean hiddenUI;

    protected MixinGuiMap(final Screen parent, final Screen escape, final Component titleIn) {
        super(parent, escape, titleIn);
    }

    @Shadow public abstract <T extends GuiEventListener & Renderable & NarratableEntry> T addButton(final T guiEventListener);
    @Shadow public abstract <T extends GuiEventListener & NarratableEntry> T addWidget(final T guiEventListener);

    @Unique
    private Component xaeroPlus$prefix(Component component) {
        return Component.literal("[XP] ").append(component);
    }

    @Unique
    private Component xaeroPlus$keybindPrefix(Component component, KeyMapping bind) {
        return Component.empty()
            .append(Component.literal(KeyMappingUtils.getKeyName(bind) + " ").withStyle(ChatFormatting.DARK_GREEN))
            .append(component);
    }

    @Inject(method = "init", at = @At(value = "RETURN"), remap = true)
    public void customInitGui(CallbackInfo ci) {
        startDrawingButton = new GuiTexturedButton(
            0, this.attachedCameraButton.getY() - 20, 20, 20, 47, 0, 16, 16,
            Globals.guiTextures,
            (button -> onToggleDrawingButton()),
            () -> new Tooltip(
                xaeroPlus$keybindPrefix(xaeroPlus$prefix(
                    Component.translatable("xaeroplus.gui.world_map.start_drawing")
                ), Settings.REGISTRY.worldMapToggleDrawingKeybindSetting.getKeyBinding()
            )));
        drawLineSegmentButton = new GuiTexturedButton(
            startDrawingButton.getX() + 16, startDrawingButton.getY(), 20, 20, 65, 0, 16, 16,
            Globals.guiTextures,
            button -> setDrawingMode(DrawingMode.LINE_SEGMENT),
            () -> new Tooltip(xaeroPlus$prefix(Component.translatable("xaeroplus.gui.world_map.draw_line_segment"))));
        drawLineSegmentButton.visible = false;
        drawInfiniteLineButton = new GuiTexturedButton(
            startDrawingButton.getX() + 16, drawLineSegmentButton.getY() + 20, 20, 20, 101, 0, 16, 16,
            Globals.guiTextures,
            button -> setDrawingMode(DrawingMode.INFINITE_LINE),
            () -> new Tooltip(xaeroPlus$prefix(Component.translatable("xaeroplus.gui.world_map.draw_infinite_line"))));
        drawInfiniteLineButton.visible = false;
        drawHighlightsButton = new GuiTexturedButton(
            startDrawingButton.getX() + 16, drawInfiniteLineButton.getY() + 20, 20, 20, 82, 0, 16, 16,
            Globals.guiTextures,
            button -> setDrawingMode(DrawingMode.HIGHLIGHT),
            () -> new Tooltip(xaeroPlus$prefix(Component.translatable("xaeroplus.gui.world_map.draw_highlights"))));
        drawHighlightsButton.visible = false;
        drawEllipseButton = new GuiTexturedButton(
            startDrawingButton.getX() + 16, drawHighlightsButton.getY() + 20, 20, 20, 137, 19, 17, 17,
            Globals.guiTextures,
            button -> setDrawingMode(DrawingMode.ELLIPSE),
            () -> new Tooltip(xaeroPlus$prefix(Component.translatable("xaeroplus.gui.world_map.draw_ellipse"))));
        drawEllipseButton.visible = false;
        drawTextButton = new GuiTexturedButton(
            startDrawingButton.getX() + 16, drawEllipseButton.getY() + 20, 20, 20, 119, 0, 16, 16,
            Globals.guiTextures,
            button -> setDrawingMode(DrawingMode.TEXT),
            () -> new Tooltip(xaeroPlus$prefix(Component.translatable("xaeroplus.gui.world_map.draw_text"))));
        drawTextButton.visible = false;
        var drawingModule = ModuleManager.getModule(Drawing.class);
        drawColorPickerButton = new DrawingColorPickerButton(
            startDrawingButton.getX() + 16,
            drawTextButton.getY() + 20,
            () -> new Tooltip(xaeroPlus$prefix(Component.translatable("xaeroplus.gui.world_map.draw_color"))),
            drawingModule::getDrawingColor,
            button -> onColorPickerButton()
        );
        drawColorPickerButton.visible = false;
        var colorPickerSize = 108;
        var colorPickerX = drawColorPickerButton.getX() + drawColorPickerButton.getWidth() + 4;
        var colorPickerY = startDrawingButton.getY() - 46;
        drawColorPicker = new ColorPickerWidget(
            colorPickerX,
            colorPickerY,
            colorPickerSize,
            drawingModule.getDrawingColor(),
            drawingModule::setDrawingColor
        );
        drawColorPicker.visible = false;
        drawMeasurementToolButton = new GuiTexturedButton(
            startDrawingButton.getX() + 16, drawColorPickerButton.getY() + 20, 20, 20, 135, 0, 16, 16,
            Globals.guiTextures,
            button -> setDrawingMode(DrawingMode.MEASUREMENT),
            () -> new Tooltip(xaeroPlus$prefix(Component.translatable("xaeroplus.gui.world_map.draw_measurement_tool"))));
        drawMeasurementToolButton.visible = false;
        drawTextEntryField = new EditBox(Minecraft.getInstance().font, 0, 0, 150, 20, Component.nullToEmpty("Text:"));
        drawTextEntryField.setVisible(false);
        drawTextEntryField.setCursorPosition(0);
        drawTextEntryField.setHint(Component.literal("Text:").withStyle(ChatFormatting.DARK_GRAY));
        // right side
        switchToEndButton = new GuiTexturedButton(
            this.width - 20, zoomInButton.getY() - 20, 20, 20, 117, 19, 16, 16,
            Globals.guiTextures,
            (button -> onSwitchDimensionButton(END)),
            () -> new Tooltip(
                xaeroPlus$keybindPrefix(xaeroPlus$prefix(
                    Component.translatable("xaeroplus.keybind.switch_to_end")
                    ), Settings.REGISTRY.switchToEndSetting.getKeyBinding()
                ))
        );
        switchToOverworldButton = new GuiTexturedButton(
            this.width - 20, this.switchToEndButton.getY() - 20, 20, 20, 98, 18, 16, 16,
            Globals.guiTextures,
            (button -> onSwitchDimensionButton(OVERWORLD)),
            () -> new Tooltip(
                xaeroPlus$keybindPrefix(xaeroPlus$prefix(
                    Component.translatable("xaeroplus.keybind.switch_to_overworld")
                    ), Settings.REGISTRY.switchToOverworldSetting.getKeyBinding()
                ))
        );
        switchToNetherButton = new GuiTexturedButton(
            this.width - 20, this.switchToOverworldButton.getY() - 20, 20, 20, 79, 19, 16, 16,
            Globals.guiTextures,
            (button -> onSwitchDimensionButton(NETHER)),
            () -> new Tooltip(
                xaeroPlus$keybindPrefix(xaeroPlus$prefix(
                    Component.translatable("xaeroplus.keybind.switch_to_nether")
                    ), Settings.REGISTRY.switchToNetherSetting.getKeyBinding()
                ))
            );
        exitButton = new GuiTexturedButton(
            // invisible button on the compass
            this.width - 34, 2, 32, 32, 0, 0, 0, 0,
            Globals.guiTextures,
            (button -> onClose()),
            () -> new Tooltip(xaeroPlus$prefix(Component.translatable("xaeroplus.gui.world_map.exit")))
        );
        pan = false;
        drawing = false;

        if (!Settings.REGISTRY.worldMapUIAdditions.get()) return;

        if (!SupportMods.pac()) {  // remove useless button when pac is not installed
            this.removeWidget(this.claimsButton);
            this.exportButton.setY(this.claimsButton.getY());
            this.keybindingsButton.setY(this.claimsButton.getY() - 20);
            this.zoomOutButton.setY(this.keybindingsButton.getY() - 20);
            this.zoomInButton.setY(this.zoomOutButton.getY() - 20);
            this.switchToEndButton.setY(this.zoomInButton.getY() - 20);
            this.switchToOverworldButton.setY(this.switchToEndButton.getY() - 20);
            this.switchToNetherButton.setY(this.switchToOverworldButton.getY() - 20);
        }
        addButton(startDrawingButton);
        addButton(switchToEndButton);
        addButton(switchToOverworldButton);
        addButton(switchToNetherButton);
        addButton(exitButton);
    }

    @Unique
    private void setDrawingMode(DrawingMode drawingMode) {
        drawInProgressPos = null;
        ModuleManager.getModule(Drawing.class).removeInProgressLine();
        ModuleManager.getModule(Drawing.class).removeInProgressEllipse();
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
            addButton(drawEllipseButton);
            addButton(drawTextButton);
            addButton(drawColorPickerButton);
            addButton(drawColorPicker);
            addButton(drawMeasurementToolButton);
            drawLineSegmentButton.visible = true;
            drawInfiniteLineButton.visible = true;
            drawHighlightsButton.visible = true;
            drawEllipseButton.visible = true;
            drawTextButton.visible = true;
            drawColorPickerButton.visible = true;
            drawColorPicker.visible = colorPickerActive;
            drawMeasurementToolButton.visible = true;
        } else {
            xaeroPlus$stopDrawing();
        }
    }

    @Unique
    private void onColorPickerButton() {
        if (drawing) {
            colorPickerActive = !colorPickerActive;
            drawColorPicker.visible = colorPickerActive;
        } else {
            colorPickerActive = false;
            drawColorPicker.visible = false;
        }
    }

    @Override
    public void onExit(Screen screen) {
        if (!Settings.REGISTRY.persistMapDimensionSwitchSetting.get()) {
            try {
                var actualDimension = ChunkUtils.getActualDimension();
                if (Globals.getCurrentDimensionId() != actualDimension) {
                    Globals.switchToDimension(actualDimension);
                    if (!Settings.REGISTRY.radarWhileDimensionSwitchedSetting.get()) {
                        trySettingCurrentProfileOption(WorldMapProfiledConfigOptions.MINIMAP_RADAR, true);
                    }
                }
            } catch (final Exception e) {
                XaeroPlus.LOGGER.error("Failed to switch back to original dimension", e);
            }
        }
        super.onExit(screen);
    }

    @WrapOperation(method = "changeZoom", at = @At(
        value = "INVOKE",
        target = "Lxaero/map/gui/GuiMap;applyZoomLimits()V"
    ))
    public void trulyUnlimitedWorldMapZoom0(final GuiMap instance, final Operation<Void> original) {
        if (Settings.REGISTRY.trulyUnlimitedWorldMapZoom.get()) {
            return;
        }
        original.call(instance);
    }

    @WrapOperation(method = "render", at = @At(
        value = "INVOKE",
        target = "Lxaero/map/gui/GuiMap;applyZoomLimits()V"
    ))
    public void trulyUnlimitedWorldMapZoom1(final GuiMap instance, final Operation<Void> original) {
        if (Settings.REGISTRY.trulyUnlimitedWorldMapZoom.get()) {
            return;
        }
        original.call(instance);
    }

    @ModifyArg(method = "render", at = @At(
        value = "INVOKE",
        target = "Lxaero/map/graphics/MapRenderHelper;drawCenteredStringWithBackground(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIFFFFLcom/mojang/blaze3d/vertex/VertexConsumer;)V",
        ordinal = 0
    ),
        slice = @Slice(
            from = @At(
                value = "FIELD",
                opcode = Opcodes.GETSTATIC,
                target = "Lxaero/map/common/config/option/WorldMapProfiledConfigOptions;DISPLAY_ZOOM:Lxaero/lib/common/config/option/BooleanConfigOption;"
            )
        ),
        index = 2
    )
    public String trulyUnlimitedWorldMapZoomStrPrecisionFix2(final String zoomString) {
        if (Settings.REGISTRY.trulyUnlimitedWorldMapZoom.get()) {
            if (destScale < 0.0005 && destScale > 0) {
                int decimalPlaces = Math.max(
                    3,
                    2 - (int) Math.floor(Math.log10(destScale))
                );
                return BigDecimal.valueOf(destScale)
                    .setScale(decimalPlaces, RoundingMode.HALF_UP)
                    .toPlainString() + "x";
            }
        }
        return zoomString;
    }

    @Inject(method = "render", at = @At(
        value = "INVOKE",
        target = "Lxaero/map/gui/GuiMap;init(Lnet/minecraft/client/Minecraft;II)V",
        ordinal = 1,
        shift = At.Shift.AFTER
    ), remap = true)
    public void toggleRadarWhileDimensionSwitched(final CallbackInfo ci, @Local(name = "futureDimension") MapDimension futureDimension) {
        if (!Settings.REGISTRY.radarWhileDimensionSwitchedSetting.get() && futureDimension != null) {
            trySettingCurrentProfileOption(WorldMapProfiledConfigOptions.MINIMAP_RADAR, futureDimension.getDimId() == ChunkUtils.getActualDimension());
        }
    }

    private static void trySettingCurrentProfileOption(ConfigOption<Boolean> option, boolean value) {
        ClientConfigManager configManager = WorldMap.INSTANCE.getConfigs().getClientConfigManager();
        var currentValue = configManager.getEffective(option);
        if (currentValue != value) {
            WorldMapClientConfigUtils.tryTogglingCurrentProfileOption(option);
        }
    }

    // todo: verify
    @Redirect(method = "render", at = @At(value = "FIELD", target = "Lxaero/map/gui/GuiMap;cameraX:D", opcode = Opcodes.PUTFIELD, ordinal = 1), remap = true)
    public void fixDimensionSwitchCameraCoordsX(GuiMap owner, double value, @Local(name = "playerDimDiv") double playerDimDiv) {
        this.cameraX *= prevPlayerDimDiv / playerDimDiv;
    }

    @Redirect(method = "render", at = @At(value = "FIELD", target = "Lxaero/map/gui/GuiMap;cameraZ:D", opcode = Opcodes.PUTFIELD, ordinal = 1), remap = true)
    public void fixDimensionSwitchCameraCoordsZ(GuiMap owner, double value, @Local(name = "playerDimDiv") double playerDimDiv) {
        this.cameraZ *= prevPlayerDimDiv / playerDimDiv;
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
        if (hiddenUI) return;
        Globals.drawManager.drawWorldMapFeatures(
            flooredCameraX,
            flooredCameraZ,
            matrixStack,
            fboScale,
            renderTypeBuffers
        );
    }

    @ModifyArg(method = "render",
        slice = @Slice(
            from = @At(
                value = "FIELD",
                opcode = Opcodes.GETSTATIC,
                target = "Lxaero/map/common/config/option/WorldMapProfiledConfigOptions;COORDINATES:Lxaero/lib/common/config/option/BooleanConfigOption;"
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
        value = "INVOKE",
        target = "Lxaero/map/graphics/MapRenderHelper;restoreDefaultShaderBlendState()V"
    ), remap = true)
    public void renderCoordinatesGotoTextEntryFields(final GuiGraphics guiGraphics, final int scaledMouseX, final int scaledMouseY, final float partialTicks, final CallbackInfo ci) {
        if (!Settings.REGISTRY.worldMapUIAdditions.get()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null && mc.screen.getClass().equals(GuiMap.class)) {
            if (drawing && drawTextEntryActive && drawingMode == DrawingMode.TEXT && drawTextEntryField.visible) {
                drawTextEntryField.render(guiGraphics, scaledMouseX, scaledMouseY, partialTicks);
            }
        }
    }

    @Inject(method = "onDimensionToggleButton", at = @At(value = "RETURN"))
    public void onDimensionToggleAfter(final Button b, final CallbackInfo ci) {
        if (!Settings.REGISTRY.radarWhileDimensionSwitchedSetting.get()) {
            trySettingCurrentProfileOption(WorldMapProfiledConfigOptions.MINIMAP_RADAR, mapProcessor.getMapWorld().getFutureDimensionId() == ChunkUtils.getActualDimension());
        }
    }

    // todo: mixin on mouseClicked to close coord entry fields when clicking on something else

    @Inject(method = "tick", at = @At("RETURN"), remap = true)
    public void onTick(final CallbackInfo ci) {
        if (!Settings.REGISTRY.worldMapUIAdditions.get()) return;
        if (!drawing) return;
        startDrawingButton.setFocused(false);
        drawLineSegmentButton.setFocused(drawingMode == DrawingMode.LINE_SEGMENT);
        drawInfiniteLineButton.setFocused(drawingMode == DrawingMode.INFINITE_LINE);
        drawHighlightsButton.setFocused(drawingMode == DrawingMode.HIGHLIGHT);
        drawEllipseButton.setFocused(drawingMode == DrawingMode.ELLIPSE);
        drawTextButton.setFocused(drawingMode == DrawingMode.TEXT);
        drawColorPickerButton.setFocused(false);
        drawMeasurementToolButton.setFocused(drawingMode == DrawingMode.MEASUREMENT);
        if (drawingMode == DrawingMode.TEXT && drawTextEntryActive) {
            drawTextEntryField.setEditable(true);
            drawTextEntryField.setFocused(true);
            setFocused(drawTextEntryField);
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    public void updateInProgressLine(CallbackInfo ci) {
        if (drawing) {
            var drawingModule = ModuleManager.getModule(Drawing.class);
            if (drawingMode != DrawingMode.ELLIPSE) drawingModule.removeInProgressEllipse();
            switch (drawingMode) {
                case LINE_SEGMENT, INFINITE_LINE -> {
                    if (drawInProgressPos == null) {
                        drawingModule.removeInProgressLine();
                    } else {
                        var inProgress = drawingModule.snap(drawInProgressPos.getX(), drawInProgressPos.getZ(), mouseBlockPosX, mouseBlockPosZ, destScale);
                        drawingModule.setInProgressLine(inProgress, drawingMode);
                    }
                }
                case HIGHLIGHT -> {
                    drawingModule.removeInProgressLine();
                    if (drawingLeftClickDown) {
                        drawingModule.addHighlight(ChunkUtils.posToChunkPos(mouseBlockPosX), ChunkUtils.posToChunkPos(mouseBlockPosZ));
                    }
                }
                case ELLIPSE -> {
                    drawingModule.removeInProgressLine();
                    if (drawInProgressPos == null) {
                        drawingModule.removeInProgressEllipse();
                    } else {
                        var ellipse = drawingModule.snapEllipse(
                            drawInProgressPos.getX(),
                            drawInProgressPos.getZ(),
                            mouseBlockPosX,
                            mouseBlockPosZ,
                            destScale
                        );
                        if (ellipse == null) {
                            drawingModule.removeInProgressEllipse();
                        } else {
                            drawingModule.setInProgressEllipse(ellipse);
                        }
                    }
                }
                case MEASUREMENT -> {
                    if (drawInProgressPos == null) {
                        drawingModule.removeInProgressLine();
                    } else {
                        drawingModule.setInProgressLine(new Line(drawInProgressPos.getX(), drawInProgressPos.getZ(), mouseBlockPosX, mouseBlockPosZ), drawingMode);
                    }
                }
            }
            if (drawingRightClickDown) {
                drawingModule.removeHighlight(ChunkUtils.posToChunkPos(mouseBlockPosX), ChunkUtils.posToChunkPos(mouseBlockPosZ));
                drawingModule.removeLine(mouseBlockPosX, mouseBlockPosZ);
                drawingModule.removeEllipse(mouseBlockPosX, mouseBlockPosZ);
                drawingModule.removeText(mouseBlockPosX, mouseBlockPosZ, getFboScale());
            }
        }
    }

    @WrapOperation(method = "render", at = @At(
        value = "INVOKE",
        target = "Lcom/mojang/blaze3d/platform/GlStateManager;_clearColor(FFFF)V",
        ordinal = 2
    ))
    public void transparentBgSetTransparentClearColor(final float r, final float g, final float b, final float a, final Operation<Void> original) {
        if (Settings.REGISTRY.transparentWorldmapBackgroundSetting.get()) {
            original.call(r, g, b, 0.0f);
        } else {
            original.call(r, g, b, a);
        }
    }

    @WrapOperation(method = "render", at = @At(
        value = "INVOKE",
        target = "Lxaero/map/graphics/renderer/multitexture/MultiTextureRenderTypeRendererProvider;draw(Lxaero/map/graphics/renderer/multitexture/MultiTextureRenderTypeRenderer;)V",
        ordinal = 0
    ))
    public void transparentBgConfigMapRenderWithLight(final MultiTextureRenderTypeRendererProvider instance, final MultiTextureRenderTypeRenderer renderer, final Operation<Void> original) {
        if (Settings.REGISTRY.transparentWorldmapBackgroundSetting.get()) {
            BlendMode.lastApplied = LibShaders.WORLD_MAP.blend; // don't apply blend from shader.json
            ((CustomWorldMapShader) LibShaders.WORLD_MAP).setTransparentBackground(true);
            Globals.transparentWmBgApplyMapBlend = true;
            original.call(instance, renderer);
            ((CustomWorldMapShader) LibShaders.WORLD_MAP).setTransparentBackground(false);
            BlendMode.lastApplied = null;
        } else {
            ((CustomWorldMapShader) LibShaders.WORLD_MAP).setTransparentBackground(false);
            original.call(instance, renderer);
        }
    }

    @WrapOperation(method = "render", at = @At(
        value = "INVOKE",
        target = "Lxaero/map/graphics/renderer/multitexture/MultiTextureRenderTypeRendererProvider;draw(Lxaero/map/graphics/renderer/multitexture/MultiTextureRenderTypeRenderer;)V",
        ordinal = 1
    ))
    public void transparentBgConfigMapRenderNoLight(final MultiTextureRenderTypeRendererProvider instance, final MultiTextureRenderTypeRenderer renderer, final Operation<Void> original) {
        if (Settings.REGISTRY.transparentWorldmapBackgroundSetting.get()) {
            BlendMode.lastApplied = LibShaders.WORLD_MAP.blend; // don't apply blend from shader.json
            ((CustomWorldMapShader) LibShaders.WORLD_MAP).setTransparentBackground(true);
            Globals.transparentWmBgApplyMapBlend = true;
            original.call(instance, renderer);
            ((CustomWorldMapShader) LibShaders.WORLD_MAP).setTransparentBackground(false);
            BlendMode.lastApplied = null;
        } else {
            ((CustomWorldMapShader) LibShaders.WORLD_MAP).setTransparentBackground(false);
            original.call(instance, renderer);
        }
    }

    @WrapOperation(method = "render", at = @At(
        value = "INVOKE",
        target = "Lxaero/map/graphics/renderer/multitexture/MultiTextureRenderTypeRendererProvider;draw(Lxaero/map/graphics/renderer/multitexture/MultiTextureRenderTypeRenderer;)V",
        ordinal = 2
    ))
    public void transparentBgConfigMainFBORender(final MultiTextureRenderTypeRendererProvider instance, final MultiTextureRenderTypeRenderer renderer, final Operation<Void> original) {
        if (Settings.REGISTRY.transparentWorldmapBackgroundSetting.get()) {
            BlendMode.lastApplied = LibShaders.POSITION_COLOR_TEX.blend; // don't apply blend from shader.json
            Globals.transparentWmBgApplyGuiBilinearBlend = true;
            original.call(instance, renderer);
            BlendMode.lastApplied = null;
        } else {
            original.call(instance, renderer);
        }
    }

    @Inject(method = "shouldSkipWorldRender", at = @At("HEAD"), cancellable = true)
    public void transparentBgDisableWorldRenderSkip(final CallbackInfoReturnable<Boolean> cir) {
        if (Settings.REGISTRY.transparentWorldmapBackgroundSetting.get()) {
            cir.setReturnValue(false);
        }
    }


    // honestly no idea why xaero is doing here, its drawing 2 thin lines along the bottom and right side of the map
    // but it looks bad with transparent background, so bye

    @WrapWithCondition(method = "render", at = @At(
        value = "INVOKE",
        target = "Lxaero/map/graphics/MapRenderHelper;fillIntoExistingBuffer(Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIIIFFFF)V",
        ordinal = 0
    ),
        slice = @Slice(
            from = @At(
                value = "FIELD",
                target = "Lxaero/map/graphics/CustomRenderTypes;MAP_COLOR_FILLER:Lnet/minecraft/client/renderer/RenderType;",
                opcode = Opcodes.GETSTATIC
            )
        )
    )
    public boolean transparentBgCancelMapColorFiller0(final Matrix4f matrix, final VertexConsumer bufferBuilder, final int x1, final int y1, final int x2, final int y2, final float r, final float g, final float b, final float a) {
        return !Settings.REGISTRY.transparentWorldmapBackgroundSetting.get();
    }

    @WrapWithCondition(method = "render", at = @At(
        value = "INVOKE",
        target = "Lxaero/map/graphics/MapRenderHelper;fillIntoExistingBuffer(Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIIIFFFF)V",
        ordinal = 1
    ),
        slice = @Slice(
            from = @At(
                value = "FIELD",
                target = "Lxaero/map/graphics/CustomRenderTypes;MAP_COLOR_FILLER:Lnet/minecraft/client/renderer/RenderType;",
                opcode = Opcodes.GETSTATIC
            )
        )
    )
    public boolean transparentBgCancelMapColorFiller1(final Matrix4f matrix, final VertexConsumer bufferBuilder, final int x1, final int y1, final int x2, final int y2, final float r, final float g, final float b, final float a) {
        return !Settings.REGISTRY.transparentWorldmapBackgroundSetting.get();
    }

    @Inject(method = "render", at = @At(
        value = "FIELD",
        target = "Lxaero/map/common/config/option/WorldMapProfiledConfigOptions;COORDINATES:Lxaero/lib/common/config/option/BooleanConfigOption;",
        opcode = Opcodes.GETSTATIC,
        ordinal = 0
    ), remap = true)
    public void renderMeasurementToolText(
        final GuiGraphics guiGraphics,
        final int scaledMouseX,
        final int scaledMouseY,
        final float partialTicks,
        final CallbackInfo ci,
        @Local(name = "backgroundVertexBuffer") VertexConsumer backgroundVertexBuffer
    ) {
        if (!drawing) return;
        if (drawingMode != DrawingMode.MEASUREMENT) return;
        if (drawInProgressPos == null) return;
        var line = ModuleManager.getModule(Drawing.class).getInProgressLine();
        if (line == null) return;
        int len = Mth.floor(line.length());
        int dx = line.x2() - line.x1();
        int dz = line.z2() - line.z1();
        MapRenderHelper.drawCenteredStringWithBackground(guiGraphics, font, len + " blocks [" + dx + " x " + dz + "]", scaledMouseX, scaledMouseY - font.lineHeight, -1, 0.0f, 0.0f, 0.0f, 0.4f, backgroundVertexBuffer);
        var degreeStr = String.format("%.2f", line.angle());
        MapRenderHelper.drawCenteredStringWithBackground(guiGraphics, font, degreeStr + "°", scaledMouseX, scaledMouseY + font.lineHeight, -1, 0.0f, 0.0f, 0.0f, 0.4f, backgroundVertexBuffer);
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
    public void renderDrawingStatusText(
        final CallbackInfo ci,
        @Local(argsOnly = true) final GuiGraphics guiGraphics,
        @Local (name = "backgroundVertexBuffer") VertexConsumer backgroundVertexBuffer
    ) {
        if (!drawing) return;
        MapRenderHelper.drawCenteredStringWithBackground(
            guiGraphics, Minecraft.getInstance().font,
            "[XP] " + I18n.get("xaeroplus.gui.world_map.drawing_mode"),
            this.width / 2,
            24,
            -1,
            0.0F, 0.0F, 0.0F, 0.4F,
            backgroundVertexBuffer
        );
        var lines = I18n.get("xaeroplus.gui.world_map.drawing_mode_controls").split("\n");
        for (int i = 0; i < lines.length; i++) {
            MapRenderHelper.drawStringWithBackground(
                guiGraphics, Minecraft.getInstance().font,
                lines[i].trim(),
                40,
                this.height - 2 - (lines.length * (Minecraft.getInstance().font.lineHeight + 1)) + (i * (Minecraft.getInstance().font.lineHeight + 1)),
                -1,
                0.0F, 0.0F, 0.0F, 0.4F,
                backgroundVertexBuffer
            );
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true, remap = true)
    public void cancelClicksWhileDrawing(final double mouseX, final double mouseY, final int button, final CallbackInfoReturnable<Boolean> cir) {
        if (!drawing) return;
        boolean toReturn = super.mouseClicked(mouseX, mouseY, button);
        if (toReturn) {
            cir.setReturnValue(true);
            return;
        }
        if (colorPickerActive && (button == 0 || button == 1) && drawColorPicker.isMouseOver(mouseX, mouseY)) {
            cir.setReturnValue(true);
            return;
        }
        if (button == 0) { // start drawing on left click
            drawingLeftClickDown = true;
            switch (drawingMode) {
                case LINE_SEGMENT, INFINITE_LINE, ELLIPSE, TEXT, MEASUREMENT -> {
                    drawInProgressPos = new BlockPos(mouseBlockPosX, 0, mouseBlockPosZ);
                }
            }
            if (drawingMode == DrawingMode.TEXT) {
                if (drawTextEntryActive) {
                    if (drawTextEntryField.isMouseOver(mouseX, mouseY)) {
                        return;
                    }
                    removeWidget(drawTextEntryField);
                }
                drawTextEntryActive = true;
                drawTextEntryField.setX(Mth.clamp((int) mouseX - (drawTextEntryField.getWidth() / 2), 5, width - drawTextEntryField.getWidth() - 5));
                drawTextEntryField.setY(Mth.clamp((int) mouseY - (drawTextEntryField.getHeight() / 2), 5, height - drawTextEntryField.getHeight() - 5));
                addWidget(drawTextEntryField);
                drawTextEntryField.setVisible(true);
                drawTextEntryField.setCursorPosition(0);
                drawTextEntryField.setHint(Component.literal("Text:").withStyle(ChatFormatting.DARK_GRAY));
                setFocused(drawTextEntryField);
            }
            ModuleManager.getModule(Drawing.class).startOperation(Globals.getCurrentDimensionId(), false);
            cir.setReturnValue(true);
        } else if (button == 1) {
            drawingRightClickDown = true;
            ModuleManager.getModule(Drawing.class).startOperation(Globals.getCurrentDimensionId(), true);
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true, remap = true)
    public void drawingClickReleasedHandler(double mouseX, double mouseY, int button, final CallbackInfoReturnable<Boolean> cir) {
        if (!drawing) return;
        if (colorPickerActive && (button == 0 || button == 1) && drawColorPicker.isMouseOver(mouseX, mouseY)) {
            drawingLeftClickDown = false;
            drawingRightClickDown = false;
            drawInProgressPos = null;
        }
        boolean toReturn = super.mouseReleased(mouseX, mouseY, button);
        if (toReturn) {
            cir.setReturnValue(true);
            return;
        }
        if (button == 0) { // stop drawing on left click release
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
                        ModuleManager.getModule(Drawing.class).endOperation();
                    }
                }
                case MEASUREMENT -> {
                    drawInProgressPos = null;
                }
                case ELLIPSE -> {
                    if (drawInProgressPos != null) {
                        var drawingModule = ModuleManager.getModule(Drawing.class);
                        var ellipse = drawingModule.snapEllipse(
                            drawInProgressPos.getX(),
                            drawInProgressPos.getZ(),
                            mouseBlockPosX,
                            mouseBlockPosZ,
                            destScale
                        );
                        if (ellipse != null) drawingModule.addEllipse(ellipse);
                        drawInProgressPos = null;
                        drawingModule.removeInProgressEllipse();
                        drawingModule.endOperation();
                    }
                }
                case HIGHLIGHT -> {
                    ModuleManager.getModule(Drawing.class).endOperation();
                }
            }
            drawingLeftClickDown = false;
            cir.setReturnValue(true);
        } else if (button == 1) { // clear drawing on right click
            drawingRightClickDown = false;
            if (drawInProgressPos != null) return;
            ModuleManager.getModule(Drawing.class).removeLine(mouseBlockPosX, mouseBlockPosZ);
            ModuleManager.getModule(Drawing.class).removeEllipse(mouseBlockPosX, mouseBlockPosZ);
            ModuleManager.getModule(Drawing.class).removeText(mouseBlockPosX, mouseBlockPosZ, getFboScale());
            ModuleManager.getModule(Drawing.class).endOperation();
            cir.setReturnValue(true);
        }
    }

    @Unique
    private void xaeroPlus$stopDrawing() {
        drawing = false;
        drawInProgressPos = null;
        ModuleManager.getModule(Drawing.class).endOperation();
        ModuleManager.getModule(Drawing.class).removeInProgressLine();
        ModuleManager.getModule(Drawing.class).removeInProgressEllipse();
        drawingLeftClickDown = false;
        drawingRightClickDown = false;
        drawTextEntryActive = false;
        colorPickerActive = false;
        removeWidget(drawLineSegmentButton);
        removeWidget(drawInfiniteLineButton);
        removeWidget(drawHighlightsButton);
        removeWidget(drawEllipseButton);
        removeWidget(drawTextButton);
        removeWidget(drawColorPickerButton);
        removeWidget(drawColorPicker);
        removeWidget(drawTextEntryField);
        removeWidget(drawMeasurementToolButton);
        drawLineSegmentButton.visible = false;
        drawInfiniteLineButton.visible = false;
        drawHighlightsButton.visible = false;
        drawEllipseButton.visible = false;
        drawTextButton.visible = false;
        drawColorPickerButton.visible = false;
        drawColorPicker.visible = false;
        drawTextEntryField.visible = false;
        drawMeasurementToolButton.visible = false;
        this.init(Minecraft.getInstance(), width, height);
    }

    @Inject(method = "keyPressed", at = @At("RETURN"), remap = true)
    public void xaeroplus$drawingModeUndo(final int code, final int scanCode, final int modifiers, final CallbackInfoReturnable<Boolean> cir) {
        if (Screen.hasControlDown() && code == GLFW_KEY_Z) {
            ModuleManager.getModule(Drawing.class).undoLastOperation();
        }
    }

    @Inject(method = "onInputPress", at = @At("HEAD"))
    public void panMouseButtonClick(final InputConstants.Type type, final int code, final CallbackInfoReturnable<Boolean> cir) {
        if (type != InputConstants.Type.MOUSE) return;
        if (code != GLFW_MOUSE_BUTTON_MIDDLE) return;
        if (!Settings.REGISTRY.worldMapUIAdditions.get()) return;
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
                        onToggleDrawingButton(); // re-enable
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
    public void panMapOnRender(
        final CallbackInfo ci,
        @Local(argsOnly = true) final float partialTicks
    ) {
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
        target = "Lxaero/map/common/config/option/WorldMapProfiledConfigOptions;COORDINATES:Lxaero/lib/common/config/option/BooleanConfigOption;",
        opcode = Opcodes.GETSTATIC,
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
        if (!Settings.REGISTRY.worldMapUIAdditions.get()) return;
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
        }
        if (Settings.REGISTRY.worldMapRotateHereKeybindSetting.getKeyBinding().matches(code, scanCode)) {
            var mc = Minecraft.getInstance();
            mc.execute(() -> {
                if (mc.player == null) return;
                if (mc.player.isFallFlying()) {
                    PlayerRotationHelper.rotatePlayerTo(mouseBlockPosX, mouseBlockPosZ);
                } else {
                    PlayerRotationHelper.rotatePlayerTo(mouseBlockPosX, mouseBlockPosY, mouseBlockPosZ);
                }
            });
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "getRightClickOptions", at = @At(value = "RETURN"), remap = false)
    public void getRightClickOptionsInject(final CallbackInfoReturnable<ArrayList<RightClickOption>> cir) {
        if (!Settings.REGISTRY.worldMapUIAdditions.get()) return;
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
                    }.setNameFormatArgs(KeyMappingUtils.getKeyName(Settings.REGISTRY.worldMapBaritoneGoalHereKeybindSetting.getKeyBinding())));
            options.add(index++, new RightClickOption("xaeroplus.gui.world_map.baritone_path_here", options.size(), this) {
                        @Override
                        public void onAction(Screen screen) {
                            BaritoneExecutor.path(goalX, goalZ);
                        }
                    }.setNameFormatArgs(KeyMappingUtils.getKeyName(Settings.REGISTRY.worldMapBaritonePathHereKeybindSetting.getKeyBinding())));
            if (BaritoneHelper.isBaritoneElytraPresent()) {
                options.add(index++, new RightClickOption("xaeroplus.gui.world_map.baritone_elytra_here", options.size(), this) {
                        @Override
                        public void onAction(Screen screen) {
                            BaritoneExecutor.elytra(goalX, goalZ);
                        }
                    }.setNameFormatArgs(KeyMappingUtils.getKeyName(Settings.REGISTRY.worldMapBaritoneElytraHereKeybindSetting.getKeyBinding())));
            }
        }
        options.add(index++, new RightClickOption("xaeroplus.gui.world_map.rotate_here", options.size(), this) {
            @Override
            public void onAction(Screen screen) {
                var mc = Minecraft.getInstance();
                mc.execute(() -> {
                    if (mc.player == null) return;
                    if (mc.player.isFallFlying()) {
                        PlayerRotationHelper.rotatePlayerTo(rightClickX, rightClickZ);
                    } else {
                        PlayerRotationHelper.rotatePlayerTo(rightClickX, rightClickY, rightClickZ);
                    }
                });
            }
        }.setNameFormatArgs(KeyMappingUtils.getKeyName(Settings.REGISTRY.worldMapRotateHereKeybindSetting.getKeyBinding())));
        boolean tileSelPresent = this.mapTileSelection != null;
        final int delHighlightMinX = tileSelPresent ? mapTileSelection.getLeft() : rightClickX;
        final int delHighlightMaxX = tileSelPresent ? mapTileSelection.getRight() : rightClickX;
        final int delHighlightMinZ = tileSelPresent ? mapTileSelection.getTop() : rightClickZ;
        final int delHighlightMaxZ = tileSelPresent ? mapTileSelection.getBottom() : rightClickZ;
        options.add(index++, new RightClickOption("xaeroplus.gui.world_map.delete_highlights", options.size(), this) {
            @Override
            public void onAction(final Screen screen) {
                var dim = Globals.getCurrentDimensionId();
                LongList toRemove = new LongArrayList((delHighlightMaxX - delHighlightMinX + 1) * (delHighlightMaxZ - delHighlightMinZ + 1));
                for (int x = delHighlightMinX; x <= delHighlightMaxX; x++) {
                    for (int z = delHighlightMinZ; z <= delHighlightMaxZ; z++) {
                        toRemove.add(ChunkUtils.chunkPosToLong(x, z));
                        ModuleManager.getModule(Drawing.class).removeLine(ChunkUtils.chunkCoordToCoord(x), ChunkUtils.chunkCoordToCoord(z));
                        ModuleManager.getModule(Drawing.class).removeEllipse(ChunkUtils.chunkCoordToCoord(x), ChunkUtils.chunkCoordToCoord(z));
                        ModuleManager.getModule(Drawing.class).removeText(ChunkUtils.chunkCoordToCoord(x), ChunkUtils.chunkCoordToCoord(z), 1);
                    }
                }
                ModuleManager.getModule(Drawing.class).removeHighlights(toRemove);
                var breadcrumbs = ModuleManager.getModule(Breadcrumbs.class);
                if (breadcrumbs.isEnabled()) {
                    breadcrumbs.breadcrumbsCache.get().removeHighlights(toRemove, dim);
                }
                var liquidNewChunks = ModuleManager.getModule(LiquidNewChunks.class);
                if (liquidNewChunks.isEnabled()) {
                    liquidNewChunks.newChunksCache.get().removeHighlights(toRemove, dim);
                    liquidNewChunks.inverseNewChunksCache.get().removeHighlights(toRemove, dim);
                }
                var oldbiomes = ModuleManager.getModule(OldBiomes.class);
                if (oldbiomes.isEnabled()) {
                    oldbiomes.oldBiomesCache.get().removeHighlights(toRemove, dim);
                }
                var oldChunks = ModuleManager.getModule(OldChunks.class);
                if (oldChunks.isEnabled()) {
                    oldChunks.oldChunksCache.get().removeHighlights(toRemove, dim);
                    oldChunks.modernChunksCache.get().removeHighlights(toRemove, dim);
                }
                var paletteNewChunks = ModuleManager.getModule(PaletteNewChunks.class);
                if (paletteNewChunks.isEnabled()) {
                    paletteNewChunks.newChunksCache.get().removeHighlights(toRemove, dim);
                    paletteNewChunks.newChunksInverseCache.get().removeHighlights(toRemove, dim);
                }
                var portals = ModuleManager.getModule(Portals.class);
                if (portals.isEnabled()) {
                    portals.portalsCache.get().removeHighlights(toRemove, dim);
                }
                var lavaColumns = ModuleManager.getModule(LavaColumns.class);
                if (lavaColumns.isEnabled()) {
                    lavaColumns.lavaColumnsCache.get().removeHighlights(toRemove, dim);
                }
            }
        });

        if (Settings.REGISTRY.disableWaypointSharing.get()) {
            options.removeIf(option -> ((AccessorRightClickOption) option).invokeGetName().equals("gui.xaero_right_click_map_share_location"));
        }

        if (Settings.REGISTRY.disableTeleportation.get()) {
            options.removeIf(option -> ((AccessorRightClickOption) option).invokeGetName().equals("gui.xaero_wm_right_click_map_teleport_not_allowed"));
        }
    }

    @Unique
    private void onSwitchDimensionButton(final ResourceKey<Level> newDimId) {
        Globals.switchToDimension(newDimId);
    }
}
