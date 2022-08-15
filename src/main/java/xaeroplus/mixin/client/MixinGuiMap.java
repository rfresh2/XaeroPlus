package xaeroplus.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.map.MapProcessor;
import xaero.map.WorldMap;
import xaero.map.animation.Animation;
import xaero.map.animation.SinAnimation;
import xaero.map.animation.SlowingAnimation;
import xaero.map.controls.ControlsHandler;
import xaero.map.controls.ControlsRegister;
import xaero.map.effects.Effects;
import xaero.map.element.HoveredMapElementHolder;
import xaero.map.graphics.ImprovedFramebuffer;
import xaero.map.graphics.MapRenderHelper;
import xaero.map.gui.*;
import xaero.map.misc.Misc;
import xaero.map.mods.SupportMods;
import xaero.map.mods.gui.Waypoint;
import xaero.map.region.*;
import xaero.map.region.texture.RegionTexture;
import xaero.map.settings.ModSettings;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static xaeroplus.XaeroPlus.MAX_LEVEL;

@Mixin(value = GuiMap.class, remap = false)
public abstract class MixinGuiMap extends ScreenBase implements IRightClickableElement {

    // todo: make this gui a popout widget like waypoints
    GuiButton coordinateGotoButton;
    GuiTextField xTextEntryField;
    GuiTextField zTextEntryField;

    protected MixinGuiMap(GuiScreen parent, GuiScreen escape) {
        super(parent, escape);
    }

