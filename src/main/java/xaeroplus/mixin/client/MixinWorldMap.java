package xaeroplus.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.map.WorldMap;
import xaeroplus.XaeroPlus;

@Mixin(value = WorldMap.class, remap = false)
public class MixinWorldMap {

    @Inject(method = "loadCommon", at = @At("HEAD"))
    public void onWorldMapInitialize(final CallbackInfo ci) {
        XaeroPlus.initialize();
    }

}
