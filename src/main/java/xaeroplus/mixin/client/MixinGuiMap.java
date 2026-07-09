package xaeroplus.mixin.client;

import baritone.api.BaritoneAPI;
import baritone.api.pathing.goals.GoalXZ;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
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
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.common.HudMod;
import xaero.common.misc.OptimizedMath;
import xaero.hud.minimap.common.config.option.MinimapProfiledConfigOptions;
import xaero.lib.client.config.ClientConfigManager;
import xaero.lib.client.gui.ScreenBase;
import xaero.lib.client.gui.widget.Tooltip;
import xaero.lib.common.config.option.ConfigOption;
import xaero.map.WorldMap;
import xaero.map.animation.SlowingAnimation;
import xaero.map.config.util.WorldMapClientConfigUtils;
import xaero.map.gui.GuiMap;
import xaero.map.gui.GuiTexturedButton;
import xaero.map.gui.IRightClickableElement;
import xaero.map.gui.dropdown.rightclick.RightClickOption;
import xaeroplus.XaeroPlus;
import xaeroplus.module.ModuleManager;
import xaeroplus.module.impl.NewChunks;
import xaeroplus.module.impl.PortalSkipDetection;
import xaeroplus.module.impl.Portals;
import xaeroplus.settings.Settings;
import xaeroplus.util.*;

import java.util.ArrayList;
import java.util.function.Consumer;

import static java.util.Arrays.asList;
import static org.lwjgl.opengl.GL11.GL_LINE_LOOP;
import static xaero.map.gui.GuiMap.setupTextureMatricesAndTextures;
import static xaeroplus.util.Globals.getCurrentDimensionId;

@Mixin(value = GuiMap.class, remap = false)
public abstract class MixinGuiMap extends ScreenBase implements IRightClickableElement {
    @Unique
    GuiButton switchToNetherButton;
    @Unique
    GuiButton switchToOverworldButton;
    @Unique
    GuiButton switchToEndButton;

