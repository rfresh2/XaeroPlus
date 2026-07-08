package xaeroplus.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.common.HudMod;
import xaero.hud.minimap.Minimap;
import xaero.hud.minimap.common.config.option.MinimapProfiledConfigOptions;
import xaero.hud.minimap.config.util.MinimapConfigClientUtils;
import xaero.lib.client.config.ClientConfigManager;
import xaeroplus.Globals;

import java.util.Objects;

@Mixin(value = MinimapConfigClientUtils.class, remap = false)
public class MixinMinimapConfigClientUtils {

    @ModifyExpressionValue(method = "getEffectiveNorthLocked", at = @At(
        value = "CONSTANT",
        args = "intValue=180"))
    private static int allowNoNorthLockWithTransparentMM(final int original) {
        ClientConfigManager configManager = HudMod.INSTANCE.getHudConfigs().getClientConfigManager();
        var undiscoveredOpacityConfig = configManager.getEffective(MinimapProfiledConfigOptions.UNDISCOVERED_OPACITY);
        var maxOpacity = MinimapProfiledConfigOptions.UNDISCOVERED_OPACITY.getValidValues().stream().sorted().findFirst().orElse(100);
        if (Objects.equals(undiscoveredOpacityConfig, maxOpacity)) {
            return Integer.MAX_VALUE;
        }
        else return original;
    }

    @Inject(method = "getEffectiveMinimapSize", at = @At(
        value = "RETURN"
    ), cancellable = true)
    private static void modifyMinimapSize(final CallbackInfoReturnable<Integer> cir) {
        try {
            var hudMod = HudMod.INSTANCE;
            if (hudMod == null) return;
            Minimap minimap = hudMod.getMinimap();
            if (minimap == null) return;
            if (minimap.usingFBO()) {
                cir.setReturnValue(cir.getReturnValue() * Globals.minimapSizeMultiplier);
            }
        } catch (final Exception e) {
            // fall through
        }
    }
}
