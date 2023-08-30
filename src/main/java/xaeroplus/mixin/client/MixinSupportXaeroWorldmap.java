package xaeroplus.mixin.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import xaero.common.IXaeroMinimap;
import xaero.common.XaeroMinimapSession;
import xaero.common.effect.Effects;
import xaero.common.graphics.renderer.multitexture.MultiTextureRenderTypeRenderer;
import xaero.common.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider;
import xaero.common.minimap.render.MinimapRendererHelper;
import xaero.common.minimap.waypoints.WaypointWorld;
import xaero.common.mods.SupportXaeroWorldmap;
import xaero.map.MapProcessor;
import xaero.map.WorldMap;
import xaero.map.WorldMapSession;
import xaero.map.graphics.CustomRenderTypes;
import xaero.map.graphics.shader.MapShaders;
import xaero.map.misc.Misc;
import xaero.map.region.LeveledRegion;
import xaero.map.region.MapRegion;
import xaero.map.region.MapTileChunk;
import xaeroplus.module.ModuleManager;
import xaeroplus.module.impl.NewChunks;
import xaeroplus.module.impl.OldChunks;
import xaeroplus.module.impl.PortalSkipDetection;
import xaeroplus.module.impl.Portals;
import xaeroplus.settings.XaeroPlusSettingRegistry;
import xaeroplus.util.CustomDimensionMapProcessor;
import xaeroplus.util.CustomSupportXaeroWorldMap;
import xaeroplus.util.GuiHelper;
import xaeroplus.util.Shared;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.IntConsumer;

import static xaeroplus.util.ChunkUtils.getPlayerX;
import static xaeroplus.util.ChunkUtils.getPlayerZ;

@Mixin(value = SupportXaeroWorldmap.class, remap = false)
public abstract class MixinSupportXaeroWorldmap implements CustomSupportXaeroWorldMap {
    @Shadow
    public int compatibilityVersion;
    @Shadow
    private IXaeroMinimap modMain;
    @Shadow
    private int destinationCaving;
    @Shadow
    private long lastDestinationCavingSwitch;
    @Shadow
    private int previousRenderedCaveLayer;
    @Shadow
    private int lastRenderedCaveLayer;
    private ArrayList<MapRegion> regionBuffer;
    @Final
    @Shadow
    private static HashMap<MapTileChunk, Long> seedsUsed;

    @Shadow
    public abstract float getMinimapBrightness();
    @Shadow
    public abstract void prepareMapTexturedRect(
            Matrix4f matrix,
            float x,
            float y,
            int textureX,
            int textureY,
            float width,
            float height,
            MapTileChunk chunk,
            MultiTextureRenderTypeRenderer noLightRenderer,
            MultiTextureRenderTypeRenderer withLightrenderer,
            MinimapRendererHelper helper
    );
    @Shadow
    public abstract void renderSlimeChunks(
        MapTileChunk chunk, Long seed, int drawX, int drawZ, MatrixStack matrixStack, MinimapRendererHelper helper, VertexConsumer overlayBufferBuilder
    );