    @Shadow
    private static int lastAmountOfRegionsViewed;
    @Shadow
    private long loadingAnimationStart;
    @Shadow
    private Entity player;
    @Shadow
    private int screenScale;
    @Shadow
    private int mouseDownPosX;
    @Shadow
    private int mouseDownPosY;
    @Shadow
    private double mouseDownCameraX;
    @Shadow
    private double mouseDownCameraZ;
    @Shadow
    private int mouseCheckPosX;
    @Shadow
    private int mouseCheckPosY;
    @Shadow
    private long mouseCheckTimeNano;
    @Shadow
    private int prevMouseCheckPosX;
    @Shadow
    private int prevMouseCheckPosY;
    @Shadow
    private long prevMouseCheckTimeNano;
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
    private double userScale;
    @Shadow
    private static double destScale;
    @Shadow
    private boolean pauseZoomKeys;
    @Shadow
    private int lastZoomMethod;
    @Shadow
    private HoveredMapElementHolder<?, ?> viewed;
    @Shadow
    private boolean viewedInList;
    @Shadow
    private boolean overWaypointsMenu;
    @Shadow
    private Animation zoomAnim;
    @Shadow
    public boolean waypointMenu;
    @Shadow
    private static ImprovedFramebuffer primaryScaleFBO;
    @Shadow
    private ArrayList<MapRegion> regionBuffer;
    @Shadow
    private ArrayList<BranchLeveledRegion> branchRegionBuffer;
    @Shadow
    private boolean prevWaitingForBranchCache = true;
    @Shadow
    private boolean prevLoadingLeaves = true;
    @Shadow
    private Integer lastViewedDimensionId;
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
    @Shadow
    private List<GuiDropDown> dropdowns;
    @Shadow
    private List<GuiDropDown> dropdownsToRemove;
    @Shadow
    private GuiMapSwitching dimensionSettings;
    @Shadow
    private MapMouseButtonPress leftMouseButton;
    @Shadow
    private MapMouseButtonPress rightMouseButton;
    @Shadow
    private MapProcessor mapProcessor;
    @Shadow
    private ScaledResolution scaledresolution;
    @Shadow
    public boolean noUploadingLimits;
    @Shadow
    private boolean[] waitingForBranchCache;
    @Shadow
    private GuiButton waypointsButton;
    @Shadow
    private GuiButton zoomInButton;
    @Shadow
    private GuiButton zoomOutButton;
    @Shadow
    private GuiButton buttonPressed;
    @Shadow
    private GuiRightClickMenu rightClickMenu;
    @Shadow
    private boolean lastFrameRenderedRootTextures;
    @Shadow
    private MapTileSelection mapTileSelection;
    @Shadow
    public abstract void setFocused(GuiTextField field);
    @Shadow
    protected abstract void closeDropdowns();
    @Shadow
    protected abstract double getScaleMultiplier(int screenShortSide);
    @Shadow
    private static long bytesToMb(long bytes) {
        return bytes / 1024L / 1024L;
    }
    @Shadow
    protected abstract void setColourBuffer(float r, float g, float b, float a);
    @Shadow
    public abstract void drawDotOnMap(double x, double z, float angle, double sc);
    @Shadow
    public abstract void drawFarArrowOnMap(double x, double z, float angle, double sc);
    @Shadow
    public abstract void drawArrowOnMap(double x, double z, float angle, double sc);
    @Shadow
    protected abstract void onWaypointsButton(GuiButton b);
    @Shadow
    protected abstract void renderLoadingScreen();
    @Shadow
    protected abstract void renderMessageScreen(String message);
    @Shadow
    protected abstract void renderMessageScreen(String message, String message2);
    @Shadow
    protected abstract <E, C> CursorBox hoveredElementTooltipHelper(HoveredMapElementHolder<E, C> hovered, boolean viewedInList);
    @Shadow
    public abstract void addGuiButton(GuiButton b);

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
        if (GuiMap.isCtrlKeyDown()) {
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
                    destScale = 1.0 / Math.pow(2.0, log2 + (double)(factor > 0.0 ? -1 : 1));
                }
            }
        } else {
            destScale *= Math.pow(1.2, factor);
        }

        if (destScale < 0.0625) {
            // remove min zoom
//            destScale = 0.0625;
        } else if (destScale > 50.0) {
            destScale = 50.0;
        }
    }

    @Inject(method = "initGui()V", at = @At(value = "TAIL"), remap = true)
    public void customInitGui(CallbackInfo ci) {
        int h = this.height / 2;
        coordinateGotoButton = new GuiTexturedButton(0, h + 40 , 20, 20, 229, 16, 16, 16, WorldMap.guiTextures, new Consumer<GuiButton>() {
            @Override
            public void accept(GuiButton guiButton) {
                onGotoCoordinatesButton(guiButton);
            }
        }, new CursorBox(new TextComponentString("Go to Coordinates")));
        addGuiButton(coordinateGotoButton);
        zTextEntryField = new GuiTextField(1, mc.fontRenderer, 2, h + 20, 50, 20);
        xTextEntryField = new GuiTextField(2, mc.fontRenderer, 2, h, 50, 20);
    }

    @Inject(method = "drawScreen(IIF)V", at = @At(value = "TAIL"), remap = true, cancellable = true)
    public void customDrawScreen(int scaledMouseX, int scaledMouseY, float partialTicks, CallbackInfo ci) {
        while (GL11.glGetError() != 0) {
        }
        this.dropdowns.removeAll(this.dropdownsToRemove);
        this.dropdownsToRemove.clear();
        Minecraft mc = Minecraft.getMinecraft();
        double cameraXBefore = this.cameraX;
        double cameraZBefore = this.cameraZ;
        double scaleBefore = this.scale;
        long startTime = System.currentTimeMillis();
        this.dimensionSettings.preMapRender((GuiMap)(Object)this, this.dropdowns, mc, this.width, this.height);
        long passed = this.lastStartTime == 0L ? 16L : startTime - this.lastStartTime;
        double passedScrolls = (float)passed / 64.0f;
        int direction = this.buttonPressed == this.zoomInButton || ControlsHandler.isDown(ControlsRegister.keyZoomIn) ? 1 : (this.buttonPressed == this.zoomOutButton || ControlsHandler.isDown(ControlsRegister.keyZoomOut) ? -1 : 0);
        if (direction != 0) {
            boolean ctrlKey = GuiMap.isCtrlKeyDown();
            if (!ctrlKey || !this.pauseZoomKeys) {
                this.changeZoom((double)direction * passedScrolls, this.buttonPressed == this.zoomInButton || this.buttonPressed == this.zoomOutButton ? 2 : 1);
                if (ctrlKey) {
                    this.pauseZoomKeys = true;
                }
            }
        } else {
            this.pauseZoomKeys = false;
        }
        this.lastStartTime = startTime;
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
        this.mouseBlockPosY = -1;
        boolean discoveredForHighlights = false;
        Object object = this.mapProcessor.renderThreadPauseSync;
        synchronized (object) {
            if (!this.mapProcessor.isRenderingPaused()) {
                boolean mapLoaded = this.mapProcessor.getCurrentWorldId() != null && this.mapProcessor.getMapSaveLoad().isRegionDetectionComplete();
                boolean noWorldMapEffect = mc.player.isPotionActive(Effects.NO_WORLD_MAP) || mc.player.isPotionActive(Effects.NO_WORLD_MAP_BENEFICIAL) || mc.player.isPotionActive(Effects.NO_WORLD_MAP_HARMFUL);
                boolean allowedBasedOnItem = ModSettings.mapItem == null || Misc.hasItem((EntityPlayer)mc.player, ModSettings.mapItem);
                boolean isLocked = this.mapProcessor.isCurrentMapLocked();
                if (mapLoaded && !noWorldMapEffect && allowedBasedOnItem && !isLocked) {
                    LeveledRegion<?> leveledRegion;
                    double secondaryOffsetY;
                    double secondaryOffsetX;
                    String subWorldNameToRender;
                    RegionTexture tex;
                    MapRegion leafRegion;
                    if (SupportMods.vivecraft) {
                        GlStateManager.clearColor((float)0.0f, (float)0.0f, (float)0.0f, (float)1.0f);
                        GL11.glClearColor((float)0.0f, (float)0.0f, (float)0.0f, (float)1.0f);
                        GlStateManager.clear((int)16384);
                    }
                    this.lastViewedDimensionId = this.mapProcessor.getMapWorld().getCurrentDimension().getDimId();
                    this.lastViewedMultiworldId = this.mapProcessor.getMapWorld().getCurrentDimension().getCurrentMultiworld();
                    if (SupportMods.minimap()) {
                        SupportMods.xaeroMinimap.checkWaypoints(this.mapProcessor.getMapWorld().isMultiplayer(), this.lastViewedDimensionId, this.lastViewedMultiworldId, this.width, this.height, (GuiMap) (Object)this);
                    }
                    int mouseXPos = (int)Misc.getMouseX(mc);
                    int mouseYPos = (int)Misc.getMouseY(mc);
                    double scaleMultiplier = this.getScaleMultiplier(Math.min(mc.displayWidth, mc.displayHeight));
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
                                double speed_z;
                                double frameTime60FPS;
                                double downTime = 0.0;
                                int draggedX = 0;
                                int draggedY = 0;
                                draggedX = mouseXPos - this.prevMouseCheckPosX;
                                downTime = System.nanoTime() - this.prevMouseCheckTimeNano;
                                double speedScale = downTime / (frameTime60FPS = 1.6666666666666666E7);
                                double speed_x = (double)(-draggedX) / this.scale / speedScale;
                                double speed = Math.sqrt(speed_x * speed_x + (speed_z = (double)(-(draggedY = mouseYPos - this.prevMouseCheckPosY)) / this.scale / speedScale) * speed_z);
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
                    int mouseFromCentreX = mouseXPos - mc.displayWidth / 2;
                    int mouseFromCentreY = mouseYPos - mc.displayHeight / 2;
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
                    double fboScale = this.scale >= 1.0 ? Math.max(1.0, Math.floor(this.scale)) : this.scale;
                    if (this.userScale < 1.0) {
                        double reversedScale = 1.0 / this.userScale;
                        double log2 = Math.floor(Math.log(reversedScale) / Math.log(2.0));
                        /** Use max cache level variable instead of hardcoded value **/
                        textureLevel = Math.min((int)log2, MAX_LEVEL);
                    }
                    this.mapProcessor.getMapSaveLoad().mainTextureLevel = textureLevel;
                    int leveledRegionShift = 9 + textureLevel;
                    double secondaryScale = this.scale / fboScale;
                    GlStateManager.pushMatrix();
                    double mousePosX = (double)mouseFromCentreX / this.scale + this.cameraX;
                    double mousePosZ = (double)mouseFromCentreY / this.scale + this.cameraZ;
                    GlStateManager.pushMatrix();
                    GlStateManager.translate((float)0.0f, (float)0.0f, (float)971.0f);
                    this.mouseBlockPosX = (int)Math.floor(mousePosX);
                    this.mouseBlockPosZ = (int)Math.floor(mousePosZ);
                    int mouseRegX = this.mouseBlockPosX >> leveledRegionShift;
                    int mouseRegZ = this.mouseBlockPosZ >> leveledRegionShift;
                    LeveledRegion<?> reg = this.mapProcessor.getLeveledRegion(mouseRegX, mouseRegZ, textureLevel);
                    int maxRegBlockCoord = (1 << leveledRegionShift) - 1;
                    int mouseRegPixelX = (this.mouseBlockPosX & maxRegBlockCoord) >> textureLevel;
                    int mouseRegPixelZ = (this.mouseBlockPosZ & maxRegBlockCoord) >> textureLevel;
                    this.mouseBlockPosX = (mouseRegX << leveledRegionShift) + (mouseRegPixelX << textureLevel);
                    this.mouseBlockPosZ = (mouseRegZ << leveledRegionShift) + (mouseRegPixelZ << textureLevel);
                    if (this.mapTileSelection != null && this.rightClickMenu == null) {
                        this.mapTileSelection.setEnd(this.mouseBlockPosX >> 4, this.mouseBlockPosZ >> 4);
                    }
                    MapTileChunk chunk = (leafRegion = this.mapProcessor.getMapRegion(this.mouseBlockPosX >> 9, this.mouseBlockPosZ >> 9, false)) == null ? null : leafRegion.getChunk(this.mouseBlockPosX >> 6 & 7, this.mouseBlockPosZ >> 6 & 7);
                    int debugTextureX = this.mouseBlockPosX >> leveledRegionShift - 3 & 7;
                    int debugTextureY = this.mouseBlockPosZ >> leveledRegionShift - 3 & 7;
                    RegionTexture regionTexture = tex = reg != null && reg.hasTextures() ? (RegionTexture)reg.getTexture(debugTextureX, debugTextureY) : null;
                    if (WorldMap.settings.debug) {
                        if (reg != null) {
                            ArrayList<String> debugLines = new ArrayList<String>();
                            if (tex != null) {
                                MapBlock block;
                                MapTile mouseTile;
                                tex.addDebugLines(debugLines);
                                mouseTile = chunk == null ? null : chunk.getTile(this.mouseBlockPosX >> 4 & 3, this.mouseBlockPosZ >> 4 & 3);
                                if (mouseTile != null && (block = mouseTile.getBlock(this.mouseBlockPosX & 0xF, this.mouseBlockPosZ & 0xF)) != null) {
                                    this.drawCenteredString(mc.fontRenderer, block.toString(), this.width / 2, 12, -1);
                                    if (block.getNumberOfOverlays() != 0) {
                                        for (int i = 0; i < block.getOverlays().size(); ++i) {
                                            this.drawCenteredString(mc.fontRenderer, block.getOverlays().get(i).toString(), this.width / 2, 22 + i * 10, -1);
                                        }
                                    }
                                }
                            }
                            debugLines.add("");
                            debugLines.add(mouseRegX + " " + mouseRegZ + " " + textureLevel);
                            reg.addDebugLines(debugLines, this.mapProcessor, debugTextureX, debugTextureY);
                            for (int i = 0; i < debugLines.size(); ++i) {
                                this.drawString(mc.fontRenderer, (String)debugLines.get(i), 5, 15 + 10 * i, -1);
                            }
                        }
                        if (this.mapProcessor.getMapWorld().isMultiplayer()) {
                            this.drawString(mc.fontRenderer, "MultiWorld ID: " + this.mapProcessor.getMapWorld().getCurrentMultiworld(), 5, 255, -1);
                        }
                        LeveledRegionManager regions = this.mapProcessor.getMapWorld().getCurrentDimension().getMapRegions();
                        this.drawString(mc.fontRenderer, String.format("regions: %d loaded: %d processed: %d viewed: %d benchmarks %s", regions.size(), regions.loadedCount(), this.mapProcessor.getProcessedCount(), lastAmountOfRegionsViewed, WorldMap.textureUploadBenchmark.getTotalsString()), 5, 265, -1);
                        this.drawString(mc.fontRenderer, String.format("toLoad: %d toSave: %d tile pool: %d overlays: %d toLoadBranchCache: %d buffers: %d", this.mapProcessor.getMapSaveLoad().getSizeOfToLoad(), this.mapProcessor.getMapSaveLoad().getToSave().size(), this.mapProcessor.getTilePool().size(), this.mapProcessor.getOverlayManager().getNumberOfUniqueOverlays(), this.mapProcessor.getMapSaveLoad().getSizeOfToLoadBranchCache(), WorldMap.textureDirectBufferPool.size()), 5, 275, -1);
                        long i = Runtime.getRuntime().maxMemory();
                        long j = Runtime.getRuntime().totalMemory();
                        long k = Runtime.getRuntime().freeMemory();
                        long l = j - k;
                        this.drawString(mc.fontRenderer, String.format("FPS: %d", Minecraft.getDebugFPS()), 5, 295, -1);
                        this.drawString(mc.fontRenderer, String.format("Mem: % 2d%% %03d/%03dMB", l * 100L / i, bytesToMb(l), bytesToMb(i)), 5, 305, -1);
                        this.drawString(mc.fontRenderer, String.format("Allocated: % 2d%% %03dMB", j * 100L / i, bytesToMb(j)), 5, 315, -1);
                        this.drawString(mc.fontRenderer, String.format("Available VRAM: %dMB", this.mapProcessor.getMapLimiter().getAvailableVRAM() / 1024), 5, 325, -1);
                    }
                    int pixelInsideTexX = mouseRegPixelX & 0x3F;
                    int pixelInsideTexZ = mouseRegPixelZ & 0x3F;
                    boolean hasAmbiguousHeight = false;
                    int mouseBlockTopY = -1;
                    if (tex != null) {
                        this.mouseBlockPosY = tex.getHeight(pixelInsideTexX, pixelInsideTexZ);
                        mouseBlockTopY = tex.getTopHeight(pixelInsideTexX, pixelInsideTexZ);
                        hasAmbiguousHeight = this.mouseBlockPosY != mouseBlockTopY;
                    }
                    if (WorldMap.settings.coordinates) {
                        String coordsString = "X: " + this.mouseBlockPosX;
                        if (this.mouseBlockPosY != -1) {
                            coordsString = coordsString + " Y: " + this.mouseBlockPosY;
                        }
                        if (hasAmbiguousHeight && mouseBlockTopY != -1) {
                            coordsString = coordsString + " (" + mouseBlockTopY + ")";
                        }
                        coordsString = coordsString + " Z: " + this.mouseBlockPosZ;
                        this.drawCenteredString(mc.fontRenderer, coordsString, this.width / 2, 2, -1);
                    }
                    int subtleTooltipOffset = 12;
                    if (WorldMap.settings.displayZoom) {
                        String zoomString = (double)Math.round(destScale * 1000.0) / 1000.0 + "x";
                        this.drawCenteredString(mc.fontRenderer, zoomString, this.width / 2, this.height - subtleTooltipOffset, -1);
                    }
                    if (SupportMods.minimap() && (subWorldNameToRender = SupportMods.xaeroMinimap.getSubWorldNameToRender()) != null) {
                        this.drawCenteredString(mc.fontRenderer, subWorldNameToRender, this.width / 2, this.height - (subtleTooltipOffset += 24), -1);
                    }
                    discoveredForHighlights = this.mouseBlockPosY != -1;
                    ITextComponent subtleHighlightTooltip = this.mapProcessor.getMapWorld().getCurrentDimension().getHighlightHandler().getBlockHighlightSubtleTooltip(this.mouseBlockPosX, this.mouseBlockPosZ, discoveredForHighlights);
                    if (subtleHighlightTooltip != null) {
                        this.drawCenteredString(mc.fontRenderer, subtleHighlightTooltip.getFormattedText(), this.width / 2, this.height - (subtleTooltipOffset += 12), -1);
                    }
                    if (hasAmbiguousHeight) {
                        if (mouseBlockTopY != -1) {
                            this.mouseBlockPosY = mouseBlockTopY;
                        } else if (WorldMap.settings.detectAmbiguousY) {
                            this.mouseBlockPosY = -1;
                        }
                    }
                    GlStateManager.popMatrix();
                    if (primaryScaleFBO == null || primaryScaleFBO.framebufferWidth != mc.displayWidth || primaryScaleFBO.framebufferHeight != mc.displayHeight) {
                        if (!Minecraft.getMinecraft().gameSettings.fboEnable) {
                            Minecraft.getMinecraft().gameSettings.setOptionValue(GameSettings.Options.FBO_ENABLE, 0);
                            WorldMap.LOGGER.info("FBO is off. Turning it on.");
                        }
                        primaryScaleFBO = new ImprovedFramebuffer(mc.displayWidth, mc.displayHeight, false);
                    }
                    if (primaryScaleFBO.framebufferObject == -1) {
                        GlStateManager.popMatrix();
                        return;
                    }
                    primaryScaleFBO.bindFramebuffer(false);
                    GlStateManager.clearColor((float)0.0f, (float)0.0f, (float)0.0f, (float)1.0f);
                    GL11.glClearColor((float)0.0f, (float)0.0f, (float)0.0f, (float)1.0f);
                    GlStateManager.clear((int)16384);
                    GlStateManager.color((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
                    GlStateManager.scale((float)(1.0f / (float)this.screenScale), (float)(1.0f / (float)this.screenScale), (float)1.0f);
                    GlStateManager.translate((float)(mc.displayWidth / 2), (float)(mc.displayHeight / 2), (float)0.0f);
                    GlStateManager.pushMatrix();
                    GlStateManager.disableCull();
                    int flooredCameraX = (int)Math.floor(this.cameraX);
                    int flooredCameraZ = (int)Math.floor(this.cameraZ);
                    double primaryOffsetX = 0.0;
                    double primaryOffsetY = 0.0;
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
                        int offset;
                        secondaryOffsetX = (this.cameraX - (double)flooredCameraX) * fboScale;
                        secondaryOffsetY = (this.cameraZ - (double)flooredCameraZ) * fboScale;
                        if (secondaryOffsetX >= 1.0) {
                            offset = (int)secondaryOffsetX;
                            GlStateManager.translate((float)(-offset), (float)0.0f, (float)0.0f);
                            secondaryOffsetX -= (double)offset;
                        }
                        if (secondaryOffsetY >= 1.0) {
                            offset = (int)secondaryOffsetY;
                            GlStateManager.translate((float)0.0f, (float)offset, (float)0.0f);
                            secondaryOffsetY -= (double)offset;
                        }
                    }
                    GlStateManager.scale((double)fboScale, (double)(-fboScale), (double)1.0);
                    GlStateManager.translate((double)(-primaryOffsetX), (double)(-primaryOffsetY), (double)0.0);
                    GlStateManager.enableTexture2D();
                    double leftBorder = this.cameraX - (double)(mc.displayWidth / 2) / this.scale;
                    double rightBorder = leftBorder + (double)mc.displayWidth / this.scale;
                    double topBorder = this.cameraZ - (double)(mc.displayHeight / 2) / this.scale;
                    double bottomBorder = topBorder + (double)mc.displayHeight / this.scale;
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
                    GlStateManager.disableBlend();
                    GlStateManager.disableAlpha();
                    this.regionBuffer.clear();
                    this.branchRegionBuffer.clear();
                    float brightness = this.mapProcessor.getBrightness();
                    int globalRegionCacheHashCode = WorldMap.settings.getRegionCacheHashCode();
                    boolean reloadEverything = WorldMap.settings.reloadEverything;
                    int globalReloadVersion = WorldMap.settings.reloadVersion;
                    boolean oldMinimapMessesUpTextureFilter = SupportMods.minimap() && SupportMods.xaeroMinimap.compatibilityVersion < 11;
                    int globalVersion = this.mapProcessor.getGlobalVersion();
                    boolean prevWaitingForBranchCache = this.prevWaitingForBranchCache;
                    this.waitingForBranchCache[0] = false;
                    GuiMap.setupTextureMatricesAndTextures(brightness);
                    LeveledRegion.setComparison(this.mouseBlockPosX >> leveledRegionShift, this.mouseBlockPosZ >> leveledRegionShift, textureLevel, this.mouseBlockPosX >> 9, this.mouseBlockPosZ >> 9);
                    LeveledRegion<?> lastUpdatedRootLeveledRegion = null;
                    boolean frameRenderedRootTextures = false;
                    boolean loadingLeaves = false;
                    for (int leveledRegX = minRegX; leveledRegX <= maxRegX; ++leveledRegX) {
                        for (int leveledRegZ = minRegZ; leveledRegZ <= maxRegZ; ++leveledRegZ) {
                            boolean rootHasTextures;
                            int leveledSideInRegions = 1 << textureLevel;
                            int leveledSideInBlocks = leveledSideInRegions * 512;
                            int leafRegionMinX = leveledRegX * leveledSideInRegions;
                            int leafRegionMinZ = leveledRegZ * leveledSideInRegions;
                            leveledRegion = null;
                            for (int leafX = 0; leafX < leveledSideInRegions; ++leafX) {
                                for (int leafZ = 0; leafZ < leveledSideInRegions; ++leafZ) {
                                    int regZ;
                                    int regX = leafRegionMinX + leafX;
                                    if (regX < minLeafRegX || regX > maxLeafRegX || (regZ = leafRegionMinZ + leafZ) < minLeafRegZ || regZ > maxLeafRegZ) continue;
                                    MapRegion region = this.mapProcessor.getMapRegion(regX, regZ, false);
                                    if (region == null) {
                                        region = this.mapProcessor.getMapRegion(regX, regZ, this.mapProcessor.regionExists(regX, regZ));
                                    }
                                    if (region == null) continue;
                                    if (leveledRegion == null) {
                                        leveledRegion = this.mapProcessor.getLeveledRegion(leveledRegX, leveledRegZ, textureLevel);
                                    }
                                    if (prevWaitingForBranchCache) continue;
                                    MapRegion mapRegion = region;
                                    synchronized (mapRegion) {
                                        if (textureLevel != 0 && region.getLoadState() == 0 && region.loadingNeededForBranchLevel != 0 && region.loadingNeededForBranchLevel != textureLevel) {
                                            region.loadingNeededForBranchLevel = 0;
                                            region.getParent().setShouldCheckForUpdatesRecursive(true);
                                        }
                                        if (!region.recacheHasBeenRequested() && !region.reloadHasBeenRequested() && (region.getLoadState() == 4 || region.getLoadState() == 2 && region.isBeingWritten() || region.getLoadState() == 0) && (reloadEverything && region.getReloadVersion() != globalReloadVersion || region.getCacheHashCode() != globalRegionCacheHashCode || region.getVersion() != globalVersion || (region.isMetaLoaded() || region.getLoadState() != 0 || !region.hasHadTerrain()) && region.getHighlightsHash() != region.getDim().getHighlightHandler().getRegionHash(region.getRegionX(), region.getRegionZ()) || region.getLoadState() != 2 && region.shouldCache() || region.getLoadState() == 0 && (textureLevel == 0 || region.loadingNeededForBranchLevel == textureLevel))) {
                                            loadingLeaves = true;
                                            if (region.getLoadState() == 2) {
                                                region.requestRefresh(this.mapProcessor);
                                            } else {
                                                region.calculateSortingDistance();
                                                /** inc buffer to 100 **/
                                                Misc.addToListOfSmallest(100, this.regionBuffer, region);
                                            }
                                        }
                                        continue;
                                    }
                                }
                            }
                            if (leveledRegion == null) continue;
                            LeveledRegion<?> rootLeveledRegion = leveledRegion.getRootRegion();
                            if (rootLeveledRegion == leveledRegion) {
                                rootLeveledRegion = null;
                            }
                            if (rootLeveledRegion != null && !rootLeveledRegion.isLoaded()) {
                                if (!rootLeveledRegion.recacheHasBeenRequested() && !rootLeveledRegion.reloadHasBeenRequested()) {
                                    rootLeveledRegion.calculateSortingDistance();
                                    /** inc buffer to 100**/
                                    Misc.addToListOfSmallest(100, this.branchRegionBuffer, (BranchLeveledRegion)rootLeveledRegion);
                                }
                                this.waitingForBranchCache[0] = true;
                                rootLeveledRegion = null;
                            }
                            if (!this.mapProcessor.isUploadingPaused() && !WorldMap.settings.pauseRequests) {
                                if (leveledRegion instanceof BranchLeveledRegion) {
                                    BranchLeveledRegion branchRegion = (BranchLeveledRegion)leveledRegion;
                                    branchRegion.checkForUpdates(this.mapProcessor, prevWaitingForBranchCache, this.waitingForBranchCache, this.branchRegionBuffer, textureLevel, minLeafRegX, minLeafRegZ, maxLeafRegX, maxLeafRegZ);
                                }
                                if ((textureLevel != 0 && !prevWaitingForBranchCache || textureLevel == 0 && !this.prevLoadingLeaves) && this.lastFrameRenderedRootTextures && rootLeveledRegion != null && rootLeveledRegion != lastUpdatedRootLeveledRegion) {
                                    BranchLeveledRegion branchRegion = (BranchLeveledRegion)rootLeveledRegion;
                                    branchRegion.checkForUpdates(this.mapProcessor, prevWaitingForBranchCache, this.waitingForBranchCache, this.branchRegionBuffer, textureLevel, minLeafRegX, minLeafRegZ, maxLeafRegX, maxLeafRegZ);
                                    lastUpdatedRootLeveledRegion = rootLeveledRegion;
                                }
                                this.mapProcessor.getMapWorld().getCurrentDimension().getMapRegions().bumpLoadedRegion(leveledRegion);
                                if (rootLeveledRegion != null) {
                                    this.mapProcessor.getMapWorld().getCurrentDimension().getMapRegions().bumpLoadedRegion(rootLeveledRegion);
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
                            boolean bl = rootHasTextures = rootLeveledRegion != null && rootLeveledRegion.hasTextures();
                            if (hasTextures || rootHasTextures) {
                                for (int o = 0; o < 8; ++o) {
                                    int textureX = minXBlocks + o * textureSize;
                                    if ((double)textureX > rightBorder || (double)(textureX + textureSize) < leftBorder) continue;
                                    for (int p = 0; p < 8; ++p) {
                                        int textureZ = minZBlocks + p * textureSize;
                                        if ((double)textureZ > bottomBorder || (double)(textureZ + textureSize) < topBorder) continue;
                                        RegionTexture regionTexture2 = hasTextures ? leveledRegion.getTexture(o, p) : null;
                                        if (regionTexture2 == null || regionTexture2.getGlColorTexture() == -1) {
                                            int insideZ;
                                            int rootTextureZ;
                                            int insideX;
                                            int rootTextureX;
                                            if (!rootHasTextures || (regionTexture2 = rootLeveledRegion.getTexture(rootTextureX = firstRootTextureX + ((insideX = firstInsideTextureX + o) >> levelDiff), rootTextureZ = firstRootTextureZ + ((insideZ = firstInsideTextureZ + p) >> levelDiff))) == null) continue;
                                            RegionTexture regionTexture4 = regionTexture2;
                                            synchronized (regionTexture4) {
                                                if (regionTexture2.getGlColorTexture() == -1) {
                                                    continue;
                                                }
                                                frameRenderedRootTextures = true;
                                                int insideTextureX = insideX & maxInsideCoord;
                                                int insideTextureZ = insideZ & maxInsideCoord;
                                                float textureX1 = (float)insideTextureX / (float)rootSize;
                                                float textureX2 = (float)(insideTextureX + 1) / (float)rootSize;
                                                float textureY1 = (float)insideTextureZ / (float)rootSize;
                                                float textureY2 = (float)(insideTextureZ + 1) / (float)rootSize;
                                                boolean hasLight = regionTexture2.getTextureHasLight();
                                                GuiMap.bindMapTextureWithLighting3(regionTexture2, 9728, oldMinimapMessesUpTextureFilter, 0, hasLight);
                                                GuiMap.renderTexturedModalSubRectWithLighting(textureX - flooredCameraX, textureZ - flooredCameraZ, textureX1, textureY1, textureX2, textureY2, textureSize, textureSize, hasLight);
                                                continue;
                                            }
                                        }
                                        RegionTexture insideX = regionTexture2;
                                        synchronized (insideX) {
                                            if (regionTexture2.getGlColorTexture() == -1) {
                                                continue;
                                            }
                                            boolean hasLight = regionTexture2.getTextureHasLight();
                                            GuiMap.bindMapTextureWithLighting3(regionTexture2, 9728, oldMinimapMessesUpTextureFilter, 0, hasLight);
                                            GuiMap.renderTexturedModalRectWithLighting2(textureX - flooredCameraX, textureZ - flooredCameraZ, textureSize, textureSize, hasLight);
                                            continue;
                                        }
                                    }
                                }
                            }
                            if (leveledRegion.loadingAnimation()) {
                                GlStateManager.pushMatrix();
                                GlStateManager.translate((double)((double)leveledSideInBlocks * ((double)leveledRegX + 0.5) - (double)flooredCameraX), (double)((double)leveledSideInBlocks * ((double)leveledRegZ + 0.5) - (double)flooredCameraZ), (double)0.0);
                                float loadingAnimationPassed = System.currentTimeMillis() - this.loadingAnimationStart;
                                if (loadingAnimationPassed > 0.0f) {
                                    GuiMap.restoreTextureStates();
                                    int period = 2000;
                                    int numbersOfActors = 3;
                                    float loadingAnimation = loadingAnimationPassed % (float)period / (float)period * 360.0f;
                                    float step = 360.0f / (float)numbersOfActors;
                                    GlStateManager.rotate((float)loadingAnimation, (float)0.0f, (float)0.0f, (float)1.0f);
                                    int numberOfVisibleActors = 1 + (int)loadingAnimationPassed % (3 * period) / period;
                                    GlStateManager.scale((float)leveledSideInRegions, (float)leveledSideInRegions, (float)1.0f);
                                    for (int i = 0; i < numberOfVisibleActors; ++i) {
                                        GlStateManager.rotate((float)step, (float)0.0f, (float)0.0f, (float)1.0f);
                                        GuiMap.drawRect((int)16, (int)-8, (int)32, (int)8, (int)-1);
                                    }
                                    GlStateManager.disableBlend();
                                    GuiMap.setupTextureMatricesAndTextures(brightness);
                                }
                                GlStateManager.popMatrix();
                            }
                            if (WorldMap.settings.debug && leveledRegion instanceof MapRegion) {
                                MapRegion region = (MapRegion)leveledRegion;
                                GuiMap.restoreTextureStates();
                                GlStateManager.pushMatrix();
                                GlStateManager.translate((float)(512 * region.getRegionX() + 32 - flooredCameraX), (float)(512 * region.getRegionZ() + 32 - flooredCameraZ), (float)0.0f);
                                GlStateManager.scale((float)10.0f, (float)10.0f, (float)1.0f);
                                this.drawString(mc.fontRenderer, "" + region.getLoadState(), 0, 0, -1);
                                GlStateManager.popMatrix();
                                GlStateManager.disableBlend();
                                GuiMap.setupTextureMatricesAndTextures(brightness);
                            }
                            if (!WorldMap.settings.debug || textureLevel <= 0) continue;
                            GuiMap.restoreTextureStates();
                            for (int leafX = 0; leafX < leveledSideInRegions; ++leafX) {
                                for (int leafZ = 0; leafZ < leveledSideInRegions; ++leafZ) {
                                    boolean currentlyLoading;
                                    int regX = leafRegionMinX + leafX;
                                    int regZ = leafRegionMinZ + leafZ;
                                    MapRegion region = this.mapProcessor.getMapRegion(regX, regZ, false);
                                    if (region == null) continue;
                                    currentlyLoading = this.mapProcessor.getMapSaveLoad().getNextToLoadByViewing() == region;
                                    if (!currentlyLoading && !region.isLoaded() && !region.isMetaLoaded()) continue;
                                    GlStateManager.pushMatrix();
                                    GlStateManager.translate((float)(512 * region.getRegionX() - flooredCameraX), (float)(512 * region.getRegionZ() - flooredCameraZ), (float)0.0f);
                                    GuiMap.drawRect((int)0, (int)0, (int)512, (int)512, (int)(currentlyLoading ? 687800575 : (region.isLoaded() ? 671153920 : 687865600)));
                                    GlStateManager.popMatrix();
                                }
                            }
                            GlStateManager.disableBlend();
                            GuiMap.setupTextureMatricesAndTextures(brightness);
                        }
                    }
                    this.lastFrameRenderedRootTextures = frameRenderedRootTextures;
                    GuiMap.restoreTextureStates();
                    LeveledRegion<?> nextToLoad = this.mapProcessor.getMapSaveLoad().getNextToLoadByViewing();
                    boolean shouldRequest = false;
                    if (nextToLoad != null) {
                        LeveledRegion<?> leveledSideInRegions = nextToLoad;
                        synchronized (leveledSideInRegions) {
                            if (!nextToLoad.reloadHasBeenRequested() && !nextToLoad.hasRemovableSourceData()) {
                                shouldRequest = true;
                            }
                        }
                    } else {
                        shouldRequest = true;
                    }
                    /** inc loading freq limit **/
                    shouldRequest = shouldRequest && this.mapProcessor.getAffectingLoadingFrequencyCount() < 128;
                    if (shouldRequest && !WorldMap.settings.pauseRequests) {
                        int i;
                        /** Modify: inc toRequest limit **/
                        int toRequest = 50; //
                        int counter = 0;
                        for (i = 0; i < this.branchRegionBuffer.size() && counter < toRequest; ++i) {
                            BranchLeveledRegion region = this.branchRegionBuffer.get(i);
                            if (region.reloadHasBeenRequested() || region.recacheHasBeenRequested() || region.isLoaded()) continue;
                            region.setReloadHasBeenRequested(true, "Gui");
                            this.mapProcessor.getMapSaveLoad().requestBranchCache(region, "Gui");
                            if (counter == 0) {
                                this.mapProcessor.getMapSaveLoad().setNextToLoadByViewing(region);
                            }
                            ++counter;
                        }
                        counter = 0;
                        if (!prevWaitingForBranchCache) {
                            for (i = 0; i < this.regionBuffer.size() && counter < toRequest; ++i) {
                                MapRegion region = this.regionBuffer.get(i);
                                if (region == nextToLoad) continue; // remove check on regionbuffer size
                                leveledRegion = region;
                                synchronized (leveledRegion) {
                                    if (region.reloadHasBeenRequested() || region.recacheHasBeenRequested() || region.getLoadState() != 0 && region.getLoadState() != 4) {
                                        continue;
                                    }
                                    this.mapProcessor.getMapSaveLoad().requestLoad(region, "Gui");
                                    if (counter == 0) {
                                        this.mapProcessor.getMapSaveLoad().setNextToLoadByViewing(region);
                                    }
                                    ++counter;
                                    if (region.getLoadState() == 4) {
                                        break;
                                    }
                                    continue;
                                }
                            }
                        }
                    }
                    this.prevWaitingForBranchCache = this.waitingForBranchCache[0];
                    this.prevLoadingLeaves = loadingLeaves;
                    int chunkHighlightLeftX = this.mouseBlockPosX >> 4 << 4;
                    int chunkHighlightRightX = (this.mouseBlockPosX >> 4) + 1 << 4;
                    int chunkHighlightTopZ = this.mouseBlockPosZ >> 4 << 4;
                    int chunkHighlightBottomZ = (this.mouseBlockPosZ >> 4) + 1 << 4;
                    MapRenderHelper.renderDynamicHighlight(flooredCameraX, flooredCameraZ, chunkHighlightLeftX, chunkHighlightRightX, chunkHighlightTopZ, chunkHighlightBottomZ, 0.0f, 0.0f, 0.0f, 0.2f, 1.0f, 1.0f, 1.0f, 0.1569f);
                    if (this.mapTileSelection != null) {
                        MapRenderHelper.renderDynamicHighlight(flooredCameraX, flooredCameraZ, this.mapTileSelection.getLeft() << 4, this.mapTileSelection.getRight() + 1 << 4, this.mapTileSelection.getTop() << 4, this.mapTileSelection.getBottom() + 1 << 4, 0.0f, 0.0f, 0.0f, 0.2f, 1.0f, 0.5f, 0.5f, 0.4f);
                    }
                    GlStateManager.enableBlend();
                    GlStateManager.enableAlpha();
                    GlStateManager.disableBlend();
                    GlStateManager.disableAlpha();
                    primaryScaleFBO.unbindFramebuffer();
                    Minecraft.getMinecraft().getFramebuffer().bindFramebuffer(false);
                    GlStateManager.enableCull();
                    GlStateManager.popMatrix();
                    GlStateManager.pushMatrix();
                    GlStateManager.scale((double)secondaryScale, (double)secondaryScale, (double)1.0);
                    primaryScaleFBO.bindFramebufferTexture();
                    GL11.glTexParameteri((int)3553, (int)10240, (int)9729);
                    GL11.glTexParameteri((int)3553, (int)10241, (int)9729);
                    GlStateManager.depthMask((boolean)false);
                    int lineX = -mc.displayWidth / 2;
                    int lineY = mc.displayHeight / 2 - 5;
                    int lineW = mc.displayWidth;
                    int lineH = 6;
                    GuiMap.drawRect((int)lineX, (int)lineY, (int)(lineX + lineW), (int)(lineY + lineH), (int)-16777216);
                    lineX = mc.displayWidth / 2 - 5;
                    lineY = -mc.displayHeight / 2;
                    lineW = 6;
                    lineH = mc.displayHeight;
                    GuiMap.drawRect((int)lineX, (int)lineY, (int)(lineX + lineW), (int)(lineY + lineH), (int)-16777216);
                    GlStateManager.color((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
                    GlStateManager.enableDepth();
                    if (SupportMods.vivecraft) {
                        GlStateManager.enableBlend();
                        GlStateManager.tryBlendFuncSeparate((int)1, (int)0, (int)0, (int)1);
                    }
                    GuiMap.renderTexturedModalRect((float)(-mc.displayWidth / 2) - (float)secondaryOffsetX, (float)(-mc.displayHeight / 2) - (float)secondaryOffsetY, mc.displayWidth, mc.displayHeight);
                    GlStateManager.depthMask((boolean)true);
                    if (SupportMods.vivecraft) {
                        GlStateManager.tryBlendFuncSeparate((int)770, (int)771, (int)1, (int)0);
                    }
                    GlStateManager.popMatrix();
                    GlStateManager.scale((double)this.scale, (double)this.scale, (double)1.0);
                    GlStateManager.enableBlend();
                    GlStateManager.enableAlpha();
                    double screenSizeBasedScale = scaleMultiplier;
                    GlStateManager.disableCull();
                    try {
                        this.viewed = WorldMap.mapElementRenderHandler.render((GuiMap)(Object)this, this.cameraX, this.cameraZ, mc.displayWidth, mc.displayHeight, screenSizeBasedScale, this.scale, mousePosX, mousePosZ, brightness, this.mapProcessor.getCaveStart() != -1, this.viewed, mc, partialTicks, this.scaledresolution);
                    }
                    catch (Throwable t) {
                        WorldMap.LOGGER.error("error rendering map elements", t);
                        throw t;
                    }
                    this.viewedInList = false;
                    GlStateManager.enableCull();
                    GlStateManager.enableBlend();
                    GlStateManager.pushMatrix();
                    GlStateManager.translate((float)0.0f, (float)0.0f, (float)970.0f);
                    if (WorldMap.settings.footsteps) {
                        ArrayList<Double[]> footprints;
                        ArrayList<Double[]> levelDiff = footprints = this.mapProcessor.getFootprints();
                        synchronized (levelDiff) {
                            for (int i = 0; i < footprints.size(); ++i) {
                                Double[] coords = footprints.get(i);
                                this.setColourBuffer(1.0f, 0.1f, 0.1f, 1.0f);
                                this.drawDotOnMap(coords[0] - this.cameraX, coords[1] - this.cameraZ, 0.0f, 1.0 / this.scale);
                            }
                        }
                    }
                    if (WorldMap.settings.renderArrow) {
                        boolean toTheLeft = this.player.posX < leftBorder;
                        boolean toTheRight = this.player.posX > rightBorder;
                        boolean down = this.player.posZ > bottomBorder;
                        boolean up = this.player.posZ < topBorder;
                        GlStateManager.enableBlend();
                        float configuredR = 1.0f;
                        float configuredG = 1.0f;
                        float configuredB = 1.0f;
                        int effectiveArrowColorIndex = WorldMap.settings.arrowColour;
                        if (effectiveArrowColorIndex == -2 && !SupportMods.minimap()) {
                            effectiveArrowColorIndex = 0;
                        }
                        if (effectiveArrowColorIndex == -2 && SupportMods.xaeroMinimap.getArrowColorIndex() == -1) {
                            effectiveArrowColorIndex = -1;
                        }
                        if (effectiveArrowColorIndex == -1) {
                            int rgb = Misc.getTeamColour((Entity)mc.player);
                            if (rgb == -1) {
                                effectiveArrowColorIndex = 0;
                            } else {
                                configuredR = (float)(rgb >> 16 & 0xFF) / 255.0f;
                                configuredG = (float)(rgb >> 8 & 0xFF) / 255.0f;
                                configuredB = (float)(rgb & 0xFF) / 255.0f;
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
                        if (toTheLeft || toTheRight || up || down) {
                            double arrowX = this.player.posX;
                            double arrowZ = this.player.posZ;
                            float a = 0.0f;
                            if (toTheLeft) {
                                a = up ? 1.5f : (down ? 0.5f : 1.0f);
                                arrowX = leftBorder;
                            } else if (toTheRight) {
                                a = up ? 2.5f : (down ? 3.5f : 3.0f);
                                arrowX = rightBorder;
                            }
                            if (down) {
                                arrowZ = bottomBorder;
                            } else if (up) {
                                if (a == 0.0f) {
                                    a = 2.0f;
                                }
                                arrowZ = topBorder;
                            }
                            this.setColourBuffer(0.0f, 0.0f, 0.0f, 0.9f);
                            this.drawFarArrowOnMap(arrowX - this.cameraX, arrowZ + 2.0 * screenSizeBasedScale / this.scale - this.cameraZ, a, screenSizeBasedScale / this.scale);
                            this.setColourBuffer(configuredR, configuredG, configuredB, 1.0f);
                            this.drawFarArrowOnMap(arrowX - this.cameraX, arrowZ - this.cameraZ, a, screenSizeBasedScale / this.scale);
                        } else {
                            this.setColourBuffer(0.0f, 0.0f, 0.0f, 0.9f);
                            this.drawArrowOnMap(this.player.posX - this.cameraX, this.player.posZ + 2.0 * screenSizeBasedScale / this.scale - this.cameraZ, this.player.rotationYaw, screenSizeBasedScale / this.scale);
                            this.setColourBuffer(configuredR, configuredG, configuredB, 1.0f);
                            this.drawArrowOnMap(this.player.posX - this.cameraX, this.player.posZ - this.cameraZ, this.player.rotationYaw, screenSizeBasedScale / this.scale);
                        }
                    }
                    GlStateManager.popMatrix();
                    GlStateManager.popMatrix();
                    this.overWaypointsMenu = false;
                    if (this.waypointMenu && SupportMods.xaeroMinimap.getWaypointsSorted() != null) {
                        GlStateManager.pushMatrix();
                        GlStateManager.translate((float)0.0f, (float)0.0f, (float)972.0f);
                        HoveredMapElementHolder<?, ?> hovered = SupportMods.xaeroMinimap.renderWaypointsMenu((GuiMap)(Object)this, this.scale, this.width, this.height, scaledMouseX, scaledMouseY, this.leftMouseButton.isDown, this.leftMouseButton.clicked, this.viewed, mc);
                        if (hovered != null) {
                            this.overWaypointsMenu = true;
                            if (hovered.getElement() instanceof Waypoint) {
                                this.viewed = hovered;
                                this.viewedInList = true;
                                if (this.leftMouseButton.clicked) {
                                    this.cameraDestination = new int[]{(int)((Waypoint)this.viewed.getElement()).getRenderX(), (int)((Waypoint)this.viewed.getElement()).getRenderZ()};
                                    this.leftMouseButton.isDown = false;
                                    if (WorldMap.settings.closeWaypointsWhenHopping) {
                                        onWaypointsButton(this.waypointsButton);
                                    }
                                }
                            }
                        }
                        GlStateManager.popMatrix();
                    }
                    if (SupportMods.minimap()) {
                        SupportMods.xaeroMinimap.drawSetChange(this.scaledresolution);
                    }
                    GlStateManager.color((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
                } else if (!mapLoaded) {
                    this.renderLoadingScreen();
                } else if (isLocked) {
                    this.renderMessageScreen(I18n.format((String)"gui.xaero_current_map_locked1", (Object[])new Object[0]), I18n.format((String)"gui.xaero_current_map_locked2", (Object[])new Object[0]));
                } else if (noWorldMapEffect) {
                    this.renderMessageScreen(I18n.format((String)"gui.xaero_no_world_map_message", (Object[])new Object[0]));
                } else if (!allowedBasedOnItem) {
                    this.renderMessageScreen(I18n.format((String)"gui.xaero_no_world_map_item_message", (Object[])new Object[0]), ModSettings.mapItem.getUnlocalizedName());
                }
            } else {
                this.renderLoadingScreen();
            }
            this.dimensionSettings.renderText(mc, scaledMouseX, scaledMouseY, this.width, this.height);
            mc.getTextureManager().bindTexture(WorldMap.guiTextures);
            this.drawTexturedModalRect(this.width - 34, 2, 0, 37, 32, 32);
        }
        GlStateManager.enableDepth();
        GlStateManager.pushMatrix();
        GlStateManager.translate((float)0.0f, (float)0.0f, (float)973.0f);
        super.drawScreen(scaledMouseX, scaledMouseY, partialTicks);
        if (this.waypointMenu) {
            SupportMods.xaeroMinimap.getWaypointMenuRenderer().postMapRender((GuiMap)(Object)this, scaledMouseX, scaledMouseY, this.width, this.height, partialTicks);
        }
        for (GuiDropDown d : this.dropdowns) {
            if (!d.isClosed()) continue;
            d.drawButton(scaledMouseX, scaledMouseY, this.height);
        }
        for (GuiDropDown d : this.dropdowns) {
            if (d.isClosed()) continue;
            d.drawButton(scaledMouseX, scaledMouseY, this.height);
        }
        this.dimensionSettings.postMapRender(mc, scaledMouseX, scaledMouseY, this.width, this.height);
        GlStateManager.translate((float)0.0f, (float)0.0f, (float)10.0f);
        if (mc.currentScreen == this) {
            if (!(this.renderTooltips(scaledMouseX, scaledMouseY, partialTicks) || this.leftMouseButton.isDown || this.rightMouseButton.isDown)) {
                if (this.viewed != null) {
                    CursorBox hoveredTooltip = this.hoveredElementTooltipHelper(this.viewed, this.viewedInList);
                    if (hoveredTooltip != null) {
                        hoveredTooltip.drawBox(scaledMouseX, scaledMouseY, this.width, this.height);
                    }
                } else {
                    object = this.mapProcessor.renderThreadPauseSync;
                    synchronized (object) {
                        ITextComponent bluntHighlightTooltip;
                        if (!this.mapProcessor.isRenderingPaused() && this.mapProcessor.getCurrentWorldId() != null && this.mapProcessor.getMapSaveLoad().isRegionDetectionComplete() && (bluntHighlightTooltip = this.mapProcessor.getMapWorld().getCurrentDimension().getHighlightHandler().getBlockHighlightBluntTooltip(this.mouseBlockPosX, this.mouseBlockPosZ, discoveredForHighlights)) != null) {
                            new CursorBox(bluntHighlightTooltip).drawBox(scaledMouseX, scaledMouseY, this.width, this.height);
                        }
                    }
                }
            }
            GlStateManager.translate((float)0.0f, (float)0.0f, (float)1.0f);
            this.mapProcessor.getMessageBoxRenderer().render(this.mapProcessor.getMessageBox(), this.fontRenderer, 1, this.height / 2, false);
        }
        GlStateManager.popMatrix();
        this.rightMouseButton.clicked = false;
        this.leftMouseButton.clicked = false;
        this.noUploadingLimits = this.cameraX == cameraXBefore && this.cameraZ == cameraZBefore && scaleBefore == this.scale;


        // insert Coordinates goto button
        if (mc.currentScreen != null && mc.currentScreen.getClass().equals(GuiMap.class)) {
            if (xTextEntryField.getText().isEmpty() && !this.xTextEntryField.isFocused()) {
                Misc.setFieldText(xTextEntryField, "X:", -11184811);
            }
            xTextEntryField.drawTextBox();
            if (zTextEntryField.getText().isEmpty() && !this.zTextEntryField.isFocused()) {
                Misc.setFieldText(zTextEntryField, "Z:", -11184811);
            }
            zTextEntryField.drawTextBox();
        }
        // hacky overwrite
        ci.cancel();
    }

    @Inject(method = "mouseClicked(III)V", at = @At(value = "TAIL"), remap = true)
    public void mouseClicked(int x, int y, int button, CallbackInfo ci) throws IOException {
        if (button == 0) {
            handleTextFieldClick(x, y, xTextEntryField);
            handleTextFieldClick(x, y, zTextEntryField);
        }
    }

    private void handleTextFieldClick(int x, int y, GuiTextField guiTextField) {
        if ((x >= guiTextField.x && x <= guiTextField.x + guiTextField.width) && (y >= guiTextField.y && y <= guiTextField.y + xTextEntryField.height)) {
            guiTextField.setFocused(true);
            if (guiTextField.getText().startsWith("X:") || guiTextField.getText().startsWith("Z:")) {
                guiTextField.setText("");
                guiTextField.setTextColor(14737632);
            }
            this.setFocused(guiTextField);
            guiTextField.mouseClicked(x, y, 0);
        } else {
            guiTextField.setFocused(false);
        }
    }

    public void onGotoCoordinatesButton(final GuiButton b) {
        try {
            int x = Integer.parseInt(xTextEntryField.getText());
            int z = Integer.parseInt(zTextEntryField.getText());
            cameraX = x;
            cameraZ = z;
        } catch (final NumberFormatException e) {
            // todo: do some default action if we detect placeholder text like go to 0,0?
            WorldMap.LOGGER.warn("Go to coordinates failed" , e);
        }
    }
}
