package xaeroplus.module.impl;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import xaeroplus.Globals;
import xaeroplus.feature.render.Line;
import xaeroplus.module.Module;
import xaeroplus.settings.Settings;
import xaeroplus.util.ChunkUtils;
import xaeroplus.util.ColorHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class SpawnChunksBase extends Module {
    final String entityProcessingId = getClass().getName() + "$EntityProcessing";
    final String redstoneProcessingId = getClass().getName() + "$RedstoneProcessing";
    final String lazyChunkId = getClass().getName() + "$LazyChunk";
    final String outerChunksId = getClass().getName() + "$OuterChunks";
    final List<Line> entityProcessingCache = new ArrayList<>();
    final List<Line> redstoneProcessingCache = new ArrayList<>();
    final List<Line> lazyChunksCache = new ArrayList<>();
    final List<Line> outerChunksCache = new ArrayList<>();
    int alpha = 204;
    int entityProcessingColor = ColorHelper.getColor(0, 255, 0, alpha);
    int redstoneProcessingColor = ColorHelper.getColor(255, 0, 0, alpha);
    int lazyChunksColor = ColorHelper.getColor(0, 0, 255, alpha);
    int outerChunksColor = ColorHelper.getColor(255, 255, 0, alpha);
    float lineWidth = 0.1f;

    public abstract ResourceKey<Level> dimension();

    abstract int getSpawnRadius();

    abstract long getSpawnChunkPos();

    void onClientTick() {
        updateCaches();
    }

    @Override
    public void onEnable() {
        Globals.drawManager.registry().registerLineProvider(
            entityProcessingId,
            this::entityProcessing,
            this::entityProcessingColor,
            this::getLineWidth,
            50
        );
        Globals.drawManager.registry().registerLineProvider(
            redstoneProcessingId,
            this::redstoneProcessing,
            this::redstoneProcessingColor,
            this::getLineWidth,
            50
        );
        Globals.drawManager.registry().registerLineProvider(
            lazyChunkId,
            this::lazyChunks,
            this::lazyChunksColor,
            this::getLineWidth,
            50
        );
        Globals.drawManager.registry().registerLineProvider(
            outerChunksId,
            this::outerChunks,
            this::outerChunksColor,
            this::getLineWidth,
            50
        );
    }

    @Override
    public void onDisable() {
        Globals.drawManager.registry().unregisterLineProvider(entityProcessingId);
        Globals.drawManager.registry().unregisterLineProvider(redstoneProcessingId);
        Globals.drawManager.registry().unregisterLineProvider(lazyChunkId);
        Globals.drawManager.registry().unregisterLineProvider(outerChunksId);
    }

    public List<Line> entityProcessing(final int windowRegionX, final int windowRegionZ, final int windowRegionSize, ResourceKey<Level> dimension) {
        if (dimension != dimension()) return Collections.emptyList();
        return entityProcessingCache;
    }

    public List<Line> redstoneProcessing(final int windowRegionX, final int windowRegionZ, final int windowRegionSize, ResourceKey<Level> dimension) {
        if (dimension != dimension()) return Collections.emptyList();
        return redstoneProcessingCache;
    }

    public List<Line> lazyChunks(final int windowRegionX, final int windowRegionZ, final int windowRegionSize, ResourceKey<Level> dimension) {
        if (dimension != dimension()) return Collections.emptyList();
        return lazyChunksCache;
    }

    public List<Line> outerChunks(final int windowRegionX, final int windowRegionZ, final int windowRegionSize, ResourceKey<Level> dimension) {
        if (dimension != dimension()) return Collections.emptyList();
        return outerChunksCache;
    }

    int entityProcessingColor() {
        return entityProcessingColor;
    }

    int redstoneProcessingColor() {
        return redstoneProcessingColor;
    }

    int lazyChunksColor() {
        return lazyChunksColor;
    }

    int outerChunksColor() {
        return outerChunksColor;
    }

    public void setEntityProcessingColor(final int color) {
        entityProcessingColor = ColorHelper.getColorWithAlpha(color, alpha);
    }

    public void setRedstoneProcessingColor(final int color) {
        redstoneProcessingColor = ColorHelper.getColorWithAlpha(color, alpha);
    }

    public void setLazyChunksColor(final int color) {
        lazyChunksColor = ColorHelper.getColorWithAlpha(color, alpha);
    }

    public void setOuterChunksColor(final int color) {
        outerChunksColor = ColorHelper.getColorWithAlpha(color, alpha);
    }

    public void setAlpha(final int alpha) {
        this.alpha = alpha;
        setEntityProcessingColor(entityProcessingColor);
        setRedstoneProcessingColor(redstoneProcessingColor);
        setLazyChunksColor(lazyChunksColor);
        setOuterChunksColor(outerChunksColor);
    }

    public float getLineWidth() {
        return lineWidth;
    }

    public void setLineWidth(final float lineWidth) {
        this.lineWidth = lineWidth;
    }

    void updateCaches() {
        int spawnChunkRadius = getSpawnRadius();
        clearCaches();
        var level = mc.level;
        if (level == null) return;
        long spawnChunkPosLong = getSpawnChunkPos();
        int spawnChunkX = ChunkUtils.longToChunkX(spawnChunkPosLong);
        int spawnChunkZ = ChunkUtils.longToChunkZ(spawnChunkPosLong);
        int lazyRadius = spawnChunkRadius + 1;
        int redstoneRadius = spawnChunkRadius;
        int entityProcessingRadius = spawnChunkRadius - 1;
        int worldGenRadius = lazyRadius + 11;

        populateCache(entityProcessingCache, spawnChunkX, spawnChunkZ, entityProcessingRadius);
        if (Settings.REGISTRY.spawnChunksRedstoneProcessingEnabled.get())
            populateCache(redstoneProcessingCache, spawnChunkX, spawnChunkZ, redstoneRadius);
        populateCache(lazyChunksCache, spawnChunkX, spawnChunkZ, lazyRadius);
        if (Settings.REGISTRY.spawnChunksOuterChunksEnabled.get())
            populateCache(outerChunksCache, spawnChunkX, spawnChunkZ, worldGenRadius);
    }

    void populateCache(List<Line> cache, int centerX, int centerZ, int radius) {
        int minChunkX = centerX - radius;
        int maxChunkX = centerX + radius;
        int minChunkZ = centerZ - radius;
        int maxChunkZ = centerZ + radius;
        int minBlockX = ChunkUtils.chunkCoordToCoord(minChunkX);
        int maxBlockX = ChunkUtils.chunkCoordToCoord(maxChunkX + 1);
        int minBlockZ = ChunkUtils.chunkCoordToCoord(minChunkZ);
        int maxBlockZ = ChunkUtils.chunkCoordToCoord(maxChunkZ + 1);
        cache.add(new Line(minBlockX, minBlockZ, maxBlockX, minBlockZ));
        cache.add(new Line(minBlockX, minBlockZ, minBlockX, maxBlockZ));
        cache.add(new Line(maxBlockX, minBlockZ, maxBlockX, maxBlockZ));
        cache.add(new Line(minBlockX, maxBlockZ, maxBlockX, maxBlockZ));
    }

    void clearCaches() {
        entityProcessingCache.clear();
        redstoneProcessingCache.clear();
        lazyChunksCache.clear();
        outerChunksCache.clear();
    }
}
