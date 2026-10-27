package xaeroplus.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
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
import xaeroplus.feature.extensions.AsyncBufferedMapTileChunk;
import xaeroplus.feature.extensions.SeenChunksTrackingMapTileChunk;
import xaeroplus.feature.render.async.AsyncTileBuffers;
import xaeroplus.settings.Settings;

@Mixin(value = MapTileChunk.class, remap = false)
public abstract class MixinMapTileChunk implements SeenChunksTrackingMapTileChunk, AsyncBufferedMapTileChunk {
    @Unique private final boolean[][] xaeroPlus$seenTiles = new boolean[4][4];
    @Unique private volatile int xaeroPlus$generation;
    @Unique private volatile AsyncTileBuffers.Job xaeroPlus$job;
    @Unique private boolean xaeroPlus$syncFallback;

    @Shadow private LeafRegionTexture leafTexture;

    @Shadow public abstract MapRegion getInRegion();

    @Override
    public boolean[][] getSeenTiles() {
        return xaeroPlus$seenTiles;
    }

    @Inject(method = "setTile", at = @At("HEAD"))
    public void setTile(final int x, final int z, final MapTile tile, final BlockStateShortShapeCache blockStateShortShapeCache, final MapProcessor mapProcessor, final CallbackInfo ci) {
        xaeroPlus$seenTiles[x][z] = tile != null;
    }

    @Inject(method = {"setToUpdateBuffers", "setTile", "clean"}, at = @At("HEAD"))
    public void invalidateAsyncBufferJob(final CallbackInfo ci) {
        xaeroPlus$generation++;
    }

    @Override
    public int xaeroPlus$generation() {
        return xaeroPlus$generation;
    }

    @Override
    public AsyncTileBuffers.Job xaeroPlus$job() {
        return xaeroPlus$job;
    }

    @Override
    public void xaeroPlus$setJob(final AsyncTileBuffers.Job job) {
        xaeroPlus$job = job;
    }

    @Override
    public boolean xaeroPlus$syncFallback() {
        return xaeroPlus$syncFallback;
    }

    @Override
    public void xaeroPlus$setSyncFallback(final boolean syncFallback) {
        xaeroPlus$syncFallback = syncFallback;
    }

    @Inject(method = "readCacheData", at = @At("RETURN"))
    public void readCacheData(final CallbackInfo ci) {
        for(int i = 0; i < 4; ++i) {
            for (int j = 0; j < 4; ++j) {
                var height = leafTexture.getHeight(i << 4, j << 4);
                xaeroPlus$seenTiles[i][j] = height != -1 && height != 32767;
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
