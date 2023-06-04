package xaeroplus.mixin.client.mc;

import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaeroplus.settings.XaeroPlusSettingRegistry;

import java.util.Set;

@Mixin(value = RenderGlobal.class)
public class MixinRenderGlobal {
    @Final
    @Shadow
    private Set<BlockPos> setLightUpdates;

    @Inject(method = "notifyLightSet", at = @At("HEAD"), cancellable = true)
    public void injectNotifyLightSet(final BlockPos pos, CallbackInfo ci) {
        if (XaeroPlusSettingRegistry.mcLightingLimiter.getValue()) {
            if (setLightUpdates.size() > XaeroPlusSettingRegistry.mcLightingLimiterLimit.getValue()) {
                ci.cancel();
            }
        }
    }
}
