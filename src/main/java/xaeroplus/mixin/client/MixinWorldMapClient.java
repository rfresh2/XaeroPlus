package xaeroplus.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.map.WorldMapClient;
import xaeroplus.util.Shared;

@Mixin(value = WorldMapClient.class, remap = false)
public class MixinWorldMapClient {

    @Inject(method = "postInit", at = @At("RETURN"))
    public void postInit(final CallbackInfo ci) {
        Shared.settingsLoadedInit = true;
    }
}
