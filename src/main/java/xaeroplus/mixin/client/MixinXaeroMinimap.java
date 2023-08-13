package xaeroplus.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.minimap.XaeroMinimap;
import xaeroplus.XaeroPlus;

@Mixin(value = XaeroMinimap.class, remap = false)
@Pseudo
public class MixinXaeroMinimap {

    @Inject(method = "loadCommon", at = @At("HEAD"))
    public void loadCommonInject(final CallbackInfo ci) {
        XaeroPlus.initialize();
    }
}
