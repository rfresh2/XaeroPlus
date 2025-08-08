package xaeroplus.module.impl;

import it.unimi.dsi.fastutil.longs.Long2LongMap;
import net.lenni0451.lambdaevents.EventHandler;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import xaeroplus.Globals;
import xaeroplus.event.ClientTickEvent;
import xaeroplus.feature.highlights.SavableHighlightCacheInstance;
import xaeroplus.feature.render.DrawFeatureFactory;
import xaeroplus.module.Module;
import xaeroplus.settings.Settings;
import xaeroplus.util.ChunkUtils;
import xaeroplus.util.ColorHelper;

public class Breadcrumbs extends Module {
    public final SavableHighlightCacheInstance breadcrumbsCache = new SavableHighlightCacheInstance("XaeroPlusBreadcrumbs");
    private int breadcrumbsColor = ColorHelper.getColor(0, 255, 0, 100);
    private int chunkRadius = 0;

    public void setDiskCache(final boolean disk) {
        breadcrumbsCache.setDiskCache(disk, isEnabled());
    }

    @EventHandler
    public void onTick(ClientTickEvent.Post event) {
        var dim = ChunkUtils.getActualDimension();
        var playerChunkX = ChunkUtils.actualPlayerChunkX();
        var playerChunkZ = ChunkUtils.actualPlayerChunkZ();

        for (int x = playerChunkX - chunkRadius; x <= playerChunkX + chunkRadius; x++) {
            for (int z = playerChunkZ - chunkRadius; z <= playerChunkZ + chunkRadius; z++) {
                if (!breadcrumbsCache.get().isHighlighted(x, z, dim)) {
                    breadcrumbsCache.get().addHighlight(x, z);
                }
            }
        }
    }

    @Override
    public void onEnable() {
        Globals.drawManager.registry().register(
            DrawFeatureFactory.chunkHighlights(
                "Breadcrumbs",
                this::getHighlightsState,
                this::getBreadcrumbsColor,
                50
            )
        );
        breadcrumbsCache.onEnable();
    }

    @Override
    public void onDisable() {
        breadcrumbsCache.onDisable();
        Globals.drawManager.registry().unregister("Breadcrumbs");
    }

    public Long2LongMap getHighlightsState(final ResourceKey<Level> dimension) {
        return breadcrumbsCache.get().getCacheMap(dimension);
    }

    public int getBreadcrumbsColor() {
        return breadcrumbsColor;
    }

    public void setRgbColor(int color) {
        this.breadcrumbsColor = ColorHelper.getColorWithAlpha(color, Settings.REGISTRY.breadcrumbsOpacitySetting.getAsInt());
    }

    public void setAlpha(double alpha) {
        this.breadcrumbsColor = ColorHelper.getColorWithAlpha(breadcrumbsColor, (int) alpha);
    }

    public void setChunkRadius(double chunkRadius) {
        this.chunkRadius = (int) chunkRadius;
    }
}
