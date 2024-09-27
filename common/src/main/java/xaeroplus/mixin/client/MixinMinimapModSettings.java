package xaeroplus.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.common.HudMod;
import xaero.common.IXaeroMinimap;
import xaero.common.settings.ModOptions;
import xaero.common.settings.ModSettings;
import xaero.hud.minimap.Minimap;
import xaeroplus.Globals;
import xaeroplus.settings.SettingHooks;
import xaeroplus.settings.Settings;


@Mixin(value = ModSettings.class, remap = false)
public class MixinMinimapModSettings {

    @Shadow
    public int caveMaps;
    @Shadow
    protected IXaeroMinimap modMain;

    @Inject(method = "<init>", at = @At(value = "RETURN"))
    private void init(CallbackInfo ci) {
        // don't show cave maps on minimap by default
        caveMaps = 0;
    }

    @ModifyExpressionValue(method = "getLockNorth", at = @At(
        value = "CONSTANT",
        args = "intValue=180"))
    public int allowNoNorthLockWithTransparentMM(final int original) {
        if (Settings.REGISTRY.transparentMinimapBackground.get())
            // will make the if expression always return false
            return Integer.MAX_VALUE;
        else return original;
    }

    @Inject(method = "getMinimapSize", at = @At(
        value = "RETURN"
    ), cancellable = true)
    public void modifyMinimapSize(final CallbackInfoReturnable<Integer> cir) {
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


    @Inject(method = "saveSettings", at = @At("RETURN"))
    public void saveSettings(final CallbackInfo ci) {
        SettingHooks.saveSettings();
    }

    @Inject(method = "getClientBooleanValue", at = @At("HEAD"), cancellable = true)
    public void getClientBooleanValue(ModOptions o, CallbackInfoReturnable<Boolean> cir) {
        SettingHooks.getClientBooleanValue(o.getEnumString(), cir);
    }

    @Inject(method = "setOptionValue", at = @At("HEAD"))
    public void setOptionValue(ModOptions o, Object value, final CallbackInfo ci) {
        SettingHooks.setOptionValue(o.getEnumString(), value);
    }

    @Inject(method = "getOptionValue", at = @At("HEAD"), cancellable = true)
    public void getOptionValue(final ModOptions o, final CallbackInfoReturnable<Object> cir) {
        SettingHooks.getOptionValue(o.getEnumString(), cir);
    }

    @Inject(method = "setOptionDoubleValue", at = @At("HEAD"))
    public void setOptionFloatValue(ModOptions o, double f, CallbackInfo ci) {
        SettingHooks.setOptionDoubleValue(o.getEnumString(), f);
    }

    @Inject(method = "getOptionDoubleValue", at = @At("HEAD"), cancellable = true)
    public void getOptionFloatValue(ModOptions o, CallbackInfoReturnable<Double> cir) {
        SettingHooks.getOptionDoubleValue(o.getEnumString(), cir);
    }

    @Inject(method = "getOptionValueName", at = @At("HEAD"), cancellable = true)
    public void getOptionValueName(ModOptions o, CallbackInfoReturnable<String> cir) {
        SettingHooks.getOptionValueName(o.getEnumString(), cir);
    }

    @Inject(method = "isKeyRepeat", at = @At("RETURN"), cancellable = true)
    public void isKeyRepeat(KeyMapping kb, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(cir.getReturnValue() && Settings.REGISTRY.getKeybindingSetting(kb) == null);
    }

    @Inject(method = "getSliderOptionText", at = @At("HEAD"), cancellable = true)
    public void getSliderOptionText(final ModOptions o, final CallbackInfoReturnable<String> cir) {
        SettingHooks.getSliderOptionText(o.getEnumString(), cir);
    }
}

