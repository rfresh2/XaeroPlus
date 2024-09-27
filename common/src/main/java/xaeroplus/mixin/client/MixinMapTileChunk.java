package xaeroplus.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.map.MapProcessor;
import xaero.map.cache.BlockStateShortShapeCache;
import xaero.map.region.MapRegion;
import xaero.map.region.MapTile;
import xaero.map.region.MapTileChunk;
import xaero.map.region.texture.LeafRegionTexture;
import xaero.map.world.MapDimension;
import xaero.map.world.MapWorld;
import xaeroplus.feature.extensions.SeenChunksTrackingMapTileChunk;
import xaeroplus.settings.Settings;

import java.io.DataInputStream;

@Mixin(value = MapTileChunk.class, remap = false)
public abstract class MixinMapTileChunk implements SeenChunksTrackingMapTileChunk {
    private final boolean[][] seenTiles = new boolean[4][4];

    @Shadow private LeafRegionTexture leafTexture;

    @Shadow public abstract MapRegion getInRegion();

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
            final int minorSaveVersion,
            final int majorSaveVersion,
            final DataInputStream input,
            final byte[] usableBuffer,
            final byte[] integerByteBuffer,
            final MapProcessor mapProcessor,
            final int x,
            final int y,
            final CallbackInfo ci
    ) {
        for(int i = 0; i < 4; ++i) {
            for (int j = 0; j < 4; ++j) {
                var height = leafTexture.getHeight(i << 4, j << 4);
                seenTiles[i][j] = height != -1 && height != 32767;
            }
        }
    }

    @WrapOperation(method = "updateBuffers", at = @At(
        value = "INVOKE",
        target = "Lxaero/map/world/MapWorld;getCurrentDimension()Lxaero/map/world/MapDimension;"
    ))
    public MapDimension useRegionDimensionInsteadOfMapWorld(final MapWorld mapWorld, final Operation<MapDimension> original) {
        if (Settings.REGISTRY.writesWhileDimSwitched.get() && mapWorld.isMultiplayer()) {
            return getInRegion().getDim();
        } else {
            return original.call(mapWorld);
        }
    }
}
