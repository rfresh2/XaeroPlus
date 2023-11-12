package xaeroplus.mixin.client;

import baritone.api.BaritoneAPI;
import baritone.api.pathing.goals.GoalXZ;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.render.*;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.common.XaeroMinimapSession;
import xaero.common.graphics.shader.MinimapShaders;
import xaero.map.MapProcessor;
import xaero.map.WorldMap;
import xaero.map.animation.Animation;
import xaero.map.animation.SinAnimation;
import xaero.map.animation.SlowingAnimation;
import xaero.map.controls.ControlsHandler;
import xaero.map.controls.ControlsRegister;
import xaero.map.effects.Effects;
import xaero.map.element.HoveredMapElementHolder;
import xaero.map.graphics.CustomRenderTypes;
import xaero.map.graphics.ImprovedFramebuffer;
import xaero.map.graphics.MapRenderHelper;
import xaero.map.graphics.renderer.multitexture.MultiTextureRenderTypeRenderer;
import xaero.map.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider;
import xaero.map.graphics.shader.MapShaders;
import xaero.map.gui.*;
import xaero.map.gui.dropdown.rightclick.GuiRightClickMenu;
import xaero.map.gui.dropdown.rightclick.RightClickOption;
import xaero.map.misc.Misc;
import xaero.map.misc.OptimizedMath;
import xaero.map.mods.SupportMods;
import xaero.map.mods.gui.Waypoint;
import xaero.map.radar.tracker.PlayerTrackerMapElement;
import xaero.map.region.*;
import xaero.map.region.texture.RegionTexture;
import xaero.map.settings.ModSettings;
import xaero.map.world.MapDimension;
import xaeroplus.Globals;
import xaeroplus.feature.extensions.CustomDimensionMapProcessor;
import xaeroplus.mixin.client.mc.AccessorGameOptions;
import xaeroplus.settings.XaeroPlusSettingRegistry;
import xaeroplus.util.BaritoneHelper;
import xaeroplus.util.ChunkUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.Objects.isNull;
import static net.minecraft.world.World.*;
import static xaero.map.gui.GuiMap.*;
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
    @Final
    @Shadow
    private static Text FULL_RELOAD_IN_PROGRESS = Text.translatable("gui.xaero_full_reload_in_progress");
    @Final
    @Shadow
    private static final double ZOOM_STEP = 1.2;
    @Final
    @Shadow
    private static final int white = -1;
    @Final
    @Shadow
    private static final int black = -16777216;
    @Final
    @Shadow
    private static final Matrix4f identityMatrix = new Matrix4f();
    @Shadow
    private static int lastAmountOfRegionsViewed = 1;
    @Shadow
    private static double destScale = 3.0;
    @Shadow
    private static ImprovedFramebuffer primaryScaleFBO = null;
    @Shadow
    public boolean waypointMenu = false;
    @Shadow
    public boolean playersMenu = false;
    @Shadow
    public boolean noUploadingLimits;
    @Shadow
    private long loadingAnimationStart;
    @Shadow
    private Entity player;
    @Shadow
    private double screenScale = 0.0;
    @Shadow
    private int mouseDownPosX = -1;
    @Shadow
    private int mouseDownPosY = -1;
    @Shadow
    private double mouseDownCameraX = -1.0;
    @Shadow
    private double mouseDownCameraZ = -1.0;
    @Shadow
    private int mouseCheckPosX = -1;
    @Shadow
    private int mouseCheckPosY = -1;
    @Shadow
    private long mouseCheckTimeNano = -1L;
    @Shadow
    private int prevMouseCheckPosX = -1;
    @Shadow
    private int prevMouseCheckPosY = -1;
    @Shadow
    private long prevMouseCheckTimeNano = -1L;
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
    private double userScale;
    @Shadow
    private boolean pauseZoomKeys;
    @Shadow
    private int lastZoomMethod;
    @Shadow
    private HoveredMapElementHolder<?, ?> viewed = null;
    @Shadow
    private boolean viewedInList;
    @Shadow
    private HoveredMapElementHolder<?, ?> viewedOnMousePress = null;
    @Shadow
    private boolean overWaypointsMenu;
    @Shadow
    private Animation zoomAnim;
    @Shadow
    private boolean overPlayersMenu;
    @Shadow
    private float[] colourBuffer = new float[4];
    @Shadow
    private ArrayList<MapRegion> regionBuffer = new ArrayList();
    @Shadow
    private ArrayList<BranchLeveledRegion> branchRegionBuffer = new ArrayList();
    @Shadow
    private boolean prevWaitingForBranchCache = true;
    @Shadow
    private boolean prevLoadingLeaves = true;
    @Shadow
    private RegistryKey<World> lastViewedDimensionId;
    @Shadow
    private String lastViewedMultiworldId;
    @Shadow
    private int mouseBlockPosX;
    @Shadow
    private int mouseBlockPosY;
    @Shadow
    private int mouseBlockPosZ;
    @Shadow
    private long lastStartTime;
    @Final
    @Shadow
    private GuiMapSwitching mapSwitchingGui;
    @Shadow
    private MapMouseButtonPress leftMouseButton;
    @Shadow
    private MapMouseButtonPress rightMouseButton;
    @Shadow
    private MapProcessor mapProcessor;
    @Shadow
    private MapDimension dimension;
    @Shadow
    private boolean[] waitingForBranchCache = new boolean[1];
    @Shadow
    private ButtonWidget settingsButton;
    @Shadow
    private ButtonWidget playersButton;
    @Shadow
    private ButtonWidget exportButton;
    @Shadow
    private ButtonWidget waypointsButton;
    @Shadow
    private ButtonWidget radarButton;
    @Shadow
    private ButtonWidget claimsButton;
    @Shadow
    private ButtonWidget zoomInButton;
    @Shadow
    private ButtonWidget zoomOutButton;
    @Shadow
    private ButtonWidget keybindingsButton;
    @Shadow
    private ButtonWidget caveModeButton;
    @Shadow
    private ButtonWidget buttonPressed;
    @Shadow
    private GuiRightClickMenu rightClickMenu;
    @Shadow
    private int rightClickX;
    @Shadow
    private int rightClickY;
    @Shadow
    private int rightClickZ;
    @Shadow
    private boolean lastFrameRenderedRootTextures;
    @Shadow
    private MapTileSelection mapTileSelection;
    @Shadow
    private boolean tabPressed;
    @Shadow
    private GuiCaveModeOptions caveModeOptions;
    @Shadow
    private boolean shouldResetCameraPos;

    protected MixinGuiMap(final Screen parent, final Screen escape, final Text titleIn) {
        super(parent, escape, titleIn);
    }

    @Shadow
    private static long bytesToMb(long bytes) {
        return bytes / 1024L / 1024L;
    }

    @Shadow
    protected abstract void closeDropdowns();

    @Shadow
    protected abstract double getScaleMultiplier(int screenShortSide);

    @Shadow
    protected abstract void setColourBuffer(float r, float g, float b, float a);

    @Shadow
    public abstract void drawDotOnMap(MatrixStack matrixStack, VertexConsumer guiLinearBuffer, double x, double z, float angle, double sc);

    @Shadow
    public abstract void drawFarArrowOnMap(MatrixStack matrixStack, VertexConsumer guiLinearBuffer, double x, double z, float angle, double sc);

    @Shadow
    public abstract void drawArrowOnMap(MatrixStack matrixStack, VertexConsumer guiLinearBuffer, double x, double z, float angle, double sc);

    @Shadow
    protected abstract void onWaypointsButton(ButtonWidget b);

    @Shadow
    protected abstract void renderLoadingScreen(DrawContext guiGraphics);

    @Shadow
    protected abstract void renderMessageScreen(DrawContext guiGraphics, String message);

    @Shadow
    protected abstract void renderMessageScreen(DrawContext guiGraphics, String message, String message2);

    @Shadow
    protected abstract <E, C> CursorBox hoveredElementTooltipHelper(HoveredMapElementHolder<E, C> hovered, boolean viewedInList);


    @Shadow public abstract <T extends Element & Drawable & Selectable> T addButton(final T guiEventListener);

    @Shadow public abstract <T extends Element & Selectable> T addSelectableChild(final T guiEventListener);

    /**
     * @author rfresh2
     * @reason Unlock minimum zoom level on WorldMap GUI
     */
    @Overwrite
    private void changeZoom(double factor, int zoomMethod) {
        this.closeDropdowns();
        this.lastZoomMethod = zoomMethod;
        this.cameraDestinationAnimX = null;
        this.cameraDestinationAnimZ = null;
        if (GuiMap.hasControlDown()) {
            double destScaleBefore = destScale;
            if (destScale >= 1.0) {
                if (factor > 0.0) {
                    destScale = Math.ceil(destScale);
                } else {
                    destScale = Math.floor(destScale);
                }

                if (destScaleBefore == destScale) {
                    destScale += factor > 0.0 ? 1.0 : -1.0;
                }

                if (destScale == 0.0) {
                    destScale = 0.5;
                }
            } else {
                double reversedScale = 1.0 / destScale;
                double log2 = Math.log(reversedScale) / Math.log(2.0);
                if (factor > 0.0) {
                    log2 = Math.floor(log2);
                } else {
                    log2 = Math.ceil(log2);
                }

                destScale = 1.0 / Math.pow(2.0, log2);
                if (destScaleBefore == destScale) {
                    destScale = 1.0 / Math.pow(2.0, log2 + (double) (factor > 0.0 ? -1 : 1));
                }
            }
        } else {
            destScale *= Math.pow(1.2, factor);
        }

        if (destScale < XaeroPlusSettingRegistry.worldMapMinZoomSetting.getValue() / 10.0f) {
            // insert our own min zoom
            destScale = XaeroPlusSettingRegistry.worldMapMinZoomSetting.getValue() / 10.0f;
        } else if (destScale > 50.0) {
            destScale = 50.0;
        }
    }


    @Inject(method = "<init>", at = @At("RETURN"), remap = true)
    public void constructorInject(final Screen parent, final Screen escape, final MapProcessor mapProcessor, final Entity player, final CallbackInfo ci) {
        RegistryKey<World> dim = MinecraftClient.getInstance().world.getRegistryKey();
        this.cameraX = player.getX();
        this.cameraZ = player.getZ();
        if ((dim == NETHER || Globals.customDimensionId == NETHER) && dim != Globals.customDimensionId) {
            if (Globals.customDimensionId == OVERWORLD) {
                this.cameraX = player.getX() * 8.0;
            } else if (Globals.customDimensionId == NETHER && dim == OVERWORLD) {
                this.cameraX = player.getX() / 8.0;
            }
        }
        if ((dim == NETHER || Globals.customDimensionId == NETHER) && dim != Globals.customDimensionId) {
            if (Globals.customDimensionId == OVERWORLD) {
                this.cameraZ = player.getZ() * 8.0;
            } else if (Globals.customDimensionId == NETHER && dim == OVERWORLD) {
                this.cameraZ = player.getZ() / 8.0;
            }
        }
    }

    @Inject(method = "init", at = @At(value = "RETURN"), remap = true)
    public void customInitGui(CallbackInfo ci) {
        followButton = new GuiTexturedButton(0, this.caveModeButton.getY() - 20, 20, 20, FOLLOW ? 133 : 149, 16, 16, 16,
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
                Globals.customDimensionId = MinecraftClient.getInstance().world.getRegistryKey();
            } catch (final Exception e) {
                Globals.customDimensionId = OVERWORLD;
            }
            WorldMap.settings.minimapRadar = true; // todo: restore previous value before custom dimension was entered (if at all)
        }
        super.onExit(screen);
    }

    /**
     * @author rfresh2
     * @reason custom rendering
     */
    @Overwrite
    public void render(DrawContext guiGraphics, int scaledMouseX, int scaledMouseY, float partialTicks) {
        while(GL11.glGetError() != 0) {
        }
        final boolean isDimensionSwitched = Globals.customDimensionId != MinecraftClient.getInstance().world.getRegistryKey();

        GlStateManager._clearColor(0.0F, 0.0F, 0.0F, 0.0F);
        GL11.glClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        MapShaders.ensureShaders();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        MinecraftClient mc = MinecraftClient.getInstance();
        if (this.shouldResetCameraPos) {
            this.cameraX = (float)this.player.getX();
            this.cameraZ = (float)this.player.getZ();
            this.shouldResetCameraPos = false;
        }
        MatrixStack matrixStack = guiGraphics.getMatrices();
        double cameraXBefore = this.cameraX;
        double cameraZBefore = this.cameraZ;
        double scaleBefore = this.scale;
        long startTime = System.currentTimeMillis();
        MapDimension currentDim = !this.mapProcessor.isMapWorldUsable() ? null : this.mapProcessor.getMapWorld().getCurrentDimension();
        if (currentDim != this.dimension) {
            this.mapSwitchingGui.active = false;
            this.init(this.client, this.width, this.height);
        }

        this.mapSwitchingGui.preMapRender((GuiMap) (Object)this, this.client, this.width, this.height);
        long passed = this.lastStartTime == 0L ? 16L : startTime - this.lastStartTime;
        double passedScrolls = (double)((float)passed / 64.0F);
        int direction = this.buttonPressed != this.zoomInButton && !ControlsHandler.isDown(ControlsRegister.keyZoomIn)
                ? (this.buttonPressed != this.zoomOutButton && !ControlsHandler.isDown(ControlsRegister.keyZoomOut) ? 0 : -1)
                : 1;
        if (direction != 0) {
            boolean ctrlKey = hasControlDown();
            if (!ctrlKey || !this.pauseZoomKeys) {
                this.changeZoom((double)direction * passedScrolls, this.buttonPressed != this.zoomInButton && this.buttonPressed != this.zoomOutButton ? 1 : 2);
                if (ctrlKey) {
                    this.pauseZoomKeys = true;
                }
            }
        } else {
            this.pauseZoomKeys = false;
        }

        this.lastStartTime = startTime;
        if (FOLLOW && isNull(this.cameraDestination) && isNull(this.cameraDestinationAnimX) && isNull(this.cameraDestinationAnimZ)) {
            this.cameraDestination = new int[]{(int) getPlayerX(), (int) getPlayerZ()};
        }
        if (this.cameraDestination != null) {
            this.cameraDestinationAnimX = new SlowingAnimation(this.cameraX, this.cameraDestination[0], 0.9, 0.01);
            this.cameraDestinationAnimZ = new SlowingAnimation(this.cameraZ, this.cameraDestination[1], 0.9, 0.01);
            this.cameraDestination = null;
        }

        if (this.cameraDestinationAnimX != null) {
            this.cameraX = this.cameraDestinationAnimX.getCurrent();
            if (this.cameraX == this.cameraDestinationAnimX.getDestination()) {
                this.cameraDestinationAnimX = null;
            }
        }

        if (this.cameraDestinationAnimZ != null) {
            this.cameraZ = this.cameraDestinationAnimZ.getCurrent();
            if (this.cameraZ == this.cameraDestinationAnimZ.getDestination()) {
                this.cameraDestinationAnimZ = null;
            }
        }

        this.lastViewedDimensionId = null;
        this.lastViewedMultiworldId = null;
        this.mouseBlockPosY = 32767;
        boolean discoveredForHighlights = false;
        synchronized(this.mapProcessor.renderThreadPauseSync) {
            if (this.mapProcessor.isRenderingPaused()) {
                this.renderLoadingScreen(guiGraphics);
            } else {
                boolean mapLoaded = this.mapProcessor.getCurrentWorldId() != null
                        && !this.mapProcessor.isWaitingForWorldUpdate()
                        && this.mapProcessor.getMapSaveLoad().isRegionDetectionComplete();
                boolean noWorldMapEffect = mc.player == null
                    || mc.player.hasStatusEffect(Effects.NO_WORLD_MAP)
                    || mc.player.hasStatusEffect(Effects.NO_WORLD_MAP_HARMFUL);
                boolean allowedBasedOnItem = ModSettings.mapItem == null || mc.player != null && Misc.hasItem(mc.player, ModSettings.mapItem);
                boolean isLocked = this.mapProcessor.isCurrentMapLocked();
                if (mapLoaded && !noWorldMapEffect && allowedBasedOnItem && !isLocked) {
                    if (SupportMods.vivecraft) {
                        GlStateManager._clearColor(0.0F, 0.0F, 0.0F, 1.0F);
                        GlStateManager._clear(16384, MinecraftClient.IS_SYSTEM_MAC);
                    }

                    this.mapProcessor.updateCaveStart();
                    this.lastViewedDimensionId = this.mapProcessor.getMapWorld().getCurrentDimension().getDimId();
                    this.lastViewedMultiworldId = this.mapProcessor.getMapWorld().getCurrentDimension().getCurrentMultiworld();
                    if (SupportMods.minimap()) {
                        SupportMods.xaeroMinimap
                                .checkWaypoints(
                                        this.mapProcessor.getMapWorld().isMultiplayer(),
                                        this.lastViewedDimensionId,
                                        this.lastViewedMultiworldId,
                                        this.width,
                                        this.height,
                                        (GuiMap) (Object) this
                                );
                    }

                    int mouseXPos = (int)Misc.getMouseX(mc, false);
                    int mouseYPos = (int)Misc.getMouseY(mc, false);
                    double scaleMultiplier = this.getScaleMultiplier(Math.min(mc.getWindow().getFramebufferWidth(), mc.getWindow().getFramebufferHeight()));
                    this.scale = this.userScale * scaleMultiplier;
                    if (this.mouseCheckPosX == -1 || System.nanoTime() - this.mouseCheckTimeNano > 30000000L) {
                        this.prevMouseCheckPosX = this.mouseCheckPosX;
                        this.prevMouseCheckPosY = this.mouseCheckPosY;
                        this.prevMouseCheckTimeNano = this.mouseCheckTimeNano;
                        this.mouseCheckPosX = mouseXPos;
                        this.mouseCheckPosY = mouseYPos;
                        this.mouseCheckTimeNano = System.nanoTime();
                    }

                    if (!this.leftMouseButton.isDown) {
                        if (this.mouseDownPosX != -1) {
                            this.mouseDownPosX = -1;
                            this.mouseDownPosY = -1;
                            if (this.prevMouseCheckTimeNano != -1L) {
                                double downTime = 0.0;
                                int draggedX = 0;
                                int draggedY = 0;
                                downTime = (double)(System.nanoTime() - this.prevMouseCheckTimeNano);
                                draggedX = mouseXPos - this.prevMouseCheckPosX;
                                draggedY = mouseYPos - this.prevMouseCheckPosY;
                                double frameTime60FPS = 1.6666666666666666E7;
                                double speedScale = downTime / frameTime60FPS;
                                double speed_x = (double)(-draggedX) / this.scale / speedScale;
                                double speed_z = (double)(-draggedY) / this.scale / speedScale;
                                double speed = Math.sqrt(speed_x * speed_x + speed_z * speed_z);
                                if (speed > 0.0) {
                                    double cos = speed_x / speed;
                                    double sin = speed_z / speed;
                                    double maxSpeed = 500.0 / this.userScale;
                                    speed = Math.abs(speed) > maxSpeed ? Math.copySign(maxSpeed, speed) : speed;
                                    double speed_factor = 0.9;
                                    double ln = Math.log(speed_factor);
                                    double move_distance = -speed / ln;
                                    double moveX = cos * move_distance;
                                    double moveZ = sin * move_distance;
                                    this.cameraDestinationAnimX = new SlowingAnimation(this.cameraX, this.cameraX + moveX, 0.9, 0.01);
                                    this.cameraDestinationAnimZ = new SlowingAnimation(this.cameraZ, this.cameraZ + moveZ, 0.9, 0.01);
                                }
                            }
                        }
                    } else if (this.viewed == null || !this.viewedInList || this.mouseDownPosX != -1) {
                        if (this.mouseDownPosX != -1) {
                            this.cameraX = (double)(this.mouseDownPosX - mouseXPos) / this.scale + this.mouseDownCameraX;
                            this.cameraZ = (double)(this.mouseDownPosY - mouseYPos) / this.scale + this.mouseDownCameraZ;
                        } else {
                            this.mouseDownPosX = mouseXPos;
                            this.mouseDownPosY = mouseYPos;
                            this.mouseDownCameraX = this.cameraX;
                            this.mouseDownCameraZ = this.cameraZ;
                            this.cameraDestinationAnimX = null;
                            this.cameraDestinationAnimZ = null;
                        }
                    }

                    int mouseFromCentreX = mouseXPos - mc.getWindow().getFramebufferWidth() / 2;
                    int mouseFromCentreY = mouseYPos - mc.getWindow().getFramebufferHeight() / 2;
                    double oldMousePosX = (double)mouseFromCentreX / this.scale + this.cameraX;
                    double oldMousePosZ = (double)mouseFromCentreY / this.scale + this.cameraZ;
                    double preScale = this.scale;
                    if (destScale != this.userScale) {
                        if (this.zoomAnim != null) {
                            this.userScale = this.zoomAnim.getCurrent();
                            this.scale = this.userScale * scaleMultiplier;
                        }

                        if (this.zoomAnim == null || Misc.round(this.zoomAnim.getDestination(), 4) != Misc.round(destScale, 4)) {
                            this.zoomAnim = new SinAnimation(this.userScale, destScale, 100L);
                        }
                    }

                    if (this.scale > preScale && this.lastZoomMethod != 2) {
                        this.cameraX = oldMousePosX - (double)mouseFromCentreX / this.scale;
                        this.cameraZ = oldMousePosZ - (double)mouseFromCentreY / this.scale;
                    }

                    int textureLevel = 0;
                    double fboScale;
                    if (this.scale >= 1.0) {
                        fboScale = Math.max(1.0, Math.floor(this.scale));
                    } else {
                        fboScale = this.scale;
                    }

                    if (this.userScale < 1.0) {
                        double reversedScale = 1.0 / this.userScale;
                        double log2 = Math.floor(Math.log(reversedScale) / Math.log(2.0));
                        textureLevel = Math.min((int)log2, 3);
                    }

                    this.mapProcessor.getMapSaveLoad().mainTextureLevel = textureLevel;
                    int leveledRegionShift = 9 + textureLevel;
                    double secondaryScale = this.scale / fboScale;
                    matrixStack.push();
                    double mousePosX = (double)mouseFromCentreX / this.scale + this.cameraX;
                    double mousePosZ = (double)mouseFromCentreY / this.scale + this.cameraZ;
                    matrixStack.push();
                    matrixStack.translate(0.0F, 0.0F, 971.0F);
                    this.mouseBlockPosX = (int)Math.floor(mousePosX);
                    this.mouseBlockPosZ = (int)Math.floor(mousePosZ);
                    int mouseRegX = this.mouseBlockPosX >> leveledRegionShift;
                    int mouseRegZ = this.mouseBlockPosZ >> leveledRegionShift;
                    int renderedCaveLayer = this.mapProcessor.getCurrentCaveLayer();
                    final CustomDimensionMapProcessor customDimensionMapProcessor = (CustomDimensionMapProcessor) this.mapProcessor;
                    LeveledRegion<?> reg = customDimensionMapProcessor.getLeveledRegionCustomDimension(renderedCaveLayer, mouseRegX, mouseRegZ, textureLevel, Globals.customDimensionId);
                    int maxRegBlockCoord = (1 << leveledRegionShift) - 1;
                    int mouseRegPixelX = (this.mouseBlockPosX & maxRegBlockCoord) >> textureLevel;
                    int mouseRegPixelZ = (this.mouseBlockPosZ & maxRegBlockCoord) >> textureLevel;
                    this.mouseBlockPosX = (mouseRegX << leveledRegionShift) + (mouseRegPixelX << textureLevel);
                    this.mouseBlockPosZ = (mouseRegZ << leveledRegionShift) + (mouseRegPixelZ << textureLevel);
                    if (this.mapTileSelection != null && this.rightClickMenu == null) {
                        this.mapTileSelection.setEnd(this.mouseBlockPosX >> 4, this.mouseBlockPosZ >> 4);
                    }

                    MapRegion leafRegion = customDimensionMapProcessor.getMapRegionCustomDimension(renderedCaveLayer, this.mouseBlockPosX >> 9, this.mouseBlockPosZ >> 9, false, Globals.customDimensionId);
                    MapTileChunk chunk = leafRegion == null ? null : leafRegion.getChunk(this.mouseBlockPosX >> 6 & 7, this.mouseBlockPosZ >> 6 & 7);
                    int debugTextureX = this.mouseBlockPosX >> leveledRegionShift - 3 & 7;
                    int debugTextureY = this.mouseBlockPosZ >> leveledRegionShift - 3 & 7;
                    RegionTexture tex = reg != null && reg.hasTextures() ? reg.getTexture(debugTextureX, debugTextureY) : null;
                    if (WorldMap.settings.debug && !mc.options.hudHidden) {
                        if (reg != null) {
                            List<String> debugLines = new ArrayList<>();
                            if (tex != null) {
                                tex.addDebugLines(debugLines);
                                MapTile mouseTile = chunk == null ? null : chunk.getTile(this.mouseBlockPosX >> 4 & 3, this.mouseBlockPosZ >> 4 & 3);
                                if (mouseTile != null) {
                                    MapBlock block = mouseTile.getBlock(this.mouseBlockPosX & 15, this.mouseBlockPosZ & 15);
                                    if (block != null) {
                                        guiGraphics.drawCenteredTextWithShadow(
                                                mc.textRenderer, block.toRenderString(leafRegion.getBiomeRegistry()), this.width / 2, 22, -1
                                        );
                                        if (block.getNumberOfOverlays() != 0) {
                                            for(int i = 0; i < block.getOverlays().size(); ++i) {
                                                guiGraphics.drawCenteredTextWithShadow(
                                                        mc.textRenderer, block.getOverlays().get(i).toRenderString(), this.width / 2, 32 + i * 10, -1
                                                );
                                            }
                                        }
                                    }
                                }
                            }

                            debugLines.add("");
                            debugLines.add(reg.toString());
                            reg.addDebugLines(debugLines, this.mapProcessor, debugTextureX, debugTextureY);

                            for(int i = 0; i < debugLines.size(); ++i) {
                                guiGraphics.drawTextWithShadow(mc.textRenderer, debugLines.get(i), 5, 15 + 10 * i, -1);
                            }
                        }

                        guiGraphics.drawTextWithShadow(mc.textRenderer, "MultiWorld ID: " + this.mapProcessor.getMapWorld().getCurrentMultiworld(), 5, 255, -1);
                        LayeredRegionManager regions = this.mapProcessor.getMapWorld().getDimension(Globals.customDimensionId).getLayeredMapRegions();
                        guiGraphics.drawTextWithShadow(
                                mc.textRenderer,
                                String.format(
                                        "regions: %d loaded: %d processed: %d viewed: %d benchmarks %s",
                                        regions.size(),
                                        regions.loadedCount(),
                                        this.mapProcessor.getProcessedCount(),
                                        lastAmountOfRegionsViewed,
                                        WorldMap.textureUploadBenchmark.getTotalsString()
                                ),
                                5,
                                265,
                                -1
                        );
                        guiGraphics.drawTextWithShadow(
                                mc.textRenderer,
                                String.format(
                                        "toLoad: %d toSave: %d tile pool: %d overlays: %d toLoadBranchCache: %d buffers: %d",
                                        this.mapProcessor.getMapSaveLoad().getSizeOfToLoad(),
                                        this.mapProcessor.getMapSaveLoad().getToSave().size(),
                                        this.mapProcessor.getTilePool().size(),
                                        this.mapProcessor.getOverlayManager().getNumberOfUniqueOverlays(),
                                        this.mapProcessor.getMapSaveLoad().getSizeOfToLoadBranchCache(),
                                        WorldMap.textureDirectBufferPool.size()
                                ),
                                5,
                                275,
                                -1
                        );
                        long i = Runtime.getRuntime().maxMemory();
                        long j = Runtime.getRuntime().totalMemory();
                        long k = Runtime.getRuntime().freeMemory();
                        long l = j - k;
                        int debugFPS = this.mapProcessor.getDebugFPS(mc);
                        guiGraphics.drawTextWithShadow(mc.textRenderer, String.format("FPS: %d", debugFPS), 5, 295, -1);
                        guiGraphics.drawTextWithShadow(
                                mc.textRenderer, String.format("Mem: % 2d%% %03d/%03dMB", l * 100L / i, bytesToMb(l), bytesToMb(i)), 5, 305, -1
                        );
                        guiGraphics.drawTextWithShadow(mc.textRenderer, String.format("Allocated: % 2d%% %03dMB", j * 100L / i, bytesToMb(j)), 5, 315, -1);
                        guiGraphics.drawTextWithShadow(
                                mc.textRenderer, String.format("Available VRAM: %dMB", this.mapProcessor.getMapLimiter().getAvailableVRAM() / 1024), 5, 325, -1
                        );
                    }

                    int pixelInsideTexX = mouseRegPixelX & 63;
                    int pixelInsideTexZ = mouseRegPixelZ & 63;
                    boolean hasAmbiguousHeight = false;
                    int mouseBlockBottomY = 32767;
                    int mouseBlockTopY = 32767;
                    RegistryKey<Biome> pointedAtBiome = null;
                    if (tex != null) {
                        mouseBlockBottomY = this.mouseBlockPosY = tex.getHeight(pixelInsideTexX, pixelInsideTexZ);
                        mouseBlockTopY = tex.getTopHeight(pixelInsideTexX, pixelInsideTexZ);
                        hasAmbiguousHeight = this.mouseBlockPosY != mouseBlockTopY;
                        pointedAtBiome = tex.getBiome(pixelInsideTexX, pixelInsideTexZ);
                    }

                    if (hasAmbiguousHeight) {
                        if (mouseBlockTopY != 32767) {
                            this.mouseBlockPosY = mouseBlockTopY;
                        } else if (WorldMap.settings.detectAmbiguousY) {
                            this.mouseBlockPosY = 32767;
                        }
                    }

                    matrixStack.pop();
                    if (primaryScaleFBO == null
                            || primaryScaleFBO.viewportWidth != mc.getWindow().getFramebufferWidth()
                            || primaryScaleFBO.viewportHeight != mc.getWindow().getFramebufferHeight()) {
                        primaryScaleFBO = new ImprovedFramebuffer(mc.getWindow().getFramebufferWidth(), mc.getWindow().getFramebufferHeight(), false);
                    }

                    if (primaryScaleFBO.fbo == -1) {
                        matrixStack.pop();
                        return;
                    }

                    primaryScaleFBO.beginWrite(false);
                    GlStateManager._clearColor(0.0F, 0.0F, 0.0F, 1.0F);
                    GlStateManager._clear(16384, MinecraftClient.IS_SYSTEM_MAC);
                    matrixStack.scale((float)(1.0 / this.screenScale), (float)(1.0 / this.screenScale), 1.0F);
                    matrixStack.translate((float)(mc.getWindow().getFramebufferWidth() / 2), (float)(mc.getWindow().getFramebufferHeight() / 2), 0.0F);
                    matrixStack.push();
                    int flooredCameraX = (int)Math.floor(this.cameraX);
                    int flooredCameraZ = (int)Math.floor(this.cameraZ);
                    double primaryOffsetX = 0.0;
                    double primaryOffsetY = 0.0;
                    double secondaryOffsetX;
                    double secondaryOffsetY;
                    if (fboScale < 1.0) {
                        double pixelInBlocks = 1.0 / fboScale;
                        int xInFullPixels = (int)Math.floor(this.cameraX / pixelInBlocks);
                        int zInFullPixels = (int)Math.floor(this.cameraZ / pixelInBlocks);
                        double fboOffsetX = (double)xInFullPixels * pixelInBlocks;
                        double fboOffsetZ = (double)zInFullPixels * pixelInBlocks;
                        flooredCameraX = (int)Math.floor(fboOffsetX);
                        flooredCameraZ = (int)Math.floor(fboOffsetZ);
                        primaryOffsetX = fboOffsetX - (double)flooredCameraX;
                        primaryOffsetY = fboOffsetZ - (double)flooredCameraZ;
                        secondaryOffsetX = (this.cameraX - fboOffsetX) * fboScale;
                        secondaryOffsetY = (this.cameraZ - fboOffsetZ) * fboScale;
                    } else {
                        secondaryOffsetX = (this.cameraX - (double)flooredCameraX) * fboScale;
                        secondaryOffsetY = (this.cameraZ - (double)flooredCameraZ) * fboScale;
                        if (secondaryOffsetX >= 1.0) {
                            int offset = (int)secondaryOffsetX;
                            matrixStack.translate((float)(-offset), 0.0F, 0.0F);
                            secondaryOffsetX -= (double)offset;
                        }

                        if (secondaryOffsetY >= 1.0) {
                            int offset = (int)secondaryOffsetY;
                            matrixStack.translate(0.0F, (float)offset, 0.0F);
                            secondaryOffsetY -= (double)offset;
                        }
                    }

                    matrixStack.scale((float)fboScale, (float)(-fboScale), 1.0F);
                    matrixStack.translate(-primaryOffsetX, -primaryOffsetY, 0.0);
                    double leftBorder = this.cameraX - (double)(mc.getWindow().getFramebufferWidth() / 2) / this.scale;
                    double rightBorder = leftBorder + (double)mc.getWindow().getFramebufferWidth() / this.scale;
                    double topBorder = this.cameraZ - (double)(mc.getWindow().getFramebufferHeight() / 2) / this.scale;
                    double bottomBorder = topBorder + (double)mc.getWindow().getFramebufferHeight() / this.scale;
                    int minRegX = (int)Math.floor(leftBorder) >> leveledRegionShift;
                    int maxRegX = (int)Math.floor(rightBorder) >> leveledRegionShift;
                    int minRegZ = (int)Math.floor(topBorder) >> leveledRegionShift;
                    int maxRegZ = (int)Math.floor(bottomBorder) >> leveledRegionShift;
                    int blockToTextureConversion = 6 + textureLevel;
                    int minTextureX = (int)Math.floor(leftBorder) >> blockToTextureConversion;
                    int maxTextureX = (int)Math.floor(rightBorder) >> blockToTextureConversion;
                    int minTextureZ = (int)Math.floor(topBorder) >> blockToTextureConversion;
                    int maxTextureZ = (int)Math.floor(bottomBorder) >> blockToTextureConversion;
                    int minLeafRegX = minTextureX << blockToTextureConversion >> 9;
                    int maxLeafRegX = (maxTextureX + 1 << blockToTextureConversion) - 1 >> 9;
                    int minLeafRegZ = minTextureZ << blockToTextureConversion >> 9;
                    int maxLeafRegZ = (maxTextureZ + 1 << blockToTextureConversion) - 1 >> 9;
                    lastAmountOfRegionsViewed = (maxRegX - minRegX + 1) * (maxRegZ - minRegZ + 1);
                    if (this.mapProcessor.getMapLimiter().getMostRegionsAtATime() < lastAmountOfRegionsViewed) {
                        this.mapProcessor.getMapLimiter().setMostRegionsAtATime(lastAmountOfRegionsViewed);
                    }

                    this.regionBuffer.clear();
                    this.branchRegionBuffer.clear();
                    float brightness = this.mapProcessor.getBrightness();
                    int globalRegionCacheHashCode = WorldMap.settings.getRegionCacheHashCode();
                    int globalCaveStart = this.mapProcessor.getMapWorld().getDimension(Globals.customDimensionId).getLayeredMapRegions().getLayer(renderedCaveLayer).getCaveStart();
                    int globalCaveDepth = WorldMap.settings.caveModeDepth;
                    boolean reloadEverything = WorldMap.settings.reloadEverything;
                    int globalReloadVersion = WorldMap.settings.reloadVersion;
                    int globalVersion = this.mapProcessor.getGlobalVersion();
                    boolean prevWaitingForBranchCache = this.prevWaitingForBranchCache;
                    this.waitingForBranchCache[0] = false;
                    Matrix4f matrix = matrixStack.peek().getPositionMatrix();
                    VertexConsumerProvider.Immediate renderTypeBuffers = this.mapProcessor.getCvc().getRenderTypeBuffers();
                    MultiTextureRenderTypeRendererProvider rendererProvider = this.mapProcessor.getMultiTextureRenderTypeRenderers();
                    MultiTextureRenderTypeRenderer withLightRenderer = rendererProvider.getRenderer(
                            t -> RenderSystem.setShaderTexture(0, t), MultiTextureRenderTypeRendererProvider::defaultTextureBind, CustomRenderTypes.MAP
                    );
                    MultiTextureRenderTypeRenderer noLightRenderer = rendererProvider.getRenderer(
                            t -> RenderSystem.setShaderTexture(0, t), MultiTextureRenderTypeRendererProvider::defaultTextureBind, CustomRenderTypes.MAP
                    );
                    VertexConsumer overlayBuffer = renderTypeBuffers.getBuffer(CustomRenderTypes.MAP_COLOR_OVERLAY);
                    LeveledRegion.setComparison(
                            this.mouseBlockPosX >> leveledRegionShift,
                            this.mouseBlockPosZ >> leveledRegionShift,
                            textureLevel,
                            this.mouseBlockPosX >> 9,
                            this.mouseBlockPosZ >> 9
                    );
                    LeveledRegion<?> lastUpdatedRootLeveledRegion = null;
                    boolean frameRenderedRootTextures = false;
                    boolean loadingLeaves = false;

                    for(int leveledRegX = minRegX; leveledRegX <= maxRegX; ++leveledRegX) {
                        for(int leveledRegZ = minRegZ; leveledRegZ <= maxRegZ; ++leveledRegZ) {
                            int leveledSideInRegions = 1 << textureLevel;
                            int leveledSideInBlocks = leveledSideInRegions * 512;
                            int leafRegionMinX = leveledRegX * leveledSideInRegions;
                            int leafRegionMinZ = leveledRegZ * leveledSideInRegions;
                            LeveledRegion<?> leveledRegion = null;

                            for(int leafX = 0; leafX < leveledSideInRegions; ++leafX) {
                                for(int leafZ = 0; leafZ < leveledSideInRegions; ++leafZ) {
                                    int regX = leafRegionMinX + leafX;
                                    if (regX >= minLeafRegX && regX <= maxLeafRegX) {
                                        int regZ = leafRegionMinZ + leafZ;
                                        if (regZ >= minLeafRegZ && regZ <= maxLeafRegZ) {
                                            MapRegion region = customDimensionMapProcessor.getMapRegionCustomDimension(renderedCaveLayer, regX, regZ, false, Globals.customDimensionId);
                                            if (region == null) {
                                                region = customDimensionMapProcessor
                                                        .getMapRegionCustomDimension(renderedCaveLayer, regX, regZ, customDimensionMapProcessor.regionExistsCustomDimension(renderedCaveLayer, regX, regZ, Globals.customDimensionId), Globals.customDimensionId);
                                            }

                                            if (region != null) {
                                                if (leveledRegion == null) {
                                                    leveledRegion = customDimensionMapProcessor.getLeveledRegionCustomDimension(renderedCaveLayer, leveledRegX, leveledRegZ, textureLevel, Globals.customDimensionId);
                                                }

                                                if (!prevWaitingForBranchCache) {
                                                    synchronized(region) {
                                                        if (textureLevel != 0
                                                                && region.getLoadState() == 0
                                                                && region.loadingNeededForBranchLevel != 0
                                                                && region.loadingNeededForBranchLevel != textureLevel) {
                                                            region.loadingNeededForBranchLevel = 0;
                                                            region.getParent().setShouldCheckForUpdatesRecursive(true);
                                                        }

                                                        if (region.canRequestReload_unsynced()
                                                            && (
                                                                reloadEverything && region.getReloadVersion() != globalReloadVersion
                                                                        || region.getCacheHashCode() != globalRegionCacheHashCode
                                                                        || region.caveStartOutdated(globalCaveStart, globalCaveDepth)
                                                                        || region.getVersion() != globalVersion
                                                                        || (region.isMetaLoaded() || region.getLoadState() != 0 || !region.hasHadTerrain())
                                                                        && region.getHighlightsHash()
                                                                        != region.getDim().getHighlightHandler().getRegionHash(region.getRegionX(), region.getRegionZ())
                                                                        || region.getLoadState() != 2 && region.shouldCache()
                                                                        || region.getLoadState() == 0 && (textureLevel == 0 || region.loadingNeededForBranchLevel == textureLevel)
                                                        )) {
                                                            loadingLeaves = true;
                                                            region.calculateSortingDistance();
                                                            /** Increase buffer to 100 **/
                                                            Misc.addToListOfSmallest(100, this.regionBuffer, region);
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            if (leveledRegion != null) {
                                LeveledRegion<?> rootLeveledRegion = leveledRegion.getRootRegion();
                                if (rootLeveledRegion == leveledRegion) {
                                    rootLeveledRegion = null;
                                }

                                if (rootLeveledRegion != null && !rootLeveledRegion.isLoaded()) {
                                    if (!rootLeveledRegion.recacheHasBeenRequested() && !rootLeveledRegion.reloadHasBeenRequested()) {
                                        rootLeveledRegion.calculateSortingDistance();
                                        /** Increase buffer to 100 **/
                                        Misc.addToListOfSmallest(100, this.branchRegionBuffer, (BranchLeveledRegion)rootLeveledRegion);
                                    }

                                    this.waitingForBranchCache[0] = true;
                                    rootLeveledRegion = null;
                                }

                                if (!this.mapProcessor.isUploadingPaused() && !WorldMap.settings.pauseRequests) {
                                    if (leveledRegion instanceof BranchLeveledRegion branchRegion) {
                                        branchRegion.checkForUpdates(
                                                this.mapProcessor,
                                                prevWaitingForBranchCache,
                                                this.waitingForBranchCache,
                                                this.branchRegionBuffer,
                                                textureLevel,
                                                minLeafRegX,
                                                minLeafRegZ,
                                                maxLeafRegX,
                                                maxLeafRegZ
                                        );
                                    }

                                    if ((textureLevel != 0 && !prevWaitingForBranchCache || textureLevel == 0 && !this.prevLoadingLeaves)
                                            && this.lastFrameRenderedRootTextures
                                            && rootLeveledRegion != null
                                            && rootLeveledRegion != lastUpdatedRootLeveledRegion) {
                                        BranchLeveledRegion branchRegion = (BranchLeveledRegion)rootLeveledRegion;
                                        branchRegion.checkForUpdates(
                                                this.mapProcessor,
                                                prevWaitingForBranchCache,
                                                this.waitingForBranchCache,
                                                this.branchRegionBuffer,
                                                textureLevel,
                                                minLeafRegX,
                                                minLeafRegZ,
                                                maxLeafRegX,
                                                maxLeafRegZ
                                        );
                                        lastUpdatedRootLeveledRegion = rootLeveledRegion;
                                    }

                                    this.mapProcessor.getMapWorld().getDimension(Globals.customDimensionId).getLayeredMapRegions().bumpLoadedRegion(leveledRegion);
                                    if (rootLeveledRegion != null) {
                                        this.mapProcessor.getMapWorld().getDimension(Globals.customDimensionId).getLayeredMapRegions().bumpLoadedRegion(rootLeveledRegion);
                                    }
                                } else {
                                    this.waitingForBranchCache[0] = prevWaitingForBranchCache;
                                }

                                int minXBlocks = leveledRegX * leveledSideInBlocks;
                                int minZBlocks = leveledRegZ * leveledSideInBlocks;
                                int textureSize = 64 * leveledSideInRegions;
                                int firstTextureX = leveledRegX << 3;
                                int firstTextureZ = leveledRegZ << 3;
                                int levelDiff = 3 - textureLevel;
                                int rootSize = 1 << levelDiff;
                                int maxInsideCoord = rootSize - 1;
                                int firstRootTextureX = firstTextureX >> levelDiff & 7;
                                int firstRootTextureZ = firstTextureZ >> levelDiff & 7;
                                int firstInsideTextureX = firstTextureX & maxInsideCoord;
                                int firstInsideTextureZ = firstTextureZ & maxInsideCoord;
                                boolean hasTextures = leveledRegion.hasTextures();
                                boolean rootHasTextures = rootLeveledRegion != null && rootLeveledRegion.hasTextures();
                                if (hasTextures || rootHasTextures) {
                                    for(int o = 0; o < 8; o += 1) {
                                        int textureX = minXBlocks + o * textureSize;
                                        if (!((double)textureX > rightBorder) && !((double)(textureX + textureSize) < leftBorder)) {
                                            for(int p = 0; p < 8; p += 1) {
                                                int textureZ = minZBlocks + p * textureSize;
                                                if (!((double)textureZ > bottomBorder) && !((double)(textureZ + textureSize) < topBorder)) {
                                                    RegionTexture<?> regionTexture = hasTextures ? leveledRegion.getTexture(o, p) : null;
                                                    if (regionTexture != null && regionTexture.getGlColorTexture() != -1) {
                                                        int texture = regionTexture.getGlColorTexture();
                                                        if (texture != -1) {
                                                            boolean hasLight = regionTexture.getTextureHasLight();
                                                            renderTexturedModalRectWithLighting3(
                                                                    matrix,
                                                                    (float)(textureX - flooredCameraX),
                                                                    (float)(textureZ - flooredCameraZ),
                                                                    (float)textureSize,
                                                                    (float)textureSize,
                                                                    texture,
                                                                    hasLight,
                                                                    hasLight ? withLightRenderer : noLightRenderer
                                                            );
                                                        }
                                                    } else if (rootHasTextures) {
                                                        int insideX = firstInsideTextureX + o;
                                                        int insideZ = firstInsideTextureZ + p;
                                                        int rootTextureX = firstRootTextureX + (insideX >> levelDiff);
                                                        int rootTextureZ = firstRootTextureZ + (insideZ >> levelDiff);
                                                        regionTexture = rootLeveledRegion.getTexture(rootTextureX, rootTextureZ);
                                                        if (regionTexture != null) {
                                                            int texture = regionTexture.getGlColorTexture();
                                                            if (texture != -1) {
                                                                frameRenderedRootTextures = true;
                                                                int insideTextureX = insideX & maxInsideCoord;
                                                                int insideTextureZ = insideZ & maxInsideCoord;
                                                                float textureX1 = (float)insideTextureX / (float)rootSize;
                                                                float textureX2 = (float)(insideTextureX + 1) / (float)rootSize;
                                                                float textureY1 = (float)insideTextureZ / (float)rootSize;
                                                                float textureY2 = (float)(insideTextureZ + 1) / (float)rootSize;
                                                                boolean hasLight = regionTexture.getTextureHasLight();
                                                                renderTexturedModalSubRectWithLighting(
                                                                        matrix,
                                                                        (float)(textureX - flooredCameraX),
                                                                        (float)(textureZ - flooredCameraZ),
                                                                        textureX1,
                                                                        textureY1,
                                                                        textureX2,
                                                                        textureY2,
                                                                        (float)textureSize,
                                                                        (float)textureSize,
                                                                        texture,
                                                                        hasLight,
                                                                        hasLight ? withLightRenderer : noLightRenderer
                                                                );
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                if (leveledRegion.loadingAnimation()) {
                                    matrixStack.push();
                                    matrixStack.translate(
                                            (double)leveledSideInBlocks * ((double)leveledRegX + 0.5) - (double)flooredCameraX,
                                            (double)leveledSideInBlocks * ((double)leveledRegZ + 0.5) - (double)flooredCameraZ,
                                            0.0
                                    );
                                    float loadingAnimationPassed = (float)(System.currentTimeMillis() - this.loadingAnimationStart);
                                    if (loadingAnimationPassed > 0.0F) {
                                        int period = 2000;
                                        int numbersOfActors = 3;
                                        float loadingAnimation = loadingAnimationPassed % (float)period / (float)period * 360.0F;
                                        float step = 360.0F / (float)numbersOfActors;
                                        OptimizedMath.rotatePose(matrixStack, loadingAnimation, OptimizedMath.ZP);
                                        int numberOfVisibleActors = 1 + (int)loadingAnimationPassed % (3 * period) / period;
                                        matrixStack.scale((float)leveledSideInRegions, (float)leveledSideInRegions, 1.0F);

                                        for(int i = 0; i < numberOfVisibleActors; i += 1) {
                                            OptimizedMath.rotatePose(matrixStack, step, OptimizedMath.ZP);
                                            MapRenderHelper.fillIntoExistingBuffer(
                                                    matrixStack.peek().getPositionMatrix(), overlayBuffer, 16, -8, 32, 8, 1.0F, 1.0F, 1.0F, 1.0F
                                            );
                                        }
                                    }

                                    matrixStack.pop();
                                }

                                if (WorldMap.settings.debug && leveledRegion instanceof MapRegion region && !mc.options.hudHidden) {
                                    matrixStack.push();
                                    matrixStack.translate(
                                            (float)(512 * region.getRegionX() + 32 - flooredCameraX), (float)(512 * region.getRegionZ() + 32 - flooredCameraZ), 0.0F
                                    );
                                    matrixStack.scale(10.0F, 10.0F, 1.0F);
                                    Misc.drawNormalText(matrixStack, region.getLoadState() + "", 0.0F, 0.0F, -1, true, renderTypeBuffers);
                                    matrixStack.pop();
                                }

                                if (WorldMap.settings.debug && textureLevel > 0 && !mc.options.hudHidden) {
                                    for(int leafX = 0; leafX < leveledSideInRegions; leafX += 1) {
                                        for(int leafZ = 0; leafZ < leveledSideInRegions; leafZ += 1) {
                                            int regX = leafRegionMinX + leafX;
                                            int regZ = leafRegionMinZ + leafZ;
                                            MapRegion region = this.mapProcessor.getMapRegion(renderedCaveLayer, regX, regZ, false);
                                            if (region != null) {
                                                boolean currentlyLoading = this.mapProcessor.getMapSaveLoad().getNextToLoadByViewing() == region;
                                                if (currentlyLoading || region.isLoaded() || region.isMetaLoaded()) {
                                                    matrixStack.push();
                                                    matrixStack.translate(
                                                            (float)(512 * region.getRegionX() - flooredCameraX), (float)(512 * region.getRegionZ() - flooredCameraZ), 0.0F
                                                    );
                                                    float r = 0.0F;
                                                    float g = 0.0F;
                                                    float b = 0.0F;
                                                    float a = 0.1569F;
                                                    if (currentlyLoading) {
                                                        b = 1.0F;
                                                        r = 1.0F;
                                                    } else if (region.isLoaded()) {
                                                        g = 1.0F;
                                                    } else {
                                                        g = 1.0F;
                                                        r = 1.0F;
                                                    }

                                                    MapRenderHelper.fillIntoExistingBuffer(
                                                            matrixStack.peek().getPositionMatrix(), overlayBuffer, 0, 0, 512, 512, r, g, b, a
                                                    );
                                                    matrixStack.pop();
                                                }
                                            }
                                        }
                                    }
                                }
                                if (!mc.options.hudHidden)
                                    Globals.drawManager.drawWorldMapFeatures(leafRegionMinX, leafRegionMinZ, leveledSideInRegions, flooredCameraX, flooredCameraZ, matrixStack, overlayBuffer);
                            }
                        }
                    }

                    this.lastFrameRenderedRootTextures = frameRenderedRootTextures;
                    MapShaders.WORLD_MAP.setBrightness(brightness);
                    MapShaders.WORLD_MAP.setWithLight(true);
                    rendererProvider.draw(withLightRenderer);
                    MapShaders.WORLD_MAP.setWithLight(false);
                    rendererProvider.draw(noLightRenderer);
                    LeveledRegion<?> nextToLoad = this.mapProcessor.getMapSaveLoad().getNextToLoadByViewing();
                    boolean shouldRequest = false;
                    if (nextToLoad != null) {
                        shouldRequest = nextToLoad.shouldAllowAnotherRegionToLoad();
                    } else {
                        shouldRequest = true;
                    }

                    /** Inc frequency limit **/
                    shouldRequest = shouldRequest && this.mapProcessor.getAffectingLoadingFrequencyCount() < 128;
                    if (shouldRequest && !WorldMap.settings.pauseRequests) {
                        /** Modify: inc toRequest limit **/
                        int toRequest = 50;
                        int counter = 0;

                        for(int i = 0; i < this.branchRegionBuffer.size() && counter < toRequest; ++i) {
                            BranchLeveledRegion region = this.branchRegionBuffer.get(i);
                            if (!region.reloadHasBeenRequested() && !region.recacheHasBeenRequested() && !region.isLoaded()) {
                                region.setReloadHasBeenRequested(true, "Gui");
                                this.mapProcessor.getMapSaveLoad().requestBranchCache(region, "Gui");
                                if (counter == 0) {
                                    this.mapProcessor.getMapSaveLoad().setNextToLoadByViewing(region);
                                }

                                ++counter;
                            }
                        }

                        int var210 = 10; // increase limit
                        counter = 0;
                        if (!prevWaitingForBranchCache) {
                            for(int i = 0; i < this.regionBuffer.size() && counter < var210; ++i) {
                                MapRegion region = this.regionBuffer.get(i);
                                if (region != nextToLoad || this.regionBuffer.size() <= 1) {
                                    synchronized(region) {
                                        if (region.canRequestReload_unsynced()) {
                                            if (region.getLoadState() == 2) {
                                                region.requestRefresh(this.mapProcessor);
                                            } else {
                                                this.mapProcessor.getMapSaveLoad().requestLoad(region, "Gui");
                                            }

                                            if (counter == 0) {
                                                this.mapProcessor.getMapSaveLoad().setNextToLoadByViewing(region);
                                            }

                                            ++counter;
                                            if (region.getLoadState() == 4) {
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    this.prevWaitingForBranchCache = this.waitingForBranchCache[0];
                    this.prevLoadingLeaves = loadingLeaves;
                    int highlightChunkX = this.mouseBlockPosX >> 4;
                    int highlightChunkZ = this.mouseBlockPosZ >> 4;
                    int chunkHighlightLeftX = highlightChunkX << 4;
                    int chunkHighlightRightX = highlightChunkX + 1 << 4;
                    int chunkHighlightTopZ = highlightChunkZ << 4;
                    int chunkHighlightBottomZ = highlightChunkZ + 1 << 4;
                    if (!mc.options.hudHidden) {
                        MapRenderHelper.renderDynamicHighlight(
                                matrixStack,
                                overlayBuffer,
                                flooredCameraX,
                                flooredCameraZ,
                                chunkHighlightLeftX,
                                chunkHighlightRightX,
                                chunkHighlightTopZ,
                                chunkHighlightBottomZ,
                                0.0F,
                                0.0F,
                                0.0F,
                                0.2F,
                                1.0F,
                                1.0F,
                                1.0F,
                                0.1569F
                        );
                        MapTileSelection mapTileSelectionToRender = this.mapTileSelection;
                        if (mapTileSelectionToRender == null && this.client.currentScreen instanceof ExportScreen) {
                            mapTileSelectionToRender = ((ExportScreen)this.client.currentScreen).getSelection();
                        }

                        if (mapTileSelectionToRender != null) {
                            MapRenderHelper.renderDynamicHighlight(
                                    matrixStack,
                                    overlayBuffer,
                                    flooredCameraX,
                                    flooredCameraZ,
                                    mapTileSelectionToRender.getLeft() << 4,
                                    mapTileSelectionToRender.getRight() + 1 << 4,
                                    mapTileSelectionToRender.getTop() << 4,
                                    mapTileSelectionToRender.getBottom() + 1 << 4,
                                    0.0F,
                                    0.0F,
                                    0.0F,
                                    0.2F,
                                    1.0F,
                                    0.5F,
                                    0.5F,
                                    0.4F
                            );
                            if (SupportMods.pac()) {
                                int playerX = (int)Math.floor(this.player.getX());
                                int playerZ = (int)Math.floor(this.player.getZ());
                                int playerChunkX = playerX >> 4;
                                int playerChunkZ = playerZ >> 4;
                                int claimDistance = SupportMods.xaeroPac.getClaimDistance();
                                int claimableAreaLeft = playerChunkX - claimDistance;
                                int claimableAreaTop = playerChunkZ - claimDistance;
                                int claimableAreaRight = playerChunkX + claimDistance;
                                int claimableAreaBottom = playerChunkZ + claimDistance;
                                int claimableAreaHighlightLeftX = claimableAreaLeft << 4;
                                int claimableAreaHighlightRightX = claimableAreaRight + 1 << 4;
                                int claimableAreaHighlightTopZ = claimableAreaTop << 4;
                                int claimableAreaHighlightBottomZ = claimableAreaBottom + 1 << 4;
                                MapRenderHelper.renderDynamicHighlight(
                                        matrixStack,
                                        overlayBuffer,
                                        flooredCameraX,
                                        flooredCameraZ,
                                        claimableAreaHighlightLeftX,
                                        claimableAreaHighlightRightX,
                                        claimableAreaHighlightTopZ,
                                        claimableAreaHighlightBottomZ,
                                        0.0F,
                                        0.0F,
                                        1.0F,
                                        0.3F,
                                        0.0F,
                                        0.0F,
                                        1.0F,
                                        0.15F
                                );
                            }
                        }
                    }

                    RenderSystem.disableCull();
                    renderTypeBuffers.draw();
                    RenderSystem.enableCull();
                    primaryScaleFBO.endWrite();
                    primaryScaleFBO.bindDefaultFramebuffer(mc);
                    matrixStack.pop();
                    matrixStack.push();
                    matrixStack.scale((float)secondaryScale, (float)secondaryScale, 1.0F);
                    primaryScaleFBO.beginRead();
                    GL11.glTexParameteri(3553, 10240, 9729);
                    GL11.glTexParameteri(3553, 10241, 9729);
                    RenderSystem.depthMask(false);
                    VertexConsumer colorBackgroundConsumer = renderTypeBuffers.getBuffer(CustomRenderTypes.MAP_COLOR_FILLER);
                    int lineX = -mc.getWindow().getFramebufferWidth() / 2;
                    int lineY = mc.getWindow().getFramebufferHeight() / 2 - 5;
                    int lineW = mc.getWindow().getFramebufferWidth();
                    int lineH = 6;
                    MapRenderHelper.fillIntoExistingBuffer(
                            matrixStack.peek().getPositionMatrix(), colorBackgroundConsumer, lineX, lineY, lineX + lineW, lineY + lineH, 0.0F, 0.0F, 0.0F, 1.0F
                    );
                    lineX = mc.getWindow().getFramebufferWidth() / 2 - 5;
                    lineY = -mc.getWindow().getFramebufferHeight() / 2;
                    int var240 = 6;
                    lineH = mc.getWindow().getFramebufferHeight();
                    MapRenderHelper.fillIntoExistingBuffer(
                            matrixStack.peek().getPositionMatrix(), colorBackgroundConsumer, lineX, lineY, lineX + var240, lineY + lineH, 0.0F, 0.0F, 0.0F, 1.0F
                    );
                    renderTypeBuffers.draw();
                    RenderLayer mainFrameRenderType = CustomRenderTypes.GUI_BILINEAR;
                    if (SupportMods.vivecraft) {
                        mainFrameRenderType = CustomRenderTypes.MAP_FRAME_TEXTURE_OVER_TRANSPARENT;
                    }

                    MultiTextureRenderTypeRenderer mainFrameRenderer = rendererProvider.getRenderer(
                            t -> RenderSystem.setShaderTexture(0, t), MultiTextureRenderTypeRendererProvider::defaultTextureBind, mainFrameRenderType
                    );
                    VertexConsumer mainFrameVertexConsumer = mainFrameRenderer.begin(
                            VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR_TEXTURE, primaryScaleFBO.getFramebufferTexture()
                    );
                    renderTexturedModalRect(
                            matrixStack.peek().getPositionMatrix(),
                            mainFrameVertexConsumer,
                            (float)(-mc.getWindow().getFramebufferWidth() / 2) - (float)secondaryOffsetX,
                            (float)(-mc.getWindow().getFramebufferHeight() / 2) - (float)secondaryOffsetY,
                            0,
                            0,
                            (float)primaryScaleFBO.viewportWidth,
                            (float)primaryScaleFBO.viewportHeight,
                            (float)primaryScaleFBO.viewportWidth,
                            (float)primaryScaleFBO.viewportHeight,
                            1.0F,
                            1.0F,
                            1.0F,
                            1.0F
                    );
                    rendererProvider.draw(mainFrameRenderer);
                    RenderSystem.depthMask(true);
                    matrixStack.pop();
                    matrixStack.scale((float)this.scale, (float)this.scale, 1.0F);
                    double screenSizeBasedScale = scaleMultiplier;
                    WorldMap.trackedPlayerRenderer.update(mc);

                    try {
                        if (!mc.options.hudHidden) {
                            this.viewed = WorldMap.mapElementRenderHandler
                                    .render(
                                            (GuiMap) (Object) this,
                                            guiGraphics,
                                            renderTypeBuffers,
                                            rendererProvider,
                                            this.cameraX,
                                            this.cameraZ,
                                            mc.getWindow().getFramebufferWidth(),
                                            mc.getWindow().getFramebufferHeight(),
                                            screenSizeBasedScale,
                                            this.scale,
                                            mousePosX,
                                            mousePosZ,
                                            brightness,
                                            renderedCaveLayer != Integer.MAX_VALUE,
                                            this.viewed,
                                            mc,
                                            partialTicks
                                    );
                        }
                    } catch (Throwable var160) {
                        WorldMap.LOGGER.error("error rendering map elements", var160);
                        throw var160;
                    }

                    this.viewedInList = false;
                    matrixStack.push();
                    matrixStack.translate(0.0F, 0.0F, 970.0F);
                    VertexConsumer regularUIObjectConsumer = renderTypeBuffers.getBuffer(CustomRenderTypes.GUI_BILINEAR);
                    if (WorldMap.settings.footsteps && !mc.options.hudHidden && !isDimensionSwitched) {
                        ArrayList<Double[]> footprints = this.mapProcessor.getFootprints();
                        synchronized(footprints) {
                            for(int i = 0; i < footprints.size(); i += 1) {
                                Double[] coords = (Double[])footprints.get(i);
                                this.setColourBuffer(1.0F, 0.1F, 0.1F, 1.0F);
                                this.drawDotOnMap(matrixStack, regularUIObjectConsumer, coords[0] - this.cameraX, coords[1] - this.cameraZ, 0.0F, 1.0 / this.scale);
                            }
                        }
                    }

                    if (WorldMap.settings.renderArrow && !mc.options.hudHidden) {
                        boolean toTheLeft = getPlayerX() < leftBorder;
                        boolean toTheRight = getPlayerX() > rightBorder;
                        boolean down = getPlayerZ() > bottomBorder;
                        boolean up = getPlayerZ() < topBorder;
                        float configuredR = 1.0F;
                        float configuredG = 1.0F;
                        float configuredB = 1.0F;
                        int effectiveArrowColorIndex = WorldMap.settings.arrowColour;
                        if (effectiveArrowColorIndex == -2 && !SupportMods.minimap()) {
                            effectiveArrowColorIndex = 0;
                        }

                        if (effectiveArrowColorIndex == -2 && SupportMods.xaeroMinimap.getArrowColorIndex() == -1) {
                            effectiveArrowColorIndex = -1;
                        }

                        if (effectiveArrowColorIndex == -1) {
                            int rgb = Misc.getTeamColour(mc.player == null ? mc.getCameraEntity() : mc.player);
                            if (rgb == -1) {
                                effectiveArrowColorIndex = 0;
                            } else {
                                configuredR = (float)(rgb >> 16 & 0xFF) / 255.0F;
                                configuredG = (float)(rgb >> 8 & 0xFF) / 255.0F;
                                configuredB = (float)(rgb & 0xFF) / 255.0F;
                            }
                        } else if (effectiveArrowColorIndex == -2) {
                            float[] c = SupportMods.xaeroMinimap.getArrowColor();
                            if (c == null) {
                                effectiveArrowColorIndex = 0;
                            } else {
                                configuredR = c[0];
                                configuredG = c[1];
                                configuredB = c[2];
                            }
                        }

                        if (effectiveArrowColorIndex >= 0) {
                            float[] c = ModSettings.arrowColours[effectiveArrowColorIndex];
                            configuredR = c[0];
                            configuredG = c[1];
                            configuredB = c[2];
                        }

                        if (!toTheLeft && !toTheRight && !up && !down) {
                            this.setColourBuffer(0.0F, 0.0F, 0.0F, 0.9F);
                            this.drawArrowOnMap(
                                    matrixStack,
                                    regularUIObjectConsumer,
                                    getPlayerX() - this.cameraX,
                                    getPlayerZ() + 2.0 * scaleMultiplier / this.scale - this.cameraZ,
                                    this.player.getYaw(),
                                    scaleMultiplier / this.scale
                            );
                            this.setColourBuffer(configuredR, configuredG, configuredB, 1.0F);
                            this.drawArrowOnMap(
                                    matrixStack,
                                    regularUIObjectConsumer,
                                    getPlayerX() - this.cameraX,
                                    getPlayerZ() - this.cameraZ,
                                    this.player.getYaw(),
                                    scaleMultiplier / this.scale
                            );
                        } else {
                            double arrowX = getPlayerX();
                            double arrowZ = getPlayerZ();
                            float a = 0.0F;
                            if (toTheLeft) {
                                a = up ? 1.5F : (down ? 0.5F : 1.0F);
                                arrowX = leftBorder;
                            } else if (toTheRight) {
                                a = up ? 2.5F : (down ? 3.5F : 3.0F);
                                arrowX = rightBorder;
                            }

                            if (down) {
                                arrowZ = bottomBorder;
                            } else if (up) {
                                if (a == 0.0F) {
                                    a = 2.0F;
                                }

                                arrowZ = topBorder;
                            }

                            this.setColourBuffer(0.0F, 0.0F, 0.0F, 0.9F);
                            this.drawFarArrowOnMap(
                                    matrixStack,
                                    regularUIObjectConsumer,
                                    arrowX - this.cameraX,
                                    arrowZ + 2.0 * scaleMultiplier / this.scale - this.cameraZ,
                                    a,
                                    scaleMultiplier / this.scale
                            );
                            this.setColourBuffer(configuredR, configuredG, configuredB, 1.0F);
                            this.drawFarArrowOnMap(
                                    matrixStack, regularUIObjectConsumer, arrowX - this.cameraX, arrowZ - this.cameraZ, a, scaleMultiplier / this.scale
                            );
                        }
                    }

                    if (XaeroPlusSettingRegistry.showRenderDistanceWorldMapSetting.getValue() && !mc.options.hudHidden) {
                        if (MinecraftClient.getInstance().world.getRegistryKey() == Globals.customDimensionId) {
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
                            MinimapShaders.FRAMEBUFFER_LINES.setFrameSize(mc.getWindow().getFramebufferWidth(), mc.getWindow().getFramebufferHeight());

                            float settingWidth = (float) XaeroMinimapSession.getCurrentSession().getModMain().getSettings().chunkGridLineWidth;
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

                    this.client.getTextureManager().bindTexture(WorldMap.guiTextures);
                    GL11.glTexParameteri(3553, 10240, 9729);
                    GL11.glTexParameteri(3553, 10241, 9729);
                    renderTypeBuffers.draw();
                    this.client.getTextureManager().bindTexture(WorldMap.guiTextures);
                    GL11.glTexParameteri(3553, 10240, 9728);
                    GL11.glTexParameteri(3553, 10241, 9728);
                    matrixStack.pop();
                    matrixStack.pop();
                    VertexConsumer backgroundVertexBuffer = renderTypeBuffers.getBuffer(CustomRenderTypes.MAP_COLOR_OVERLAY);
                    int cursorDisplayOffset = 0;
                    if (WorldMap.settings.coordinates && !mc.options.hudHidden) {
                        String coordsString = "X: " + this.mouseBlockPosX;
                        if (mouseBlockBottomY != 32767) {
                            coordsString = coordsString + " Y: " + mouseBlockBottomY;
                        }

                        if (hasAmbiguousHeight && mouseBlockTopY != 32767) {
                            coordsString = coordsString + " (" + mouseBlockTopY + ")";
                        }

                        coordsString = coordsString + " Z: " + this.mouseBlockPosZ;
                        MapRenderHelper.drawCenteredStringWithBackground(
                                guiGraphics, this.textRenderer, coordsString, this.width / 2, 2 + cursorDisplayOffset, -1, 0.0F, 0.0F, 0.0F, 0.4F, backgroundVertexBuffer
                        );
                        cursorDisplayOffset += 10;
                    }

                    if (WorldMap.settings.hoveredBiome && pointedAtBiome != null && !mc.options.hudHidden) {
                        Identifier biomeRL = pointedAtBiome.getValue();
                        String biomeText = biomeRL == null
                                ? I18n.translate("gui.xaero_wm_unknown_biome", new Object[0])
                                : I18n.translate("biome." + biomeRL.getNamespace() + "." + biomeRL.getPath(), new Object[0]);
                        MapRenderHelper.drawCenteredStringWithBackground(
                                guiGraphics, this.textRenderer, biomeText, this.width / 2, 2 + cursorDisplayOffset, -1, 0.0F, 0.0F, 0.0F, 0.4F, backgroundVertexBuffer
                        );
                    }

                    int subtleTooltipOffset = 12;
                    if (WorldMap.settings.displayZoom && !mc.options.hudHidden) {
                        String zoomString = (double)Math.round(destScale * 1000.0) / 1000.0 + "x";
                        MapRenderHelper.drawCenteredStringWithBackground(
                                guiGraphics,
                                mc.textRenderer,
                                zoomString,
                                this.width / 2,
                                this.height - subtleTooltipOffset,
                                -1,
                                0.0F,
                                0.0F,
                                0.0F,
                                0.4F,
                                backgroundVertexBuffer
                        );
                    }

                    if (this.dimension.getFullReloader() != null && !mc.options.hudHidden) {
                        subtleTooltipOffset += 12;
                        MapRenderHelper.drawCenteredStringWithBackground(
                            guiGraphics,
                            mc.textRenderer,
                            FULL_RELOAD_IN_PROGRESS,
                            this.width / 2,
                            this.height - subtleTooltipOffset,
                            -1,
                            0.0F,
                            0.0F,
                            0.0F,
                            0.4F,
                            backgroundVertexBuffer
                        );
                    }

                    if (WorldMap.settings.displayCaveModeStart && !mc.options.hudHidden) {
                        subtleTooltipOffset += 12;
                        if (globalCaveStart != Integer.MAX_VALUE && globalCaveStart != Integer.MIN_VALUE) {
                            String caveModeStartString = I18n.translate("gui.xaero_wm_cave_mode_start_display", new Object[]{globalCaveStart});
                            MapRenderHelper.drawCenteredStringWithBackground(
                                    guiGraphics,
                                    mc.textRenderer,
                                    caveModeStartString,
                                    this.width / 2,
                                    this.height - subtleTooltipOffset,
                                    -1,
                                    0.0F,
                                    0.0F,
                                    0.0F,
                                    0.4F,
                                    backgroundVertexBuffer
                            );
                        }
                    }

                    if (SupportMods.minimap() && !mc.options.hudHidden) {
                        String subWorldNameToRender = SupportMods.xaeroMinimap.getSubWorldNameToRender();
                        if (subWorldNameToRender != null) {
                            subtleTooltipOffset += 24;
                            MapRenderHelper.drawCenteredStringWithBackground(
                                    guiGraphics,
                                    mc.textRenderer,
                                    subWorldNameToRender,
                                    this.width / 2,
                                    this.height - subtleTooltipOffset,
                                    -1,
                                    0.0F,
                                    0.0F,
                                    0.0F,
                                    0.4F,
                                    backgroundVertexBuffer
                            );
                        }
                    }

                    discoveredForHighlights = mouseBlockBottomY != 32767;
                    Text subtleHighlightTooltip = this.mapProcessor
                            .getMapWorld()
                            .getDimension(Globals.customDimensionId)
                            .getHighlightHandler()
                            .getBlockHighlightSubtleTooltip(this.mouseBlockPosX, this.mouseBlockPosZ, discoveredForHighlights);
                    if (subtleHighlightTooltip != null && !mc.options.hudHidden) {
                        subtleTooltipOffset += 12;
                        MapRenderHelper.drawCenteredStringWithBackground(
                                guiGraphics,
                                mc.textRenderer,
                                subtleHighlightTooltip,
                                this.width / 2,
                                this.height - subtleTooltipOffset,
                                -1,
                                0.0F,
                                0.0F,
                                0.0F,
                                0.4F,
                                backgroundVertexBuffer
                        );
                    }

                    renderTypeBuffers.draw();
                    this.overWaypointsMenu = false;
                    this.overPlayersMenu = false;
                    if (this.waypointMenu || this.playersMenu) {
                        matrixStack.push();
                        matrixStack.translate(0.0F, 0.0F, 972.0F);
                    }

                    if (this.waypointMenu) {
                        if (SupportMods.xaeroMinimap.getWaypointsSorted() != null) {
                            HoveredMapElementHolder<?, ?> hovered = SupportMods.xaeroMinimap
                                    .renderWaypointsMenu(
                                            guiGraphics,
                                            (GuiMap) (Object) this,
                                            this.scale,
                                            this.width,
                                            this.height,
                                            scaledMouseX,
                                            scaledMouseY,
                                            this.leftMouseButton.isDown,
                                            this.leftMouseButton.clicked,
                                            this.viewed,
                                            mc
                                    );
                            if (hovered != null) {
                                this.overWaypointsMenu = true;
                                if (hovered.getElement() instanceof Waypoint) {
                                    this.viewed = hovered;
                                    this.viewedInList = true;
                                    if (this.leftMouseButton.clicked) {
                                        this.cameraDestination = new int[]{
                                                (int)((Waypoint)this.viewed.getElement()).getRenderX(), (int)((Waypoint)this.viewed.getElement()).getRenderZ()
                                        };
                                        this.leftMouseButton.isDown = false;
                                        if (WorldMap.settings.closeWaypointsWhenHopping) {
                                            this.onWaypointsButton(this.waypointsButton);
                                        }
                                    }
                                }
                            }
                        }
                    } else if (this.playersMenu) {
                        HoveredMapElementHolder<?, ?> hovered = WorldMap.trackedPlayerMenuRenderer
                                .renderMenu(
                                        guiGraphics,
                                        (GuiMap) (Object) this,
                                        this.scale,
                                        this.width,
                                        this.height,
                                        scaledMouseX,
                                        scaledMouseY,
                                        this.leftMouseButton.isDown,
                                        this.leftMouseButton.clicked,
                                        this.viewed,
                                        mc
                                );
                        if (hovered != null) {
                            this.overPlayersMenu = true;
                            if (hovered.getElement() instanceof PlayerTrackerMapElement) {
                                this.viewed = hovered;
                                this.viewedInList = true;
                                if (this.leftMouseButton.clicked) {
                                    this.cameraDestination = new int[]{
                                            (int)((PlayerTrackerMapElement)this.viewed.getElement()).getX(), (int)((PlayerTrackerMapElement)this.viewed.getElement()).getZ()
                                    };
                                    this.leftMouseButton.isDown = false;
                                }
                            }
                        }
                    }

                    if (this.waypointMenu || this.playersMenu) {
                        matrixStack.pop();
                    }

                    if (SupportMods.minimap()) {
                        SupportMods.xaeroMinimap.drawSetChange(guiGraphics);
                    }

                    if (SupportMods.pac()) {
                        SupportMods.xaeroPac
                                .onMapRender(
                                        this.client,
                                        matrixStack,
                                        scaledMouseX,
                                        scaledMouseY,
                                        partialTicks,
                                        this.mapProcessor.getWorld().getRegistryKey().getValue(),
                                        highlightChunkX,
                                        highlightChunkZ
                                );
                    }
                } else if (!mapLoaded) {
                    this.renderLoadingScreen(guiGraphics);
                } else if (isLocked) {
                    this.renderMessageScreen(
                            guiGraphics, I18n.translate("gui.xaero_current_map_locked1", new Object[0]), I18n.translate("gui.xaero_current_map_locked2", new Object[0])
                    );
                } else if (noWorldMapEffect) {
                    this.renderMessageScreen(guiGraphics, I18n.translate("gui.xaero_no_world_map_message", new Object[0]));
                } else if (!allowedBasedOnItem) {
                    this.renderMessageScreen(
                            guiGraphics,
                            I18n.translate("gui.xaero_no_world_map_item_message", new Object[0]),
                            ModSettings.mapItem.getName().getString() + " (" + ModSettings.mapItemId + ")"
                    );
                }
            }

            this.mapSwitchingGui.renderText(guiGraphics, this.client, scaledMouseX, scaledMouseY, this.width, this.height);
            if (!mc.options.hudHidden) {
                guiGraphics.drawTexture(WorldMap.guiTextures, this.width - 34, 2, 0, 37, 32, 32);
            }
        }

        matrixStack.push();
        matrixStack.translate(0.0F, 0.0F, 973.0F);
        if (mc.options.hudHidden) {
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
        super.render(guiGraphics, scaledMouseX, scaledMouseY, partialTicks);
        if (this.rightClickMenu != null) {
            this.rightClickMenu.render(guiGraphics, scaledMouseX, scaledMouseY, partialTicks);
        }

        matrixStack.translate(0.0F, 0.0F, 10.0F);
        if (mc.currentScreen == this) {
            if (!this.renderTooltips(guiGraphics, scaledMouseX, scaledMouseY, partialTicks) && !this.leftMouseButton.isDown && !this.rightMouseButton.isDown) {
                if (this.viewed != null) {
                    CursorBox hoveredTooltip = this.hoveredElementTooltipHelper(this.viewed, this.viewedInList);
                    if (hoveredTooltip != null) {
                        hoveredTooltip.drawBox(guiGraphics, scaledMouseX, scaledMouseY, this.width, this.height);
                    }
                } else {
                    synchronized(this.mapProcessor.renderThreadPauseSync) {
                        if (!this.mapProcessor.isRenderingPaused()
                                && this.mapProcessor.getCurrentWorldId() != null
                                && this.mapProcessor.getMapSaveLoad().isRegionDetectionComplete()) {
                            Text bluntHighlightTooltip = this.mapProcessor
                                    .getMapWorld()
                                    .getDimension(Globals.customDimensionId)
                                    .getHighlightHandler()
                                    .getBlockHighlightBluntTooltip(this.mouseBlockPosX, this.mouseBlockPosZ, discoveredForHighlights);
                            if (bluntHighlightTooltip != null) {
                                new CursorBox(bluntHighlightTooltip).drawBox(guiGraphics, scaledMouseX, scaledMouseY, this.width, this.height);
                            }
                        }
                    }
                }
            }

            matrixStack.translate(0.0F, 0.0F, 1.0F);
            this.mapProcessor.getMessageBoxRenderer().render(guiGraphics, this.mapProcessor.getMessageBox(), this.textRenderer, 1, this.height / 2, false);
        }

        matrixStack.pop();
        this.leftMouseButton.clicked = this.rightMouseButton.clicked = false;
        this.noUploadingLimits = this.cameraX == cameraXBefore && this.cameraZ == cameraZBefore && scaleBefore == this.scale;
        // insert Coordinates goto button
        if (mc.currentScreen != null && mc.currentScreen.getClass().equals(GuiMap.class) && xTextEntryField.isVisible() && zTextEntryField.isVisible()) {
            xTextEntryField.render(guiGraphics, scaledMouseX, scaledMouseY, partialTicks);
            zTextEntryField.render(guiGraphics, scaledMouseX, scaledMouseY, partialTicks);
        }
        MapRenderHelper.restoreDefaultShaderBlendState();
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
            RegistryKey<World> customDim = Globals.customDimensionId;
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
        if (Globals.customDimensionId != newDimId) {
            if (Globals.customDimensionId == NETHER) {
                this.cameraDestination = new int[] {(int) (cameraX * 8), (int) (cameraZ * 8)};
            } else if (newDimId == NETHER) {
                this.cameraDestination = new int[] {(int) (cameraX / 8), (int) (cameraZ / 8)};
            }
        }
        Globals.switchToDimension(newDimId);
        AccessorGuiCaveModeOptions options = (AccessorGuiCaveModeOptions) caveModeOptions;
        MapDimension newDimension = mapProcessor.getMapWorld().getDimension(Globals.customDimensionId);
        options.setDimension(newDimension);
        for (ButtonWidget button : getButtonList()) {
            if (!(button instanceof TooltipButton)) continue;
            final TooltipButton tooltipButton = (TooltipButton) button;
            final Supplier<CursorBox> xaeroWmTooltipSupplier = tooltipButton.getXaero_wm_tooltip();
            if (xaeroWmTooltipSupplier == null) continue;
            final CursorBox cursorBox = xaeroWmTooltipSupplier.get();
            if (cursorBox == null) continue;
            final String code = cursorBox.getFullCode();
            if (Objects.equals(code, "gui.xaero_wm_box_cave_mode_type")) {
                button.setMessage(options.invokeCaveModeTypeButtonMessage());
                break;
            }
        }
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
