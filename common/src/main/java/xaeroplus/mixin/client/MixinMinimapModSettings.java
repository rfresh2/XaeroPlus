package xaeroplus.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
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
import xaeroplus.settings.XaeroPlusModSettingsHooks;
import xaeroplus.settings.XaeroPlusSettingRegistry;
import xaeroplus.settings.XaeroPlusSettingsReflectionHax;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static xaeroplus.settings.XaeroPlusSettingsReflectionHax.ALL_MINIMAP_SETTINGS;

@Mixin(value = ModSettings.class, remap = false)
public class MixinMinimapModSettings {

    @Shadow
    public int caveMaps;
    @Shadow
    private boolean lockNorth;
    @Shadow
    public boolean keepUnlockedWhenEnlarged;
    @Shadow
    protected IXaeroMinimap modMain;

    @Shadow private int minimapSize;

    @Inject(method = "<init>", at = @At(value = "RETURN"))
    private void init(CallbackInfo ci) {
        // don't show cave maps on minimap by default
        caveMaps = 0;
    }

    @ModifyExpressionValue(method = "getLockNorth", at = @At(
        value = "CONSTANT",
        args = "intValue=180"))
    public int allowNoNorthLockWithTransparentMM(final int original) {
        if (XaeroPlusSettingRegistry.transparentMinimapBackground.getValue())
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
        XaeroPlusModSettingsHooks.saveSettings();
    }

    @Inject(method = "loadSettings", at = @At(value = "INVOKE", target = "Lxaero/common/settings/ModSettings;saveSettings()V"))
    public void loadSettings(final CallbackInfo ci, @Local(name = "mainConfigFile") Path mainConfigFile) throws IOException {
        if (!mainConfigFile.toFile().exists()) {
            XaeroPlusModSettingsHooks.loadSettings(null, ALL_MINIMAP_SETTINGS.get());
        }
    }

    @Inject(method = "loadSettingsFile", at = @At("RETURN"))
    public void loadSettingsFile(final File file, CallbackInfo ci) throws IOException {
        XaeroPlusModSettingsHooks.loadSettings(file, ALL_MINIMAP_SETTINGS.get());
    }

    @Inject(method = "getClientBooleanValue", at = @At("HEAD"), cancellable = true)
    public void getClientBooleanValue(ModOptions o, CallbackInfoReturnable<Boolean> cir) {
        XaeroPlusModSettingsHooks.getClientBooleanValue(o.getEnumString(), ALL_MINIMAP_SETTINGS.get(), cir);
    }

    @Inject(method = "setOptionValue", at = @At("HEAD"))
    public void setOptionValue(ModOptions o, Object value, final CallbackInfo ci) {
        XaeroPlusModSettingsHooks.setOptionValue(o.getEnumString(), value, ALL_MINIMAP_SETTINGS.get());
    }

    @Inject(method = "getOptionValue", at = @At("HEAD"), cancellable = true)
    public void getOptionValue(final ModOptions o, final CallbackInfoReturnable<Object> cir) {
        XaeroPlusModSettingsHooks.getOptionValue(o.getEnumString(), cir, ALL_MINIMAP_SETTINGS.get());
    }

    @Inject(method = "setOptionDoubleValue", at = @At("HEAD"))
    public void setOptionFloatValue(ModOptions o, double f, CallbackInfo ci) {
        XaeroPlusModSettingsHooks.setOptionDoubleValue(o.getEnumString(), f, ALL_MINIMAP_SETTINGS.get());
    }

    @Inject(method = "getOptionDoubleValue", at = @At("HEAD"), cancellable = true)
    public void getOptionFloatValue(ModOptions o, CallbackInfoReturnable<Double> cir) {
        XaeroPlusModSettingsHooks.getOptionDoubleValue(o.getEnumString(), cir, ALL_MINIMAP_SETTINGS.get());
    }

    @Inject(method = "getOptionValueName", at = @At("HEAD"), cancellable = true)
    public void getOptionValueName(ModOptions o, CallbackInfoReturnable<String> cir) {
        XaeroPlusModSettingsHooks.getOptionValueName(o.getEnumString(), cir, ALL_MINIMAP_SETTINGS.get());
    }

    @Inject(method = "isKeyRepeat", at = @At("RETURN"), cancellable = true)
    public void isKeyRepeat(KeyMapping kb, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(cir.getReturnValue() && XaeroPlusSettingsReflectionHax.keybindingMapSupplier.get().get(kb) == null);
    }

    @Inject(method = "getSliderOptionText", at = @At("HEAD"), cancellable = true)
    public void getSliderOptionText(final ModOptions o, final CallbackInfoReturnable<String> cir) {
        XaeroPlusModSettingsHooks.getSliderOptionText(o.getEnumString(), cir, ALL_MINIMAP_SETTINGS.get());
    }
}

