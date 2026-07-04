package xaeroplus.feature.highlights;

import net.lenni0451.lambdaevents.EventHandler;
import net.minecraft.client.Minecraft;
import xaeroplus.XaeroPlus;
import xaeroplus.event.ClientStoppingEvent;
import xaeroplus.event.ClientTickEvent;
import xaeroplus.event.XaeroWorldChangeEvent;

public class SavableHighlightCacheInstance {
    private ChunkHighlightCache cache;
    private final String dbName;

    public SavableHighlightCacheInstance(String dbName) {
        this.dbName = dbName;
        // always starts as a local cache
        // to switch to disk cache call setDiskCache
        this.cache = new ChunkHighlightLocalCache();
    }

    public ChunkHighlightCache get() {
        return cache;
    }

    /**
     * These must be called when the owning module is enabled/disabled
     */

    public synchronized void onEnable() {
        XaeroPlus.EVENT_BUS.register(this);
        cache.onEnable();
    }

    public synchronized void onDisable() {
        cache.onDisable();
        XaeroPlus.EVENT_BUS.unregister(this);
    }

    public synchronized void setDiskCache(final boolean disk, final boolean enabled) {
        try {
            cache.onDisable();
            if (cache instanceof ChunkHighlightSavingCache savingCache) {
                savingCache.close();
            }
            if (disk) {
                cache = new ChunkHighlightSavingCache(dbName);
            } else {
                cache = new ChunkHighlightLocalCache();
            }
            if (enabled) {
                cache.onEnable();
            }
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Error setting {} cache [{} {}]", dbName, disk, enabled, e);
        }
    }

    @EventHandler
    public void onXaeroWorldChange(XaeroWorldChangeEvent event) {
        if (!Minecraft.getInstance().isRunning()) return;
        try {
            cache.handleWorldChange(event);
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Error handling world change event for cache: {} event: {}", dbName, event, e);
        }
    }

    @EventHandler
    public void onClientTickEvent(ClientTickEvent.Post event) {
        try {
            cache.handleTick();
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Error handling tick event for cache: {}", dbName, e);
        }
    }

    @EventHandler
    public void onClientStopping(ClientStoppingEvent event) {
        try {
            cache.onShutdown();
            if (cache instanceof ChunkHighlightSavingCache savingCache) {
                savingCache.closeAndAwaitTermination();
            }
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Error closing {}", dbName, e);
        }
    }
}
