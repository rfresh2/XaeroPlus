package xaeroplus.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import xaero.common.IXaeroMinimap;
import xaero.common.effect.Effects;
import xaero.common.minimap.region.MinimapTile;
import xaero.common.minimap.render.MinimapRendererHelper;
import xaero.common.mods.SupportXaeroWorldmap;
import xaero.hud.minimap.module.MinimapSession;
import xaero.hud.minimap.world.MinimapWorld;
import xaero.map.MapProcessor;
import xaero.map.WorldMap;
import xaero.map.WorldMapSession;
import xaero.map.gui.GuiMap;
import xaero.map.misc.Misc;
import xaero.map.region.LeveledRegion;
import xaero.map.region.MapRegion;
import xaero.map.region.MapTileChunk;
import xaeroplus.module.ModuleManager;
import xaeroplus.module.impl.NewChunks;
import xaeroplus.module.impl.PortalSkipDetection;
import xaeroplus.module.impl.Portals;
import xaeroplus.settings.XaeroPlusSettingRegistry;
import xaeroplus.util.ChunkUtils;
import xaeroplus.util.Globals;
import xaeroplus.util.GuiHelper;
import xaeroplus.util.WDLHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import static org.lwjgl.opengl.GL11.*;
import static xaeroplus.settings.XaeroPlusSettingRegistry.transparentMinimapBackground;

@Mixin(value = SupportXaeroWorldmap.class, remap = false)
public abstract class MixinSupportXaeroWorldmap {
    @Shadow
    public int compatibilityVersion;
    @Shadow
    private IXaeroMinimap modMain;
    @Shadow
    private int previousRenderedCaveLayer;
    @Shadow
    private int lastRenderedCaveLayer;
    @Shadow
    private ArrayList<MapRegion> regionBuffer;
    @Final
    @Shadow
    private static HashMap<MapTileChunk, Long> seedsUsed;
    @Shadow
    public abstract float getMinimapBrightness();
    @Shadow
    protected abstract void bindMapTextureWithLighting(int compatibilityVersion, float brightness, MapTileChunk chunk, boolean zooming);
    @Shadow
    public abstract boolean hasCaveLayers();
    @Shadow
    public abstract boolean hasDimensionSwitching();
    @Shadow
    protected abstract void bumpLoadedRegion(MapProcessor mapProcessor, MapRegion region, boolean wmHasCaveLayers);

