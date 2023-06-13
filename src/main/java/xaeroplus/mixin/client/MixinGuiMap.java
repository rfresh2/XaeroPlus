package xaeroplus.mixin.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import xaero.map.MapProcessor;
import xaero.map.animation.Animation;
import xaero.map.animation.SlowingAnimation;
import xaero.map.element.HoveredMapElementHolder;
import xaero.map.graphics.ImprovedFramebuffer;
import xaero.map.gui.*;
import xaero.map.gui.dropdown.rightclick.GuiRightClickMenu;
import xaero.map.region.BranchLeveledRegion;
import xaero.map.region.MapRegion;
import xaero.map.world.MapDimension;
import xaeroplus.settings.XaeroPlusSettingRegistry;

import java.util.ArrayList;

@Mixin(value = GuiMap.class, remap = false)
public abstract class MixinGuiMap extends ScreenBase implements IRightClickableElement {
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
    public boolean waypointMenu = false;
    public boolean playersMenu = false;
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
    private GuiMapSwitching dimensionSettings;
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
}
