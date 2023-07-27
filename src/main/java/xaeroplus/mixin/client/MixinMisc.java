package xaeroplus.mixin.client;

import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.map.misc.Misc;
import xaeroplus.settings.XaeroPlusSettingRegistry;

import java.lang.reflect.Method;

@Mixin(value = Misc.class, remap = false)
public class MixinMisc {

    /**
     * @author rfresh2
     * @reason skipping world render messes up some events on hack clients
     */
    @Inject(method = "screenShouldSkipWorldRender", at = @At("HEAD"), cancellable = true)
    private static void screenShouldSkipWorldRender(GuiScreen screen, boolean checkOtherMod, CallbackInfoReturnable<Boolean> cir) {
        if (!XaeroPlusSettingRegistry.skipWorldRenderSetting.getValue()) {
            cir.setReturnValue(false);
        }
    }

    @Redirect(method = "getReflectMethodValue", at = @At(value = "INVOKE", target = "Ljava/lang/reflect/Method;setAccessible(Z)V", ordinal = 0))
    private static void setAccessible(final Method instance, final boolean b) {
        if (instance.isAccessible()) return; // don't set accessible if it already is lol
        instance.setAccessible(true);
    }

    @Redirect(method = "getReflectMethodValue", at = @At(value = "INVOKE", target = "Ljava/lang/reflect/Method;setAccessible(Z)V", ordinal = 1))
    private static void resetAccessible(final Method instance, final boolean b) {
        // don't reset accessible lol
    }
}
