package xaeroplus.mixin.client;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.map.misc.Misc;
import xaeroplus.settings.XaeroPlusSettingRegistry;

@Mixin(value = Misc.class, remap = false)
public class MixinMisc {
    /**
     * @author rfresh2
     * @reason skipping world render messes up some events on hack clients
     */
    @Inject(method = "screenShouldSkipWorldRender", at = @At("HEAD"), cancellable = true)
    private static void screenShouldSkipWorldRender(Screen screen, boolean checkOtherMod, CallbackInfoReturnable<Boolean> cir) {
        if (!XaeroPlusSettingRegistry.skipWorldRenderSetting.getValue()) {
            cir.setReturnValue(false);
        }
    }

    /**
     * @author rfresh2
     * @reason hide unset keybind text
     */
    @Overwrite
    public static String getKeyName(KeyMapping kb) {
        return kb != null && KeyBindingHelper.getBoundKeyOf(kb).getValue() != -1 ? kb.getTranslatedKeyMessage().getString().toUpperCase() : "";
    }
}
