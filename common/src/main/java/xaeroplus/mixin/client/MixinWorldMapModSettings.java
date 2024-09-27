package xaeroplus.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.map.settings.ModOptions;
import xaero.map.settings.ModSettings;
import xaeroplus.settings.SettingHooks;

import java.io.IOException;

@Mixin(value = ModSettings.class, remap = false)
public class MixinWorldMapModSettings {

    @Shadow
    public int defaultCaveModeType;

    @Inject(method = "<init>", at = @At("RETURN"))
    public void initInject(final CallbackInfo ci) {
        this.defaultCaveModeType = 2; // FULL
    }

    @Inject(method = "saveSettings", at = @At(value = "RETURN"))
    public void saveSettings(final CallbackInfo ci) throws IOException {
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
    public void setOptionDoubleValue(ModOptions o, double f, CallbackInfo ci) {
        SettingHooks.setOptionDoubleValue(o.getEnumString(), f);
    }

    @Inject(method = "getOptionDoubleValue", at = @At("HEAD"), cancellable = true)
    public void getOptionDoubleValue(ModOptions o, CallbackInfoReturnable<Double> cir) {
        SettingHooks.getOptionDoubleValue(o.getEnumString(), cir);
    }

    @Inject(method = "getOptionValueName", at = @At("HEAD"), cancellable = true)
    public void getOptionValueName(ModOptions o, CallbackInfoReturnable<String> cir) {
        SettingHooks.getOptionValueName(o.getEnumString(), cir);
    }

    @Inject(method = "getSliderOptionText", at = @At("HEAD"), cancellable = true)
    public void getSliderOptionText(final ModOptions o, final CallbackInfoReturnable<String> cir) {
        SettingHooks.getSliderOptionText(o.getEnumString(), cir);
    }
}
