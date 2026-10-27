package xaeroplus.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.map.MapProcessor;
import xaero.map.biome.BlockTintProvider;
import xaero.map.cache.BlockStateShortShapeCache;
import xaero.map.region.LeveledRegion;
import xaero.map.region.MapTileChunk;
import xaero.map.region.MapUpdateFastConfig;
import xaero.map.region.OverlayManager;
import xaero.map.region.texture.LeafRegionTexture;
import xaeroplus.feature.render.async.AsyncTileBuffers;

/**
 * Both hooks run on the render thread inside {@code MapProcessor.onRenderProcess}.
 * preUpload is only reached within the frame's upload budget, postUpload for every loaded texture,
 * so async buffer jobs get submitted even when the budget is spent.
 */
@Mixin(value = LeafRegionTexture.class, remap = false)
public abstract class MixinLeafRegionTexture {
    @Shadow private MapTileChunk tileChunk;

    @Inject(method = "preUpload", at = @At("HEAD"), cancellable = true)
    public void asyncBuffersPreUpload(final MapProcessor mapProcessor, final BlockTintProvider blockTintProvider, final OverlayManager overlayManager,
                                      final LeveledRegion<LeafRegionTexture> region, final boolean detailedDebug,
                                      final BlockStateShortShapeCache shapeCache, final MapUpdateFastConfig updateConfig, final CallbackInfo ci) {
        if (AsyncTileBuffers.handle(tileChunk, mapProcessor, true)) {
            ci.cancel();
        }
    }

    @Inject(method = "postUpload", at = @At("HEAD"))
    public void asyncBuffersPostUpload(final MapProcessor mapProcessor, final LeveledRegion<LeafRegionTexture> region,
                                       final boolean cleanAndCacheRequestsBlocked, final CallbackInfo ci) {
        AsyncTileBuffers.handle(tileChunk, mapProcessor, false);
    }
}
