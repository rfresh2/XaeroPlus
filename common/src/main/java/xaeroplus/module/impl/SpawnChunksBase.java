package xaeroplus.module.impl;

import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import xaeroplus.Globals;
import xaeroplus.module.Module;
import xaeroplus.settings.Settings;
import xaeroplus.util.ChunkUtils;
import xaeroplus.util.ColorHelper;

public abstract class SpawnChunksBase extends Module {
    final String entityProcessingId = getClass().getName() + "$EntityProcessing";
    final String redstoneProcessingId = getClass().getName() + "$RedstoneProcessing";
    final String lazyChunkId = getClass().getName() + "$LazyChunk";
    final String outerChunksId = getClass().getName() + "$OuterChunks";
    final Long2LongMap entityProcessingCache = new Long2LongOpenHashMap();
    final Long2LongMap redstoneProcessingCache = new Long2LongOpenHashMap();
    final Long2LongMap lazyChunksCache = new Long2LongOpenHashMap();
    final Long2LongMap outerChunksCache = new Long2LongOpenHashMap();
    int entityProcessingColor = ColorHelper.getColor(0, 255, 0, 100);
    int redstoneProcessingColor = ColorHelper.getColor(255, 0, 0, 100);
    int lazyChunksColor = ColorHelper.getColor(0, 0, 255, 100);
    int outerChunksColor = ColorHelper.getColor(255, 255, 0, 100);

    public abstract Long2LongMap entityProcessing(ResourceKey<Level> dimension);

    public abstract Long2LongMap redstoneProcessing(ResourceKey<Level> dimension);

    public abstract Long2LongMap lazyChunks(ResourceKey<Level> dimension);

    public abstract Long2LongMap outerChunks(ResourceKey<Level> dimension);

    abstract int getSpawnRadius();

    abstract long getSpawnChunkPos();

    void onClientTick() {
        updateCaches();
    }

    @Override
    public void onEnable() {
        Globals.drawManager.registry().registerDirectChunkHighlightProvider(
            entityProcessingId,
            true,
            this::entityProcessing,
            this::entityProcessingColor
        );
        Globals.drawManager.registry().registerDirectChunkHighlightProvider(
            redstoneProcessingId,
            true,
            this::redstoneProcessing,
            this::redstoneProcessingColor
        );
        Globals.drawManager.registry().registerDirectChunkHighlightProvider(
            lazyChunkId,
            true,
            this::lazyChunks,
            this::lazyChunksColor
        );
        Globals.drawManager.registry().registerDirectChunkHighlightProvider(
            outerChunksId,
            true,
            this::outerChunks,
            this::outerChunksColor
        );
    }

    @Override
    public void onDisable() {
        Globals.drawManager.registry().unregisterChunkHighlightProvider(entityProcessingId);
        Globals.drawManager.registry().unregisterChunkHighlightProvider(redstoneProcessingId);
        Globals.drawManager.registry().unregisterChunkHighlightProvider(lazyChunkId);
        Globals.drawManager.registry().unregisterChunkHighlightProvider(outerChunksId);
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
        entityProcessingColor = ColorHelper.getColorWithAlpha(color, Settings.REGISTRY.spawnChunksAlphaSetting.getAsInt());
    }

    public void setRedstoneProcessingColor(final int color) {
        redstoneProcessingColor = ColorHelper.getColorWithAlpha(color, Settings.REGISTRY.spawnChunksAlphaSetting.getAsInt());
    }

    public void setLazyChunksColor(final int color) {
        lazyChunksColor = ColorHelper.getColorWithAlpha(color, Settings.REGISTRY.spawnChunksAlphaSetting.getAsInt());
    }

    public void setOuterChunksColor(final int color) {
        outerChunksColor = ColorHelper.getColorWithAlpha(color, Settings.REGISTRY.spawnChunksAlphaSetting.getAsInt());
    }

    public void setAlpha(final int alpha) {
        setEntityProcessingColor(entityProcessingColor);
        setRedstoneProcessingColor(redstoneProcessingColor);
        setLazyChunksColor(lazyChunksColor);
        setOuterChunksColor(outerChunksColor);
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
            populateCache(redstoneProcessingCache, spawnChunkX, spawnChunkZ, redstoneRadius, entityProcessingCache);
        populateCache(lazyChunksCache, spawnChunkX, spawnChunkZ, lazyRadius, entityProcessingCache, redstoneProcessingCache);
        if (Settings.REGISTRY.spawnChunksOuterChunksEnabled.get())
            populateCache(outerChunksCache, spawnChunkX, spawnChunkZ, worldGenRadius, entityProcessingCache, redstoneProcessingCache, lazyChunksCache);
    }

    void populateCache(Long2LongMap cache, int centerX, int centerZ, int radius, Long2LongMap... except) {
        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                long pos = ChunkUtils.chunkPosToLong(x, z);
                boolean skip = false;
                for (Long2LongMap ex : except) {
                    if (ex.containsKey(pos)) {
                        skip = true;
                        break;
                    }
                }
                if (!skip) cache.put(pos, 0);
            }
        }
    }

    void clearCaches() {
        entityProcessingCache.clear();
        redstoneProcessingCache.clear();
        lazyChunksCache.clear();
        outerChunksCache.clear();
    }
}
