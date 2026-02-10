package xaeroplus.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.shaders.BlendMode;
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
import xaero.lib.common.config.single.SingleConfigManager;
import xaero.map.MapProcessor;
import xaero.map.WorldMap;
import xaero.map.animation.SlowingAnimation;
import xaero.map.common.config.option.WorldMapProfiledConfigOptions;
import xaero.map.config.util.WorldMapClientConfigUtils;
import xaero.map.element.HoveredMapElementHolder;
import xaero.map.element.MapElementRenderHandler;
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
import xaeroplus.feature.drawing.DrawingColorCyclerButton;
import xaeroplus.feature.extensions.CustomWorldMapShader;
import xaeroplus.feature.render.line.Line;
import xaeroplus.feature.render.text.Text;
import xaeroplus.module.ModuleManager;
import xaeroplus.module.impl.*;
import xaeroplus.settings.Settings;
import xaeroplus.util.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.regex.Pattern;
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
    @Unique Button drawMeasurementToolButton;
    @Unique Button exitButton;
    @Unique boolean drawing = false;
    @Unique BlockPos drawInProgressPos = null;
    @Unique boolean drawingLeftClickDown = false;
    @Unique boolean drawingRightClickDown = false;
    @Unique boolean drawTextEntryActive = false;
    @Unique DrawingMode drawingMode = DrawingMode.LINE_SEGMENT;
    @Unique EditBox drawTextEntryField;
    @Unique List<Button> guiMapButtonTempList = new ArrayList<>();
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
            .append(Component.literal(KeyMappingUtils.getKeyName(bind) + " ").withStyle(ChatFormatting.DARK_GREEN))
            .append(component);
    }

    @Inject(method = "init", at = @At(value = "RETURN"), remap = true)
    public void customInitGui(CallbackInfo ci) {
        // left side
        followButton = new GuiTexturedButton(
            0, this.dimensionToggleButton.getY() - 20, 20, 20, this.follow ? 61 : 42, 19, 16, 16,
            Globals.guiTextures,
            this::onFollowButton,
            () -> new Tooltip(xaeroPlus$prefix(Component.translatable("xaeroplus.gui.world_map.toggle_follow_mode")
                .append(" " + I18n.get(this.follow ? "xaeroplus.gui.off" : "xaeroplus.gui.on")))));
        coordinateGotoButton = new GuiTexturedButton(
            0, followButton.getY() - 20 , 20, 20, 23, 19, 16, 16,
            Globals.guiTextures,
            this::onGotoCoordinatesButton,
            () -> new Tooltip(xaeroPlus$prefix(Component.translatable("xaeroplus.gui.world_map.go_to_coordinates"))));
        xTextEntryField = new EditBox(Minecraft.getInstance().font, 20, coordinateGotoButton.getY() - 10, 75, 20, Component.nullToEmpty("X:"));
        xTextEntryField.setVisible(false);
        xTextEntryField.setCursorPosition(0);
        xTextEntryField.setHint(Component.literal("X:").withStyle(ChatFormatting.DARK_GRAY));
        zTextEntryField = new EditBox(Minecraft.getInstance().font, 20, xTextEntryField.getY() + 20, 75, 20, Component.nullToEmpty("Z:"));
        zTextEntryField.setVisible(false);
        zTextEntryField.setCursorPosition(0);
        zTextEntryField.setHint(Component.literal("Z:").withStyle(ChatFormatting.DARK_GRAY));
        startDrawingButton = new GuiTexturedButton(
            0, this.coordinateGotoButton.getY() - 20, 20, 20, 47, 0, 16, 16,
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
        drawTextButton = new GuiTexturedButton(
            startDrawingButton.getX() + 16, drawHighlightsButton.getY() + 20, 20, 20, 118, 0, 16, 16,
            Globals.guiTextures,
            button -> setDrawingMode(DrawingMode.TEXT),
            () -> new Tooltip(xaeroPlus$prefix(Component.translatable("xaeroplus.gui.world_map.draw_text"))));
        drawTextButton.visible = false;
        drawColorCyclerButton = new DrawingColorCyclerButton(
            startDrawingButton.getX() + 16, drawTextButton.getY() + 20,
            () -> new Tooltip(xaeroPlus$prefix(Component.translatable("xaeroplus.gui.world_map.draw_color"))),
            ModuleManager.getModule(Drawing.class).getDrawingColorCycler());
        drawColorCyclerButton.visible = false;
        drawMeasurementToolButton = new GuiTexturedButton(
            startDrawingButton.getX() + 16, drawColorCyclerButton.getY() + 20, 20, 20, 135, 0, 16, 16,
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
        addButton(followButton);
        addButton(coordinateGotoButton);
        addWidget(xTextEntryField);
        addWidget(zTextEntryField);
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
            addButton(drawMeasurementToolButton);
            drawLineSegmentButton.visible = true;
            drawInfiniteLineButton.visible = true;
            drawHighlightsButton.visible = true;
            drawTextButton.visible = true;
            drawColorCyclerButton.visible = true;
            drawMeasurementToolButton.visible = true;
        } else {
            xaeroPlus$stopDrawing();
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

    @Inject(method = "render", at = @At(
        value = "INVOKE",
        target = "Lxaero/map/gui/GuiMap;init(Lnet/minecraft/client/Minecraft;II)V",
        ordinal = 0,
        shift = At.Shift.AFTER
    ), remap = true)
    public void toggleRadarWhileDimensionSwitched(final CallbackInfo ci, @Local(name = "currentFutureDim") MapDimension currentFutureDim) {
        if (!Settings.REGISTRY.radarWhileDimensionSwitchedSetting.get()) {
            trySettingCurrentProfileOption(WorldMapProfiledConfigOptions.MINIMAP_RADAR, currentFutureDim.getDimId() == ChunkUtils.getActualDimension());
        }
    }

    private static void trySettingCurrentProfileOption(ConfigOption<Boolean> option, boolean value) {
        ClientConfigManager configManager = WorldMap.INSTANCE.getConfigs().getClientConfigManager();
        var currentValue = configManager.getEffective(option);
        if (currentValue != value) {
            WorldMapClientConfigUtils.tryTogglingCurrentProfileOption(option);
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
            this.cameraDestinationAnimX = new SlowingAnimation(this.cameraX, getPlayerX(), 0.15, 0.5);
            this.cameraDestinationAnimZ = new SlowingAnimation(this.cameraZ, getPlayerZ(), 0.15, 0.5);
        }
    }

    @WrapOperation(method = "render", at = @At(
        value = "INVOKE",
        target = "Lxaero/lib/common/config/single/SingleConfigManager;getEffective(Lxaero/lib/common/config/option/ConfigOption;)Ljava/lang/Object;",
        ordinal = 0
    ),
        slice = @Slice(
            from = @At(
                value = "FIELD",
                target = "Lxaero/map/config/primary/option/WorldMapPrimaryClientConfigOptions;DEBUG:Lxaero/lib/common/config/option/BooleanConfigOption;",
                opcode = Opcodes.GETSTATIC)
        )
    )
    public Object hideDebugRenderingOnF1(final SingleConfigManager instance, final ConfigOption option, final Operation original) {
        var value = original.call(instance, option);
        return ((Boolean) value) && !Minecraft.getInstance().options.hideGui;
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
        target = "Lxaero/map/element/MapElementRenderHandler;render(Lxaero/map/gui/GuiMap;Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lxaero/map/graphics/renderer/multitexture/MultiTextureRenderTypeRendererProvider;DDIIDDDDDFZLxaero/map/element/HoveredMapElementHolder;Lnet/minecraft/client/Minecraft;F)Lxaero/map/element/HoveredMapElementHolder;"
    ), remap = true)
    public HoveredMapElementHolder<?, ?> hideMapElementsOnF1(MapElementRenderHandler handler, GuiMap mapScreen, GuiGraphics guiGraphics, MultiBufferSource.BufferSource renderTypeBuffers, MultiTextureRenderTypeRendererProvider rendererProvider, double cameraX, double cameraZ, int width, int height, double screenSizeBasedScale, double scale, double playerDimDiv, double mouseX, double mouseZ, float brightness, boolean cave, HoveredMapElementHolder<?, ?> oldHovered, Minecraft mc, float partialTicks, Operation<HoveredMapElementHolder<?, ?>> original) {
        if (!Minecraft.getInstance().options.hideGui) {
            return original.call(handler, mapScreen, guiGraphics, renderTypeBuffers, rendererProvider, cameraX, cameraZ, width, height, screenSizeBasedScale, scale, playerDimDiv, mouseX, mouseZ, brightness, cave, oldHovered, mc, partialTicks);
        } else {
            return null;
        }
    }

    @WrapOperation(method = "render", at = @At(
        value = "INVOKE",
        target = "Lxaero/lib/client/config/ClientConfigManager;getEffective(Lxaero/lib/common/config/option/ConfigOption;)Ljava/lang/Object;",
        ordinal = 0
    ),
        slice = @Slice(
            from = @At(
                value = "FIELD",
                target = "Lxaero/map/common/config/option/WorldMapProfiledConfigOptions;FOOTSTEPS:Lxaero/lib/common/config/option/BooleanConfigOption;",
                opcode = Opcodes.GETSTATIC)
        )
    )
    public Object hideFootstepsOnF1(final ClientConfigManager instance, final ConfigOption option, final Operation original) {
        var value = original.call(instance, option);
        return ((Boolean) value) && !Minecraft.getInstance().options.hideGui;
    }

    @WrapOperation(method = "render", at = @At(
        value = "INVOKE",
        target = "Lxaero/lib/client/config/ClientConfigManager;getEffective(Lxaero/lib/common/config/option/ConfigOption;)Ljava/lang/Object;",
        ordinal = 0
    ),
        slice = @Slice(
            from = @At(
                value = "FIELD",
                target = "Lxaero/map/common/config/option/WorldMapProfiledConfigOptions;ARROW:Lxaero/lib/common/config/option/BooleanConfigOption;",
                opcode = Opcodes.GETSTATIC)
        )
    )
    public Object hideArrowOnF1(final ClientConfigManager instance, final ConfigOption option, final Operation original) {
        var value = original.call(instance, option);
        return ((Boolean) value) && !Minecraft.getInstance().options.hideGui;
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
        if (!Settings.REGISTRY.worldMapUIAdditions.get()) return;
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
        target = "Lxaero/map/graphics/MapRenderHelper;drawCenteredStringWithBackground(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIFFFFLcom/mojang/blaze3d/vertex/VertexConsumer;)V"
    ), remap = true)
    public boolean hideMoreRenderedStringsOnF1(final GuiGraphics guiGraphics, final Font font, final Component text, final int x, final int y, final int color, final float bgRed, final float bgGreen, final float bgBlue, final float bgAlpha, final VertexConsumer backgroundVertexBuffer) {
        return !Minecraft.getInstance().options.hideGui;
    }

    @Inject(method = "onDimensionToggleButton", at = @At(value = "RETURN"))
    public void onDimensionToggleAfter(final Button b, final CallbackInfo ci) {
        if (!Settings.REGISTRY.radarWhileDimensionSwitchedSetting.get()) {
            trySettingCurrentProfileOption(WorldMapProfiledConfigOptions.MINIMAP_RADAR, mapProcessor.getMapWorld().getFutureDimensionId() == ChunkUtils.getActualDimension());
        }
    }

    @Inject(method = "tick", at = @At("RETURN"), remap = true)
    public void onTick(final CallbackInfo ci) {
        if (!Settings.REGISTRY.worldMapUIAdditions.get()) return;
        xTextEntryField.tick();
        zTextEntryField.tick();
        drawTextEntryField.tick();
        if (!drawing) return;
        switch (drawingMode) {
            case LINE_SEGMENT -> {
                startDrawingButton.setFocused(false);
                drawLineSegmentButton.setFocused(true);
                drawInfiniteLineButton.setFocused(false);
                drawHighlightsButton.setFocused(false);
                drawTextButton.setFocused(false);
                drawColorCyclerButton.setFocused(false);
                drawMeasurementToolButton.setFocused(false);
            }
            case INFINITE_LINE -> {
                startDrawingButton.setFocused(false);
                drawLineSegmentButton.setFocused(false);
                drawInfiniteLineButton.setFocused(true);
                drawHighlightsButton.setFocused(false);
                drawTextButton.setFocused(false);
                drawColorCyclerButton.setFocused(false);
                drawMeasurementToolButton.setFocused(false);
            } case HIGHLIGHT -> {
                startDrawingButton.setFocused(false);
                drawLineSegmentButton.setFocused(false);
                drawInfiniteLineButton.setFocused(false);
                drawHighlightsButton.setFocused(true);
                drawTextButton.setFocused(false);
                drawColorCyclerButton.setFocused(false);
                drawMeasurementToolButton.setFocused(false);
            } case TEXT -> {
                startDrawingButton.setFocused(false);
                drawLineSegmentButton.setFocused(false);
                drawInfiniteLineButton.setFocused(false);
                drawHighlightsButton.setFocused(false);
                drawTextButton.setFocused(true);
                drawColorCyclerButton.setFocused(false);
                drawMeasurementToolButton.setFocused(false);
                if (drawTextEntryActive) {
                    drawTextEntryField.setEditable(true);
                    drawTextEntryField.setFocused(true);
                    setFocused(drawTextEntryField);
                }
            } case MEASUREMENT -> {
                startDrawingButton.setFocused(false);
                drawLineSegmentButton.setFocused(false);
                drawInfiniteLineButton.setFocused(false);
                drawHighlightsButton.setFocused(false);
                drawTextButton.setFocused(false);
                drawColorCyclerButton.setFocused(false);
                drawMeasurementToolButton.setFocused(true);
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
                } case MEASUREMENT -> {
                    if (drawInProgressPos == null) {
                        ModuleManager.getModule(Drawing.class).removeInProgressLine();
                    } else {
                        ModuleManager.getModule(Drawing.class).setInProgressLine(new Line(drawInProgressPos.getX(), drawInProgressPos.getZ(), mouseBlockPosX, mouseBlockPosZ), drawingMode);
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
        if (button == 0) { // start drawing on left click
            drawingLeftClickDown = true;
            switch (drawingMode) {
                case LINE_SEGMENT, INFINITE_LINE, TEXT, MEASUREMENT -> {
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
    public void drawingClickReleasedHandler(final double par1, final double par2, final int par3, final CallbackInfoReturnable<Boolean> cir) {
        if (!drawing) return;
        boolean toReturn = super.mouseReleased(par1, par2, par3);
        if (toReturn) {
            cir.setReturnValue(true);
            return;
        }
        if (par3 == 0) { // stop drawing on left click release
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
                case HIGHLIGHT -> {
                    ModuleManager.getModule(Drawing.class).endOperation();
                }
            }
            drawingLeftClickDown = false;
            cir.setReturnValue(true);
        } else if (par3 == 1) { // clear drawing on right click
            drawingRightClickDown = false;
            if (drawInProgressPos != null) return;
            ModuleManager.getModule(Drawing.class).removeLine(mouseBlockPosX, mouseBlockPosZ);
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
        drawingLeftClickDown = false;
        drawingRightClickDown = false;
        drawTextEntryActive = false;
        removeWidget(drawLineSegmentButton);
        removeWidget(drawInfiniteLineButton);
        removeWidget(drawHighlightsButton);
        removeWidget(drawTextButton);
        removeWidget(drawColorCyclerButton);
        removeWidget(drawTextEntryField);
        removeWidget(drawMeasurementToolButton);
        drawLineSegmentButton.visible = false;
        drawInfiniteLineButton.visible = false;
        drawHighlightsButton.visible = false;
        drawTextButton.visible = false;
        drawColorCyclerButton.visible = false;
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
            } else if (code == GLFW_KEY_TAB) {
                if (xTextEntryField.isFocused()) {
                    setFocused(zTextEntryField);
                } else if (zTextEntryField.isFocused()) {
                    setFocused(xTextEntryField);
                }
                cir.setReturnValue(true);
                return;
            } else if (Screen.isPaste(code) && xTextEntryField.isFocused()) {
                var pasteText = Minecraft.getInstance().keyboardHandler.getClipboard().trim();
                var xyzSpaces = Pattern.compile("(-?\\d+)\\s(-?\\d+)\\s(-?\\d+)").matcher(pasteText);
                if (xyzSpaces.matches()) {
                    String xText = xyzSpaces.group(1);
                    String zText = xyzSpaces.group(3);
                    xTextEntryField.setValue(xText);
                    zTextEntryField.setValue(zText);
                    cir.setReturnValue(true);
                    return;
                }
                var xyzCommaSpaces = Pattern.compile("(-?\\d+),\\s(-?\\d+),\\s(-?\\d+)").matcher(pasteText);
                if (xyzCommaSpaces.matches()) {
                    String xText = xyzCommaSpaces.group(1);
                    String zText = xyzCommaSpaces.group(3);
                    xTextEntryField.setValue(xText);
                    zTextEntryField.setValue(zText);
                    cir.setReturnValue(true);
                    return;
                }
                var xzSpaces = Pattern.compile("(-?\\d+)\\s(-?\\d+)").matcher(pasteText);
                if (xzSpaces.matches()) {
                    String xText = xzSpaces.group(1);
                    String zText = xzSpaces.group(2);
                    xTextEntryField.setValue(xText);
                    zTextEntryField.setValue(zText);
                    cir.setReturnValue(true);
                    return;
                }
                var xzCommaSpaces = Pattern.compile("(-?\\d+),\\s(-?\\d+)").matcher(pasteText);
                if (xzCommaSpaces.matches()) {
                    String xText = xzCommaSpaces.group(1);
                    String zText = xzCommaSpaces.group(2);
                    xTextEntryField.setValue(xText);
                    zTextEntryField.setValue(zText);
                    cir.setReturnValue(true);
                    return;
                }
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
        boolean tileSelPresent = this.mapTileSelection != null;
        final int delHighlightMinX = tileSelPresent ? mapTileSelection.getLeft() : rightClickX;
        final int delHighlightMaxX = tileSelPresent ? mapTileSelection.getRight() : rightClickX;
        final int delHighlightMinZ = tileSelPresent ? mapTileSelection.getTop() : rightClickZ;
        final int delHighlightMaxZ = tileSelPresent ? mapTileSelection.getBottom() : rightClickZ;
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

        if (Settings.REGISTRY.disableTeleportation.get()) {
            options.removeIf(option -> ((AccessorRightClickOption) option).getName().equals("gui.xaero_wm_right_click_map_teleport_not_allowed"));
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
            // on current tick, after this method, mouseClicked event is fired, triggering focus onto goto coords button
            // so we schedule text box focus on following tick
            ForkJoinPool.commonPool().execute(() -> {
                Minecraft.getInstance().execute(() -> {
                    xTextEntryField.setVisible(true);
                    zTextEntryField.setVisible(true);
                    setFocused(xTextEntryField);
                });
            });
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