    /**
     * @author rfresh2
     * @reason Render NewChunks on minimap
     */
    @Overwrite
    public void drawMinimap(MinimapSession minimapSession, MinimapRendererHelper helper, int xFloored, int zFloored, int minViewX, int minViewZ, int maxViewX, int maxViewZ, boolean zooming, double zoom, double mapDimensionScale) {
        WorldMapSession worldmapSession = WorldMapSession.getCurrentSession();
        if (worldmapSession != null) {
            final boolean isDimensionSwitched = Globals.getCurrentDimensionId() != Minecraft.getMinecraft().player.dimension;
            MapProcessor mapProcessor = worldmapSession.getMapProcessor();
            synchronized (mapProcessor.renderThreadPauseSync) {
                if (!mapProcessor.isRenderingPaused()) {
                    if (mapProcessor.getCurrentDimension() == null) {
                        return;
                    }

                    int compatibilityVersion = this.compatibilityVersion;
                    String worldString = compatibilityVersion >= 7 ? mapProcessor.getCurrentWorldId() : mapProcessor.getCurrentWorldString();
                    if (worldString == null) {
                        return;
                    }

                    int mapX = xFloored >> 4;
                    int mapZ = zFloored >> 4;
                    int chunkX = mapX >> 2;
                    int chunkZ = mapZ >> 2;
                    int tileX = mapX & 3;
                    int tileZ = mapZ & 3;
                    int insideX = xFloored & 15;
                    int insideZ = zFloored & 15;
                    GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                    GlStateManager.enableBlend();
                    final int scaledSize = Globals.minimapScalingFactor * 4;
                    int minX = (mapX >> 2) - scaledSize;
                    int maxX = (mapX >> 2) + scaledSize;
                    int minZ = (mapZ >> 2) - scaledSize;
                    int maxZ = (mapZ >> 2) + scaledSize;
                    boolean wmHasCaveLayers = this.hasCaveLayers();
                    boolean wmUsesHashcodes = compatibilityVersion >= 5;
                    boolean wmHasFullReload = compatibilityVersion >= 23;
                    int globalRegionCacheHashCode = wmUsesHashcodes ? WorldMap.settings.getRegionCacheHashCode() : 0;
                    boolean reloadEverything = wmUsesHashcodes ? WorldMap.settings.reloadEverything : false;
                    int globalReloadVersion = wmUsesHashcodes ? WorldMap.settings.reloadVersion : 0;
                    boolean slimeChunks = this.modMain.getSettings().getSlimeChunks(minimapSession);
                    boolean wmHasDimensionSwitch = this.hasDimensionSwitching();
                    if (wmHasDimensionSwitch) {
                        mapProcessor.initMinimapRender(xFloored, zFloored);
                    }
                    if (!wmHasDimensionSwitch && wmHasCaveLayers) {
                        mapProcessor.updateCaveStart();
                    }

                    int renderedCaveLayer = wmHasCaveLayers ? mapProcessor.getCurrentCaveLayer() : 0;
                    int globalCaveStart = !wmHasDimensionSwitch && wmHasCaveLayers
                        ? mapProcessor.getMapWorld().getCurrentDimension().getLayeredMapRegions().getLayer(renderedCaveLayer).getCaveStart()
                        : 0;
                    int globalCaveDepth = !wmHasDimensionSwitch && wmHasCaveLayers ? WorldMap.settings.caveModeDepth : 0;
                    float brightness = this.getMinimapBrightness();
                    if (renderedCaveLayer != this.lastRenderedCaveLayer) {
                        this.previousRenderedCaveLayer = this.lastRenderedCaveLayer;
                    }

                    EntityPlayer player = Minecraft.getMinecraft().player;
                    boolean noCaveMaps = player.isPotionActive(Effects.NO_CAVE_MAPS) || player.isPotionActive(Effects.NO_CAVE_MAPS_HARMFUL);
                    boolean playerIsMoving = !wmHasDimensionSwitch
                        && (player.prevPosX != player.posX || player.prevPosY != player.posY || player.prevPosZ != player.posZ);
                    boolean shouldRequestLoading = true;
                    Object nextToLoadObj = null;
                    if (!wmHasDimensionSwitch && compatibilityVersion >= 11) {
                        shouldRequestLoading = false;
                        LeveledRegion<?> nextToLoad = mapProcessor.getMapSaveLoad().getNextToLoadByViewing();
                        nextToLoadObj = nextToLoad;
                        if (nextToLoad != null) {
                            if (wmHasFullReload) {
                                shouldRequestLoading = nextToLoad.shouldAllowAnotherRegionToLoad();
                            } else {
                                synchronized (nextToLoad) {
                                    if (!nextToLoad.reloadHasBeenRequested()
                                        && !nextToLoad.hasRemovableSourceData()
                                        && (!(nextToLoad instanceof MapRegion) || !((MapRegion) nextToLoad).isRefreshing())) {
                                        shouldRequestLoading = true;
                                    }
                                }
                            }
                        } else {
                            shouldRequestLoading = true;
                        }

                        this.regionBuffer.clear();
                        if (wmHasCaveLayers) {
                            int comparisonChunkX = (xFloored >> 4) - 16;
                            int comparisonChunkZ = (zFloored >> 4) - 16;
                            LeveledRegion.setComparison(comparisonChunkX, comparisonChunkZ, 0, comparisonChunkX, comparisonChunkZ);
                        } else {
                            int comparisonRegionX = xFloored >> 9;
                            int comparisonRegionZ = zFloored >> 9;
                            LeveledRegion.setComparison(comparisonRegionX, comparisonRegionZ, 0, comparisonRegionX, comparisonRegionZ);
                        }
                    }


                    if (compatibilityVersion >= 7) {
                        GL14.glBlendFuncSeparate(1, 0, 0, 1);
                    }

                    if (compatibilityVersion >= 6) {
                        GuiMap.setupTextureMatricesAndTextures(brightness);
                    }

                    MinimapWorld world = minimapSession.getWorldManager().getAutoWorld();
                    Long seed = slimeChunks && world != null ? this.modMain.getSettings().getSlimeChunksSeed(world.getFullPath()) : null;
                    this.renderChunks(
                        minX,
                        maxX,
                        minZ,
                        maxZ,
                        minViewX,
                        maxViewX,
                        minViewZ,
                        maxViewZ,
                        mapProcessor,
                        renderedCaveLayer,
                        shouldRequestLoading,
                        reloadEverything,
                        globalReloadVersion,
                        globalRegionCacheHashCode,
                        globalCaveStart,
                        globalCaveDepth,
                        playerIsMoving,
                        noCaveMaps,
                        slimeChunks,
                        chunkX,
                        chunkZ,
                        tileX,
                        tileZ,
                        insideX,
                        insideZ,
                        seed,
                        wmHasCaveLayers,
                        wmUsesHashcodes,
                        brightness,
                        zooming,
                        helper
                    );

                    // TODO: moving this code to MinimapFBORenderer would allow it to render on top of grid lines
                    if (XaeroPlusSettingRegistry.showRenderDistanceSetting.getValue() && !isDimensionSwitched) {
                        GuiMap.restoreTextureStates();
                        if (compatibilityVersion >= 7) {
                            GL14.glBlendFuncSeparate(770, 771, 1, 771);
                        }

                        final int setting = (int) XaeroPlusSettingRegistry.assumedServerRenderDistanceSetting.getValue();
                        int width = setting * 2 + 1;
                        // origin of the chunk we are standing in
                        final int middleChunkX = -insideX;
                        final int middleChunkZ = -insideZ;
                        // this is biased to +x/+z for even sizes which im not certain is correct
                        final int x0 = middleChunkX - (width / 2) * 16;
                        final int z0 = middleChunkZ - (width / 2) * 16;
                        final int x1 = x0 + width * 16;
                        final int z1 = z0 + width * 16;

                        Tessellator tessellator = Tessellator.getInstance();
                        BufferBuilder vertexBuffer = tessellator.getBuffer();
                        vertexBuffer.begin(GL_LINE_LOOP, DefaultVertexFormats.POSITION);
                        GlStateManager.disableTexture2D();
                        GlStateManager.enableBlend();
                        // yellow
                        GlStateManager.color(1.f, 1.f, 0.f, 0.8F);
                        GlStateManager.glLineWidth((float) this.modMain.getSettings().chunkGridLineWidth * Globals.minimapScalingFactor);
                        vertexBuffer.pos(x0, z0, 0.0).endVertex();
                        vertexBuffer.pos(x1, z0, 0.0).endVertex();
                        vertexBuffer.pos(x1, z1, 0.0).endVertex();
                        vertexBuffer.pos(x0, z1, 0.0).endVertex();
                        tessellator.draw();
                        GlStateManager.enableTexture2D();
                        GlStateManager.disableBlend();

                        if (compatibilityVersion >= 6) {
                            GuiMap.setupTextures(brightness);
                        }

                        if (compatibilityVersion >= 7) {
                            GL14.glBlendFuncSeparate(1, 0, 0, 1);
                            GlStateManager.enableBlend();
                        }

                    }

                    GuiMap.restoreTextureStates();
                    GL14.glBlendFuncSeparate(770, 771, 1, 0);
                    GlStateManager.disableBlend();
                    this.lastRenderedCaveLayer = renderedCaveLayer;
                    if (wmHasDimensionSwitch) {
                        mapProcessor.finalizeMinimapRender();
                    } else if (compatibilityVersion >= 11) {
                        int toRequest = 1;
                        int counter = 0;

                        for (int i = 0; i < this.regionBuffer.size() && counter < toRequest; ++i) {
                            MapRegion region = (MapRegion) this.regionBuffer.get(i);
                            if (region != nextToLoadObj || this.regionBuffer.size() <= 1) {
                                synchronized (region) {
                                    if (!wmHasFullReload || region.canRequestReload_unsynced()) {
                                        if (wmHasFullReload
                                            || !region.reloadHasBeenRequested()
                                            && !region.recacheHasBeenRequested()
                                            && (!(region instanceof MapRegion) || !region.isRefreshing())
                                            && (region.getLoadState() == 0 || region.getLoadState() == 4 || region.getLoadState() == 2 && region.isBeingWritten())) {
                                            if (region.getLoadState() == 2) {
                                                region.requestRefresh(mapProcessor);
                                            } else {
                                                mapProcessor.getMapSaveLoad()
                                                    .requestLoad(region, "Minimap sorted", false);
                                            }

                                            if (counter == 0) {
                                                mapProcessor.getMapSaveLoad().setNextToLoadByViewing(region);
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
                }
            }
        }
    }

    /**
     * @author rfresh2
     * @reason custom drawing
     */
    @Overwrite
    private void renderChunks(
        int minX,
        int maxX,
        int minZ,
        int maxZ,
        int minViewX,
        int maxViewX,
        int minViewZ,
        int maxViewZ,
        MapProcessor mapProcessor,
        int renderedCaveLayer,
        boolean shouldRequestLoading,
        boolean reloadEverything,
        int globalReloadVersion,
        int globalRegionCacheHashCode,
        int globalCaveStart,
        int globalCaveDepth,
        boolean playerIsMoving,
        boolean noCaveMaps,
        boolean slimeChunks,
        int chunkX,
        int chunkZ,
        int tileX,
        int tileZ,
        int insideX,
        int insideZ,
        Long seed,
        boolean wmHasCaveLayers,
        boolean wmUsesHashcodes,
        float brightness,
        boolean zooming,
        MinimapRendererHelper helper
    ) {
        final boolean isDimensionSwitched = Globals.getCurrentDimensionId() != Minecraft.getMinecraft().player.dimension;
        MapRegion prevRegion = null;
        boolean wmHasFullReload = this.compatibilityVersion >= 23;
        boolean wmHasDimensionSwitch = this.hasDimensionSwitching();

        for (int i = minX; i <= maxX; ++i) {
            for (int j = minZ; j <= maxZ; ++j) {
                MapRegion region;
                if (wmHasDimensionSwitch) {
                    region = mapProcessor.getMinimapMapRegion(i >> 3, j >> 3);
                    mapProcessor.beforeMinimapRegionRender(region);
                } else if (wmHasCaveLayers) {
                    region = mapProcessor.getMapRegion(renderedCaveLayer, i >> 3, j >> 3, mapProcessor.regionExists(renderedCaveLayer, i >> 3, j >> 3));
                } else {
                    region = mapProcessor.getMapRegion(i >> 3, j >> 3, mapProcessor.regionExists(i >> 3, j >> 3));
                }

                if (!wmHasDimensionSwitch && region != null && region != prevRegion) {
                    synchronized (region) {
                        int regionHashCode = wmUsesHashcodes ? region.getCacheHashCode() : 0;
                        int regionReloadVersion = wmUsesHashcodes ? region.getReloadVersion() : 0;
                        if (shouldRequestLoading
                            && (
                                wmHasFullReload && region.canRequestReload_unsynced()
                                    || !wmHasFullReload
                                    && !region.recacheHasBeenRequested()
                                    && !region.reloadHasBeenRequested()
                                    && (!(region instanceof MapRegion) || !region.isRefreshing())
                            )
                            && (region.getLoadState() == 0 || (region.getLoadState() == 4 || region.getLoadState() == 2 && region.isBeingWritten())
                            && (reloadEverything && regionReloadVersion != globalReloadVersion
                            || regionHashCode != globalRegionCacheHashCode
                            || wmHasCaveLayers && !playerIsMoving && region.caveStartOutdated(globalCaveStart, globalCaveDepth)
                            || region.getVersion() != mapProcessor.getGlobalVersion()
                            || compatibilityVersion >= 11 && (region.isMetaLoaded() || region.getLoadState() != 0 || !region.hasHadTerrain())
                            && region.getHighlightsHash() != region.getDim().getHighlightHandler().getRegionHash(region.getRegionX(), region.getRegionZ())
                            || region.getLoadState() != 2 && region.shouldCache()))) {
                            if (compatibilityVersion >= 11) {
                                if (!this.regionBuffer.contains(region)) {
                                    if (wmHasCaveLayers) {
                                        region.calculateSortingChunkDistance();
                                    } else {
                                        region.calculateSortingDistance();
                                    }

                                    Misc.addToListOfSmallest(10, this.regionBuffer, region);
                                }
                            } else if (region.getLoadState() == 2) {
                                region.requestRefresh(mapProcessor);
                            } else if (shouldRequestLoading) {
                                mapProcessor.getMapSaveLoad().requestLoad(region, "Minimap", false);
                                mapProcessor.getMapSaveLoad().setNextToLoadByViewing(region);
                            }
                        }

                    }
                }

                prevRegion = region;
                if (i >= minViewX && i <= maxViewX && j >= minViewZ && j <= maxViewZ) {
                    MapTileChunk chunk = region == null ? null : region.getChunk(i & 7, j & 7);
                    boolean chunkIsVisible = chunk != null && chunk.getGlColorTexture() != -1;
                    if (wmHasCaveLayers && !chunkIsVisible && (!noCaveMaps || this.previousRenderedCaveLayer == Integer.MAX_VALUE)) {
                        MapRegion previousLayerRegion;
                        if (wmHasDimensionSwitch) {
                            previousLayerRegion = mapProcessor.getLeafMapRegion(this.previousRenderedCaveLayer, i >> 3, j >> 3, false);
                        } else {
                            previousLayerRegion = mapProcessor.getMapRegion(this.previousRenderedCaveLayer, i >> 3, j >> 3, false);
                        }
                        if (previousLayerRegion != null) {
                            MapTileChunk previousLayerChunk = previousLayerRegion.getChunk(i & 7, j & 7);
                            if (previousLayerChunk != null && previousLayerChunk.getGlColorTexture() != -1) {
                                region = previousLayerRegion;
                                chunk = previousLayerChunk;
                                chunkIsVisible = true;
                            }
                        }
                    }
                    if (chunkIsVisible) {
                        this.bumpLoadedRegion(mapProcessor, region, wmHasCaveLayers);
                        int drawX = ((chunk.getX() - chunkX) << 6) - (tileX << 4) - insideX;
                        int drawZ = ((chunk.getZ() - chunkZ) << 6) - (tileZ << 4) - insideZ;
                        if (transparentMinimapBackground.getValue()) {
                            GuiHelper.drawMMBackground(drawX, drawZ, brightness, chunk);
                            GuiMap.setupTextureMatricesAndTextures(brightness);
                        }

                        this.bindMapTextureWithLighting(compatibilityVersion, brightness, chunk, zooming);
                        if (zooming && compatibilityVersion >= 12) {
                            GlStateManager.setActiveTexture(33984);
                            GL11.glTexParameteri(3553, 10240, 9729);
                        }

                        GL11.glTexParameterf(3553, 33082, 0.0F);

                        if (compatibilityVersion < 7) {
                            GL14.glBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
                            GuiMap.renderTexturedModalRectWithLighting((float) drawX, (float) drawZ, 0, 0, 64.0F, 64.0F);
                        } else {
                            GuiMap.renderTexturedModalRectWithLighting((float) drawX, (float) drawZ, 64.0F, 64.0F);
                        }
                        if (XaeroPlusSettingRegistry.transparentMinimapBackground.getValue()) {
                            GuiHelper.finishMMSetup(compatibilityVersion, brightness, chunk, zooming);
                        }
                        if (zooming && compatibilityVersion >= 12) {
                            GlStateManager.setActiveTexture(33984);
                            GL11.glTexParameteri(3553, 10240, 9728);
                        }
                        GuiMap.restoreTextureStates();
                        if (compatibilityVersion >= 7) {
                            GL14.glBlendFuncSeparate(770, 771, 1, 771);
                        }
                        if (slimeChunks && !isDimensionSwitched) {
                            Long savedSeed = (Long) seedsUsed.get(chunk);
                            boolean newSeed = seed == null && savedSeed != null || seed != null && !seed.equals(savedSeed);
                            if (newSeed) {
                                seedsUsed.put(chunk, seed);
                            }
                            final List<GuiHelper.Rect> rects = new ArrayList<>(32);
                            for (int t = 0; t < 16; ++t) {
                                if (newSeed || (chunk.getTileGridsCache()[t % 4][t / 4] & 1) == 0) {
                                    chunk.getTileGridsCache()[t % 4][t / 4] = (byte) (1 | (MinimapTile.isSlimeChunk(this.modMain.getSettings(), chunk.getX() * 4 + t % 4, chunk.getZ() * 4 + t / 4, seed) ? 2 : 0));
                                }

                                if ((chunk.getTileGridsCache()[t % 4][t / 4] & 2) != 0) {
                                    final float left = drawX + 16 * (t % 4);
                                    final float top = drawZ + 16 * (t / 4);
                                    final float right = left + 16;
                                    final float bottom = top + 16;
                                    rects.add(new GuiHelper.Rect(left, top, right, bottom));
                                }
                            }
                            GuiHelper.drawRectList(rects, -2142047936);
                        }
                        if (XaeroPlusSettingRegistry.newChunksEnabledSetting.getValue()) {
                            final NewChunks newChunks = ModuleManager.getModule(NewChunks.class);
                            GuiHelper.drawMMHighlights(
                                (x, z) -> newChunks.isNewChunk(x, z, Globals.getCurrentDimensionId()),
                                drawX,
                                drawZ,
                                chunk.getX() << 2,
                                chunk.getZ() << 2,
                                newChunks.getNewChunksColor());
                        }
                        if (XaeroPlusSettingRegistry.portalSkipDetectionEnabledSetting.getValue() && XaeroPlusSettingRegistry.newChunksEnabledSetting.getValue()) {
                            final PortalSkipDetection portalSkipDetection = ModuleManager.getModule(
                                PortalSkipDetection.class);
                            GuiHelper.drawMMHighlights(
                                portalSkipDetection::isPortalSkipChunk,
                                drawX,
                                drawZ,
                                chunk.getX() << 2,
                                chunk.getZ() << 2,
                                portalSkipDetection.getPortalSkipChunksColor());
                        }
                        if (XaeroPlusSettingRegistry.portalsEnabledSetting.getValue()) {
                            final Portals portalModule = ModuleManager.getModule(Portals.class);
                            GuiHelper.drawMMHighlights(
                                (x, z) -> portalModule.isPortalChunk(x, z, Globals.getCurrentDimensionId()),
                                drawX,
                                drawZ,
                                chunk.getX() << 2,
                                chunk.getZ() << 2,
                                portalModule.getPortalsColor());
                        }
                        if (XaeroPlusSettingRegistry.wdlEnabledSetting.getValue()
                            && WDLHelper.isWdlPresent()
                            && WDLHelper.isDownloading()
                            && !isDimensionSwitched) {
                            final Set<Long> wdlSavedChunksWithCache = WDLHelper.getSavedChunksWithCache();
                            GuiHelper.drawMMHighlights(
                                (x, z) -> wdlSavedChunksWithCache.contains(ChunkUtils.chunkPosToLong(x, z)),
                                drawX,
                                drawZ,
                                chunk.getX() << 2,
                                chunk.getZ() << 2,
                                WDLHelper.getWdlColor()
                            );
                        }
                        if (compatibilityVersion >= 6) {
                            GuiMap.setupTextures(brightness);
                        }

                        if (compatibilityVersion >= 7) {
                            GL14.glBlendFuncSeparate(1, 0, 0, 1);
                            GlStateManager.enableBlend();
                        }

                        if (compatibilityVersion < 7) {
                            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                            GlStateManager.enableBlend();
                        }
                    }
                }
            }
        }
    }
}
