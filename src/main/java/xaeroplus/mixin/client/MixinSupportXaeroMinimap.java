package xaeroplus.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.map.mods.SupportXaeroMinimap;
import xaeroplus.settings.XaeroPlusSettingRegistry;

@Mixin(value = SupportXaeroMinimap.class, remap = false)
public class MixinSupportXaeroMinimap {

    @Inject(method = "getSubWorldNameToRender", at = @At("HEAD"), cancellable = true)
    public void getSubworldNameToRenderInject(final CallbackInfoReturnable<String> cir) {
        if (XaeroPlusSettingRegistry.owAutoWaypointDimension.getValue()) {
            // remove annoying string rendered in the middle of the worldmap
            cir.setReturnValue(null);
        }
    }
}
