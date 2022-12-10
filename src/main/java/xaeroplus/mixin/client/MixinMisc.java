package xaeroplus.mixin.client;

import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.map.misc.Misc;
import xaeroplus.XaeroPlusSettingRegistry;

@Mixin(value = Misc.class, remap = false)
public class MixinMisc {

    /**
     * @author rfresh2
     * @reason skipping world render messes up some events on hack clients
     */
    @Inject(method = "screenShouldSkipWorldRender", at = @At("HEAD"), cancellable = true)
    private static void screenShouldSkipWorldRender(GuiScreen screen, boolean checkOtherMod, CallbackInfoReturnable<Boolean> cir) {
        if (!XaeroPlusSettingRegistry.skipWorldRenderSetting.getBooleanSettingValue()) {
            cir.setReturnValue(false);
        }
    }
}
