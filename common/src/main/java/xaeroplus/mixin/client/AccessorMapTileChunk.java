package xaeroplus.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import xaero.map.region.MapTileChunk;
import xaero.map.region.texture.LeafRegionTexture;

/**
 * Exposes internal fields on {@link MapTileChunk} for hot-reload.
 */
@Mixin(value = MapTileChunk.class, remap = false)
public interface AccessorMapTileChunk {
    @Accessor
    byte getLoadState();

    @Accessor
    void setLoadState(byte state);

    @Accessor
    void setToUpdateBuffers(boolean value);

    @Accessor
    LeafRegionTexture getLeafTexture();
}
