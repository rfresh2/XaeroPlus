package xaeroplus.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.lib.client.level.block.BlockTextureColorUtils;
import xaeroplus.feature.render.async.AsyncTileBuffers;

@Mixin(value = BlockTextureColorUtils.class, remap = false)
public abstract class MixinBlockTextureColorUtils {
    @Inject(method = "requestCachedColoursClear", at = @At("HEAD"))
    public void clearAsyncWorkerColorCaches(final CallbackInfo ci) {
        AsyncTileBuffers.clearColorCaches();
    }
}
