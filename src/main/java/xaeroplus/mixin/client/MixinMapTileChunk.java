package xaeroplus.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.map.MapProcessor;
import xaero.map.cache.BlockStateShortShapeCache;
import xaero.map.region.MapTile;
import xaero.map.region.MapTileChunk;
import xaero.map.region.texture.LeafRegionTexture;
import xaeroplus.util.SeenChunksTrackingMapTileChunk;

import java.io.DataInputStream;

@Mixin(value = MapTileChunk.class, remap = false)
public class MixinMapTileChunk implements SeenChunksTrackingMapTileChunk {
    private final boolean[][] seenTiles = new boolean[4][4];

    @Shadow private LeafRegionTexture leafTexture;

    @Override
    public boolean[][] getSeenTiles() {
        return seenTiles;
    }

    @Inject(method = "setTile", at = @At("HEAD"))
    public void setTile(final int x, final int z, final MapTile tile, final BlockStateShortShapeCache blockStateShortShapeCache, final CallbackInfo ci) {
        seenTiles[x][z] = tile != null;
    }

    @Inject(method = "readCacheData", at = @At("RETURN"))
    public void readCacheData(
            final int cacheSaveVersion, final DataInputStream input, final byte[] usableBuffer, final byte[] integerByteBuffer, final MapProcessor mapProcessor, final int x, final int y, final CallbackInfo ci
    ) {
        for(int i = 0; i < 4; ++i) {
            for (int j = 0; j < 4; ++j) {
                seenTiles[i][j] = leafTexture.getHeight(i << 4, j << 4) != -1;
            }
        }
    }
}
