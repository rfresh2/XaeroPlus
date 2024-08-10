package xaeroplus.module.impl;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.waste.of.time.storage.cache.HotCache;
import xaeroplus.Globals;
import xaeroplus.feature.render.ChunkHighlightProvider;
import xaeroplus.feature.render.ColorHelper;
import xaeroplus.module.Module;
import xaeroplus.settings.XaeroPlusSettingRegistry;
import xaeroplus.util.ChunkUtils;
import xaeroplus.util.WorldToolsHelper;

import static xaeroplus.feature.render.ColorHelper.getColor;

public class WorldTools extends Module {

    private int worldToolsColor = getColor(0, 255, 0, 100);

    @Override
    public void onEnable() {
        if (!WorldToolsHelper.isWorldToolsPresent()) return;
        Globals.drawManager.registerChunkHighlightProvider(
            this.getClass(),
            new ChunkHighlightProvider(
                this::getHighlights,
                this::getWorldToolsColor
            )
        );
    }

    @Override
    public void onDisable() {
        Globals.drawManager.unregister(this.getClass());
    }

    public boolean isChunkDownloaded(final int x, final int z, final ResourceKey<Level> dimension) {
        return WorldToolsHelper.isDownloading()
            && dimension == ChunkUtils.getActualDimension()
            && HotCache.INSTANCE.isChunkSaved(x, z);
    }

    public LongSet getHighlights(final int windowRegionX, final int windowRegionZ, final int windowRegionSize, final ResourceKey<Level> dimension) {
        int minChunkX = ChunkUtils.regionCoordToChunkCoord(windowRegionX - windowRegionSize);
        int maxChunkX = ChunkUtils.regionCoordToChunkCoord(windowRegionX + windowRegionSize);
        int minChunkZ = ChunkUtils.regionCoordToChunkCoord(windowRegionZ - windowRegionSize);
        int maxChunkZ = ChunkUtils.regionCoordToChunkCoord(windowRegionZ + windowRegionSize);
        LongSet chunks = new LongOpenHashSet();
        for (int x = minChunkX; x <= maxChunkX; x++) {
            for (int z = minChunkZ; z <= maxChunkZ; z++) {
                if (isChunkDownloaded(x, z, dimension)) {
                    chunks.add(ChunkUtils.chunkPosToLong(x, z));
                }
            }
        }
        return chunks;
    }

    public int getWorldToolsColor() {
        return worldToolsColor;
    }

    public void setRgbColor(final int color) {
        worldToolsColor = ColorHelper.getColorWithAlpha(color, (int) XaeroPlusSettingRegistry.worldToolsAlphaSetting.getValue());
    }

    public void setAlpha(final float alpha) {
        worldToolsColor = ColorHelper.getColorWithAlpha(worldToolsColor, (int) alpha);
    }
}
