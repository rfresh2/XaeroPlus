package xaeroplus.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import xaero.map.region.MapRegion;

/**
 * Exposes internal synchronization fields on {@link MapRegion} needed for
 * safe hot-reload of region data on the render thread.
 */
@Mixin(value = MapRegion.class, remap = false)
public interface AccessorMapRegion {
    @Accessor("writerThreadPauseSync")
    Object getWriterThreadPauseSync();
}