    @Override
    public void drawMinimapWithDrawContext(
            DrawContext guiGraphics,
            XaeroMinimapSession minimapSession,
            MatrixStack matrixStack,
            MinimapRendererHelper helper,
            int xFloored,
            int zFloored,
            int minViewX,
            int minViewZ,
            int maxViewX,
            int maxViewZ,
            boolean zooming,
            double zoom,
            VertexConsumer overlayBufferBuilder,
            MultiTextureRenderTypeRendererProvider multiTextureRenderTypeRenderers) {
        WorldMapSession worldmapSession = WorldMapSession.getCurrentSession();
        if (worldmapSession != null) {
            MapProcessor mapProcessor = worldmapSession.getMapProcessor();
            synchronized(mapProcessor.renderThreadPauseSync) {
                if (!mapProcessor.isRenderingPaused()) {
                    if (mapProcessor.getCurrentDimension() == null) {
                        return;
                    }
                    int compatibilityVersion = this.compatibilityVersion;
                    String worldString = mapProcessor.getCurrentWorldId();
                    if (worldString == null) {
                        return;
                    }

                    MapShaders.ensureShaders();
                    int mapX = xFloored >> 4;
                    int mapZ = zFloored >> 4;
                    int chunkX = mapX >> 2;
                    int chunkZ = mapZ >> 2;
                    int tileX = mapX & 3;
                    int tileZ = mapZ & 3;
                    int insideX = xFloored & 15;
                    int insideZ = zFloored & 15;
                    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                    RenderSystem.enableBlend();
                    final int scaledSize = Shared.minimapScalingFactor * 4;
                    int minX = (mapX >> 2) - scaledSize;
                    int maxX = (mapX >> 2) + scaledSize;
                    int minZ = (mapZ >> 2) - scaledSize;
                    int maxZ = (mapZ >> 2) + scaledSize;
                    int globalRegionCacheHashCode = WorldMap.settings.getRegionCacheHashCode();
                    boolean reloadEverything = WorldMap.settings.reloadEverything;
                    int globalReloadVersion = WorldMap.settings.reloadVersion;
                    boolean slimeChunks = this.modMain.getSettings().getSlimeChunks(minimapSession.getWaypointsManager());
                    mapProcessor.updateCaveStart();
                    int renderedCaveLayer = mapProcessor.getCurrentCaveLayer();
                    int globalCaveStart = mapProcessor.getMapWorld().getDimension(Shared.customDimensionId).getLayeredMapRegions().getLayer(renderedCaveLayer).getCaveStart();
                    int globalCaveDepth = WorldMap.settings.caveModeDepth;
                    float brightness = this.getMinimapBrightness();
                    if (renderedCaveLayer != this.lastRenderedCaveLayer) {
                        this.previousRenderedCaveLayer = this.lastRenderedCaveLayer;
                    }

                    PlayerEntity player = MinecraftClient.getInstance().player;
                    boolean noCaveMaps = player.hasStatusEffect(Effects.NO_CAVE_MAPS) || player.hasStatusEffect(Effects.NO_CAVE_MAPS_HARMFUL);
                    boolean playerIsMoving = player.prevX != player.getX() || player.prevY != player.getY() || player.prevZ != player.getZ();
                    boolean shouldRequestLoading = true;
                    Object nextToLoadObj = null;
                    shouldRequestLoading = false;
                    LeveledRegion<?> nextToLoad = mapProcessor.getMapSaveLoad().getNextToLoadByViewing();
                    Object var76 = nextToLoad;
                    if (nextToLoad != null) {
                        synchronized(nextToLoad) {
                            if (!nextToLoad.reloadHasBeenRequested()
                                    && !nextToLoad.hasRemovableSourceData()
                                    && (!(nextToLoad instanceof MapRegion) || !((MapRegion)nextToLoad).isRefreshing())) {
                                shouldRequestLoading = true;
                            }
                        }
                    } else {
                        shouldRequestLoading = true;
                    }

                    this.regionBuffer.clear();
                    int comparisonChunkX = (MathHelper.floor(getPlayerX()) >> 4) - 16;
                    int comparisonChunkZ = (MathHelper.floor(getPlayerZ()) >> 4) - 16;
                    LeveledRegion.setComparison(comparisonChunkX, comparisonChunkZ, 0, comparisonChunkX, comparisonChunkZ);
                    MultiTextureRenderTypeRenderer mapWithLightRenderer = null;
                    MultiTextureRenderTypeRenderer mapNoLightRenderer = null;
                    Runnable finalizer = null;
                    IntConsumer binder;
                    IntConsumer shaderBinder;
                    if (zooming) {
                        binder = t -> {
                            MultiTextureRenderTypeRendererProvider.defaultTextureBind(t);
                            GL11.glTexParameteri(3553, 10240, 9729);
                        };
                        shaderBinder = t -> {
                            RenderSystem.setShaderTexture(0, t);
                            MultiTextureRenderTypeRendererProvider.defaultTextureBind(t);
                            GL11.glTexParameteri(3553, 10240, 9729);
                        };
                        finalizer = () -> GL11.glTexParameteri(3553, 10240, 9728);
                    } else {
                        binder = MultiTextureRenderTypeRendererProvider::defaultTextureBind;
                        shaderBinder = t -> RenderSystem.setShaderTexture(0, t);
                    }

                    mapWithLightRenderer = multiTextureRenderTypeRenderers.getRenderer(shaderBinder, binder, finalizer, CustomRenderTypes.MAP);
                    mapNoLightRenderer = multiTextureRenderTypeRenderers.getRenderer(shaderBinder, binder, finalizer, CustomRenderTypes.MAP);
                    WaypointWorld world = minimapSession.getWaypointsManager().getAutoWorld();
                    Long seed = slimeChunks && world != null ? this.modMain.getSettings().getSlimeChunksSeed(world.getFullId()) : null;
                    this.renderChunksWithDrawContext(
                        guiGraphics,
                        matrixStack,
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
                        mapWithLightRenderer,
                        mapNoLightRenderer,
                        helper,
                        overlayBufferBuilder
                    );

                    MapShaders.WORLD_MAP.setBrightness(brightness);
                    MapShaders.WORLD_MAP.setWithLight(true);
                    multiTextureRenderTypeRenderers.draw(mapWithLightRenderer);
                    MapShaders.WORLD_MAP.setWithLight(false);
                    multiTextureRenderTypeRenderers.draw(mapNoLightRenderer);
                    GL14.glBlendFuncSeparate(770, 771, 1, 0);
                    RenderSystem.disableBlend();
                    this.lastRenderedCaveLayer = renderedCaveLayer;
                    int toRequest = 1;
                    int counter = 0;

                    for(int i = 0; i < this.regionBuffer.size() && counter < toRequest; ++i) {
                        MapRegion region = (MapRegion)this.regionBuffer.get(i);
                        if (region != var76 || this.regionBuffer.size() <= 1) {
                            synchronized(region) {
                                if (!region.reloadHasBeenRequested()
                                    && !region.recacheHasBeenRequested()
                                    && (!(region instanceof MapRegion) || !region.isRefreshing())
                                    && (region.getLoadState() == 0 || region.getLoadState() == 4 || region.getLoadState() == 2 && region.isBeingWritten())) {
                                    if (region.getLoadState() == 2) {
                                        region.requestRefresh(mapProcessor);
                                    } else {
                                        mapProcessor.getMapSaveLoad().requestLoad(region, "Minimap sorted", false);
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

    @Override
    public void renderChunksWithDrawContext(
        DrawContext guiGraphics,
        MatrixStack matrixStack,
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
        MultiTextureRenderTypeRenderer mapWithLightRenderer,
        MultiTextureRenderTypeRenderer mapNoLightRenderer,
        MinimapRendererHelper helper,
        VertexConsumer overlayBufferBuilder
    ) {
        final boolean isDimensionSwitched = Shared.customDimensionId != MinecraftClient.getInstance().world.getRegistryKey();
        MapRegion prevRegion = null;
        Tessellator bgTesselator = Tessellator.getInstance();
        BufferBuilder bgBufferBuilder = bgTesselator.getBuffer();
        if (XaeroPlusSettingRegistry.transparentMinimapBackground.getValue())
            bgBufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        Matrix4f matrix = matrixStack.peek().getPositionMatrix();

        for(int i = minX; i <= maxX; ++i) {
            for(int j = minZ; j <= maxZ; ++j) {
                MapRegion region = ((CustomDimensionMapProcessor) mapProcessor).getMapRegionCustomDimension(
                    renderedCaveLayer,
                    i >> 3,
                    j >> 3,
                    ((CustomDimensionMapProcessor) mapProcessor).regionExistsCustomDimension(renderedCaveLayer, i >> 3, j >> 3, Shared.customDimensionId),
                    Shared.customDimensionId
                );
                if (region != null && region != prevRegion) {
                    synchronized(region) {
                        int regionHashCode = region.getCacheHashCode();
                        int regionReloadVersion = region.getReloadVersion();
                        if (shouldRequestLoading
                            && !region.recacheHasBeenRequested()
                            && !region.reloadHasBeenRequested()
                            && (!(region instanceof MapRegion) || !region.isRefreshing())
                            && (
                            region.getLoadState() == 0
                                || (region.getLoadState() == 4 || region.getLoadState() == 2 && region.isBeingWritten())
                                && (
                                reloadEverything && regionReloadVersion != globalReloadVersion
                                    || regionHashCode != globalRegionCacheHashCode
                                    || !playerIsMoving && region.caveStartOutdated(globalCaveStart, globalCaveDepth)
                                    || region.getVersion() != mapProcessor.getGlobalVersion()
                                    || (region.isMetaLoaded() || region.getLoadState() != 0 || !region.hasHadTerrain())
                                    && region.getHighlightsHash()
                                    != region.getDim().getHighlightHandler().getRegionHash(region.getRegionX(), region.getRegionZ())
                                    || region.getLoadState() != 2 && region.shouldCache()
                            )
                        )
                            && !this.regionBuffer.contains(region)) {
                            region.calculateSortingChunkDistance();
                            Misc.addToListOfSmallest(10, this.regionBuffer, region);
                        }
                    }
                }

                prevRegion = region;
                if (i >= minViewX && i <= maxViewX && j >= minViewZ && j <= maxViewZ) {
                    MapTileChunk chunk = region == null ? null : region.getChunk(i & 7, j & 7);
                    boolean chunkIsVisible = chunk != null && chunk.getLeafTexture().getGlColorTexture() != -1;
                    if (!chunkIsVisible && (!noCaveMaps || this.previousRenderedCaveLayer == Integer.MAX_VALUE)) {
                        MapRegion previousLayerRegion = ((CustomDimensionMapProcessor) mapProcessor).getMapRegionCustomDimension(this.previousRenderedCaveLayer, i >> 3, j >> 3, false, Shared.customDimensionId);
                        if (previousLayerRegion != null) {
                            MapTileChunk previousLayerChunk = previousLayerRegion.getChunk(i & 7, j & 7);
                            if (previousLayerChunk != null && previousLayerChunk.getLeafTexture().getGlColorTexture() != -1) {
                                region = previousLayerRegion;
                                chunk = previousLayerChunk;
                                chunkIsVisible = true;
                            }
                        }
                    }

                    if (chunkIsVisible) {
                        if (!mapProcessor.isUploadingPaused() && region.isLoaded()) {
                            mapProcessor.getMapWorld().getDimension(Shared.customDimensionId).getLayeredMapRegions().bumpLoadedRegion(region);
                        }
                        int drawX = ((chunk.getX() - chunkX) << 6) - (tileX << 4) - insideX;
                        int drawZ = ((chunk.getZ() - chunkZ) << 6) - (tileZ << 4) - insideZ;
                        if (XaeroPlusSettingRegistry.transparentMinimapBackground.getValue()) {
                            GuiHelper.addMMBackgroundToBuffer(guiGraphics.getMatrices().peek().getPositionMatrix(),
                                                              bgBufferBuilder,
                                                              drawX,
                                                              drawZ,
                                                              chunk);
                        }
                        GL11.glTexParameterf(3553, 33082, 0.0F);

                        this.prepareMapTexturedRect(
                            matrix, (float)drawX, (float)drawZ, 0, 0, 64.0F, 64.0F, chunk, mapNoLightRenderer, mapWithLightRenderer, helper
                        );
                        if (slimeChunks && !isDimensionSwitched) {
                            this.renderSlimeChunks(chunk, seed, drawX, drawZ, matrixStack, helper, overlayBufferBuilder);
                        }
                        if (XaeroPlusSettingRegistry.newChunksEnabledSetting.getValue()) {
                            NewChunks newChunks = ModuleManager.getModule(NewChunks.class);
                            for (int t = 0; t < 16; ++t) {
                                final int chunkPosX = chunk.getX() * 4 + t % 4;
                                final int chunkPosZ = chunk.getZ() * 4 + t / 4;
                                int color = newChunks.getNewChunksColor();
                                if (newChunks.isNewChunk(chunkPosX, chunkPosZ, Shared.customDimensionId)) {
                                    final float left = drawX + 16 * (t % 4);
                                    final float top = drawZ + 16 * (t / 4);
                                    helper.addColoredRectToExistingBuffer(matrixStack.peek().getPositionMatrix(), overlayBufferBuilder, left, top, 16, 16, color);
                                }
                            }
                        }
                        if (XaeroPlusSettingRegistry.oldChunksEnabledSetting.getValue()) {
                            OldChunks oldChunks = ModuleManager.getModule(OldChunks.class);
                            for (int t = 0; t < 16; ++t) {
                                final int chunkPosX = chunk.getX() * 4 + t % 4;
                                final int chunkPosZ = chunk.getZ() * 4 + t / 4;
                                int color = oldChunks.getOldChunksColor();
                                if (oldChunks.isOldChunk(chunkPosX, chunkPosZ, Shared.customDimensionId)) {
                                    final float left = drawX + 16 * (t % 4);
                                    final float top = drawZ + 16 * (t / 4);
                                    helper.addColoredRectToExistingBuffer(matrixStack.peek().getPositionMatrix(), overlayBufferBuilder, left, top, 16, 16, color);
                                }
                            }
                        }
                        if (XaeroPlusSettingRegistry.portalsEnabledSetting.getValue()) {
                            Portals portals = ModuleManager.getModule(Portals.class);
                            for (int t = 0; t < 16; ++t) {
                                final int chunkPosX = chunk.getX() * 4 + t % 4;
                                final int chunkPosZ = chunk.getZ() * 4 + t / 4;
                                int color = portals.getPortalsColor();
                                if (portals.isPortalChunk(chunkPosX, chunkPosZ, Shared.customDimensionId)) {
                                    final float left = drawX + 16 * (t % 4);
                                    final float top = drawZ + 16 * (t / 4);
                                    helper.addColoredRectToExistingBuffer(matrixStack.peek().getPositionMatrix(), overlayBufferBuilder, left, top, 16, 16, color);
                                }
                            }
                        }
                        if (XaeroPlusSettingRegistry.portalSkipDetectionEnabledSetting.getValue() && XaeroPlusSettingRegistry.newChunksEnabledSetting.getValue()) {
                            PortalSkipDetection portalSkipDetection = ModuleManager.getModule(PortalSkipDetection.class);
                            for (int t = 0; t < 16; ++t) {
                                final int chunkPosX = chunk.getX() * 4 + t % 4;
                                final int chunkPosZ = chunk.getZ() * 4 + t / 4;
                                int color = portalSkipDetection.getPortalSkipChunksColor();
                                if (portalSkipDetection.isPortalSkipChunk(chunkPosX, chunkPosZ)) {
                                    final float left = drawX + 16 * (t % 4);
                                    final float top = drawZ + 16 * (t / 4);
                                    helper.addColoredRectToExistingBuffer(matrixStack.peek().getPositionMatrix(), overlayBufferBuilder, left, top, 16, 16, color);
                                }
                            }
                        }
                    }
                }
            }
        }
        if (XaeroPlusSettingRegistry.transparentMinimapBackground.getValue()) bgTesselator.draw();
    }
}