    protected MixinGuiMap(GuiScreen parent, GuiScreen escape, ITextComponent title) {
        super(parent, escape, title);
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
    protected abstract void closeDropdowns();
    @Shadow
    public abstract void addGuiButton(GuiButton b);
    @Shadow
    private int rightClickX;
    @Shadow
    private int rightClickZ;
    @Shadow
    public static boolean hiddenUI;

    @Inject(method = "initGui()V", at = @At(value = "TAIL"), remap = true)
    public void customInitGui(CallbackInfo ci) {
        // right side
        this.switchToEndButton = new GuiTexturedButton(
            this.width - 20, zoomInButton.y - 20, 20, 20, 31, 0, 16, 16, Globals.xpGuiTextures, new Consumer<GuiButton>() {
            @Override
            public void accept(final GuiButton button) {
                onSwitchDimensionButton(1);
            }
        }, () -> new Tooltip(new TextComponentTranslation("setting.keybinds.switch_to_end")));
        this.switchToOverworldButton = new GuiTexturedButton(
            this.width - 20, this.switchToEndButton.y - 20, 20, 20, 16, 0, 16, 16, Globals.xpGuiTextures, new Consumer<GuiButton>() {
            @Override
            public void accept(final GuiButton button) {
                onSwitchDimensionButton(0);
            }
        }, () -> new Tooltip(new TextComponentTranslation("setting.keybinds.switch_to_overworld")));
        this.switchToNetherButton = new GuiTexturedButton(
            this.width - 20, this.switchToOverworldButton.y - 20, 20, 20, 0, 0, 16, 16, Globals.xpGuiTextures, new Consumer<GuiButton>() {
            @Override
            public void accept(final GuiButton button) {
                onSwitchDimensionButton(-1);
            }
        }, () -> new Tooltip(new TextComponentTranslation("setting.keybinds.switch_to_nether")));
        addGuiButton(switchToNetherButton);
        addGuiButton(switchToOverworldButton);
        addGuiButton(switchToEndButton);
    }

    @Inject(method = "onGuiClosed", at = @At(value = "RETURN"), remap = true)
    public void onGuiClosed(final CallbackInfo ci) {
        if (!Settings.REGISTRY.persistMapDimensionSwitchSetting.getValue()) {
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

    private static void trySettingCurrentProfileOption(ConfigOption<Boolean> option, boolean value) {
        ClientConfigManager configManager = WorldMap.INSTANCE.getConfigs().getClientConfigManager();
        Boolean currentValue = configManager.getEffective(option);
        if (currentValue != value) {
            WorldMapClientConfigUtils.tryTogglingCurrentProfileOption(option);
        }
    }

    @Inject(method = "drawScreen",
        at = @At(
            value = "INVOKE",
            target = "Lxaero/map/region/LeveledRegion;loadingAnimation()Z",
            ordinal = 0
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
        if (Settings.REGISTRY.newChunksEnabledSetting.getValue() && !hiddenUI) {
            final NewChunks newChunks = ModuleManager.getModule(NewChunks.class);
            GuiHelper.drawHighlightAtChunkPosList(newChunks.getNewChunksInRegion(leafRegionMinX, leafRegionMinZ, leveledSideInRegions, getCurrentDimensionId()),
                                                  flooredCameraX,
                                                  flooredCameraZ,
                                                  newChunks.getNewChunksColor());
        }
        if (Settings.REGISTRY.portalSkipDetectionEnabledSetting.getValue() && !hiddenUI && Settings.REGISTRY.newChunksEnabledSetting.getValue()) {
            final PortalSkipDetection portalSkipDetection = ModuleManager.getModule(PortalSkipDetection.class);
            GuiHelper.drawHighlightAtChunkPosList(portalSkipDetection.getPortalSkipChunksInRegion(leafRegionMinX, leafRegionMinZ, leveledSideInRegions),
                                                  flooredCameraX,
                                                  flooredCameraZ,
                                                  portalSkipDetection.getPortalSkipChunksColor());
        }
        if (Settings.REGISTRY.portalsEnabledSetting.getValue() && !hiddenUI) {
            final Portals portals = ModuleManager.getModule(Portals.class);
            GuiHelper.drawHighlightAtChunkPosList(portals.getPortalsInRegion(leafRegionMinX, leafRegionMinZ, leveledSideInRegions, getCurrentDimensionId()),
                                                  flooredCameraX,
                                                  flooredCameraZ,
                                                  portals.getPortalsColor());
        }
        final boolean isDimensionSwitched = Globals.getCurrentDimensionId() != mc.player.dimension;
        if (Settings.REGISTRY.wdlEnabledSetting.getValue()
            && !hiddenUI
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
        target = "Lxaero/map/common/config/option/WorldMapProfiledConfigOptions;ARROW:Lxaero/lib/common/config/option/BooleanConfigOption;",
        opcode = Opcodes.GETSTATIC,
        ordinal = 0
    ), remap = true)
    public void showRenderDistanceWorldMap(int scaledMouseX, int scaledMouseY, float partialTicks, CallbackInfo ci,
                                           @Local(name = "scaledPlayerX") double scaledPlayerX,
                                           @Local(name = "scaledPlayerZ") double scaledPlayerZ,
                                           @Local(name = "flooredCameraX") int flooredCameraX,
                                           @Local(name = "flooredCameraZ") int flooredCameraZ
    ){
        final boolean isDimensionSwitched = Globals.getCurrentDimensionId() != mc.player.dimension;
        if (Settings.REGISTRY.showRenderDistanceWorldMapSetting.getValue() && !hiddenUI && !isDimensionSwitched) {
            final int setting = (int) Settings.REGISTRY.assumedServerRenderDistanceSetting.getValue();
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
            float settingWidth = (float) HudMod.INSTANCE.getHudConfigs().getClientConfigManager().getEffective(
                MinimapProfiledConfigOptions.CHUNK_GRID_LINE_WIDTH);
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

    private void onSwitchDimensionButton(final int newDimId) {
        Globals.switchToDimension(newDimId);
    }
}
