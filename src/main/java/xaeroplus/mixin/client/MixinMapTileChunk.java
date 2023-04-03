package xaeroplus.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.map.MapProcessor;
import xaero.map.cache.BlockStateShortShapeCache;
import xaero.map.mcworld.WorldMapClientWorldData;
import xaero.map.region.MapTile;
import xaero.map.region.MapTileChunk;
import xaero.map.region.texture.LeafRegionTexture;
import xaeroplus.XaeroPlus;
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

    @Redirect(method = "updateBuffers", at = @At(value = "FIELD", target = "Lxaero/map/mcworld/WorldMapClientWorldData;shadowR:F"))
    public float getShadowR(final WorldMapClientWorldData instance) {
        if (XaeroPlus.customDimensionId == 0) {
            return 0.518F;
        } else if (XaeroPlus.customDimensionId == -1) {
            return 1.0F;
        } else {
            return 1.0F;
        }
    }

    @Redirect(method = "updateBuffers", at = @At(value = "FIELD", target = "Lxaero/map/mcworld/WorldMapClientWorldData;shadowG:F"))
    public float getShadowG(final WorldMapClientWorldData instance) {
        if (XaeroPlus.customDimensionId == 0) {
            return 0.678F;
        } else if (XaeroPlus.customDimensionId == -1) {
            return .0F;
        } else {
            return 1.0F;
        }
    }

    @Redirect(method = "updateBuffers", at = @At(value = "FIELD", target = "Lxaero/map/mcworld/WorldMapClientWorldData;shadowB:F"))
    public float getShadowB(final WorldMapClientWorldData instance) {
        if (XaeroPlus.customDimensionId == 0) {
            return 1.0F;
        } else if (XaeroPlus.customDimensionId == -1) {
            return .0F;
        } else {
            return 1.0F;
        }
    }
}
