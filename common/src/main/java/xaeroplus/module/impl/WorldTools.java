package xaeroplus.module.impl;

import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.waste.of.time.storage.cache.HotCache;
import xaeroplus.Globals;
import xaeroplus.module.Module;
import xaeroplus.settings.Settings;
import xaeroplus.util.ChunkUtils;
import xaeroplus.util.ColorHelper;
import xaeroplus.util.WorldToolsHelper;

import static xaeroplus.util.ColorHelper.getColor;

public class WorldTools extends Module {
    private int worldToolsColor = getColor(0, 255, 0, 100);

    @Override
    public void onEnable() {
        if (!WorldToolsHelper.isWorldToolsPresent()) return;
        Globals.drawManager.registry().registerAsyncChunkHighlightProvider(
            this.getClass().getName(),
            this::getWindowedHighlightsSnapshot,
            this::getWorldToolsColor
        );
    }

    @Override
    public void onDisable() {
        Globals.drawManager.registry().unregisterChunkHighlightProvider(this.getClass().getName());
    }

    public boolean isChunkDownloaded(final int x, final int z, final ResourceKey<Level> dimension) {
        return WorldToolsHelper.isDownloading()
            && dimension == ChunkUtils.getActualDimension()
            && HotCache.INSTANCE.isChunkSaved(x, z);
    }

    public Long2LongMap getWindowedHighlightsSnapshot(final int windowRegionX, final int windowRegionZ, final int windowRegionSize, final ResourceKey<Level> dimension) {
        int minChunkX = ChunkUtils.regionCoordToChunkCoord(windowRegionX - windowRegionSize);
        int maxChunkX = ChunkUtils.regionCoordToChunkCoord(windowRegionX + windowRegionSize);
        int minChunkZ = ChunkUtils.regionCoordToChunkCoord(windowRegionZ - windowRegionSize);
        int maxChunkZ = ChunkUtils.regionCoordToChunkCoord(windowRegionZ + windowRegionSize);
        Long2LongMap chunks = new Long2LongOpenHashMap(8);
        for (int x = minChunkX; x <= maxChunkX; x++) {
            for (int z = minChunkZ; z <= maxChunkZ; z++) {
                if (isChunkDownloaded(x, z, dimension)) {
                    chunks.put(ChunkUtils.chunkPosToLong(x, z), 0);
                }
            }
        }
        return chunks;
    }

    public int getWorldToolsColor() {
        return worldToolsColor;
    }

    public void setRgbColor(final int color) {
        worldToolsColor = ColorHelper.getColorWithAlpha(color, Settings.REGISTRY.worldToolsAlphaSetting.getAsInt());
    }

    public void setAlpha(final double alpha) {
        worldToolsColor = ColorHelper.getColorWithAlpha(worldToolsColor, (int) alpha);
    }
}
