package xaeroplus.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import xaero.common.IXaeroMinimap;
import xaero.common.XaeroMinimapSession;
import xaero.common.settings.ModOptions;
import xaero.common.settings.ModSettings;
import xaeroplus.settings.XaeroPlusModSettingsHooks;
import xaeroplus.settings.XaeroPlusSettingRegistry;

import java.io.File;
import java.io.IOException;

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

    @Inject(method = "<init>", at = @At(value = "RETURN"))
    private void init(CallbackInfo ci) {
        // don't show cave maps on minimap by default
        caveMaps = 0;
    }

    @Inject(method = "getLockNorth", at = @At("HEAD"), cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD)
    public void getLockNorth(int mapSize, int shape, CallbackInfoReturnable<Boolean> cir) {
        if (!XaeroPlusSettingRegistry.transparentMinimapBackground.getValue()) return;
        // prevent lock north from being forced to true when minimap is square and greater than 180 size
        XaeroMinimapSession minimapSession = XaeroMinimapSession.getCurrentSession();
        if (minimapSession == null) {
            cir.setReturnValue(this.lockNorth);
        } else {
            cir.setReturnValue(this.lockNorth || !this.keepUnlockedWhenEnlarged && minimapSession.getMinimapProcessor().isEnlargedMap());
        }
    }

    @Inject(method = "saveSettings", at = @At("TAIL"))
    public void saveSettings(final CallbackInfo ci) throws IOException {
        XaeroPlusModSettingsHooks.saveSettings(this.modMain.getConfigFile(), ALL_MINIMAP_SETTINGS.get());
    }

    @Inject(method = "loadSettingsFile", at = @At("TAIL"))
    public void loadSettings(final File file, CallbackInfo ci) throws IOException {
        XaeroPlusModSettingsHooks.loadSettings(file, ALL_MINIMAP_SETTINGS.get());
    }

    @Inject(method = "getClientBooleanValue", at = @At("HEAD"), cancellable = true)
    public void getClientBooleanValue(ModOptions o, CallbackInfoReturnable<Boolean> cir) {
        XaeroPlusModSettingsHooks.getClientBooleanValue(o.getEnumString(), ALL_MINIMAP_SETTINGS.get(), cir);
    }

    @Inject(method = "setOptionValue", at = @At("HEAD"))
    public void setOptionValue(ModOptions o, int par2, final CallbackInfo ci) {
        XaeroPlusModSettingsHooks.setOptionValue(o.getEnumString(), ALL_MINIMAP_SETTINGS.get());
    }

    @Inject(method = "setOptionFloatValue", at = @At("HEAD"))
    public void setOptionFloatValue(ModOptions o, double f, CallbackInfo ci) {
        XaeroPlusModSettingsHooks.setOptionFloatValue(o.getEnumString(), f, ALL_MINIMAP_SETTINGS.get());
    }

    @Inject(method = "getOptionFloatValue", at = @At("HEAD"), cancellable = true)
    public void getOptionFloatValue(ModOptions o, CallbackInfoReturnable<Double> cir) {
        XaeroPlusModSettingsHooks.getOptionFloatValue(o.getEnumString(), cir, ALL_MINIMAP_SETTINGS.get());
    }

    @Inject(method = "getKeyBinding", at = @At("HEAD"), cancellable = true)
    public void getKeybinding(final ModOptions par1EnumOptions, final CallbackInfoReturnable<String> cir) {
        XaeroPlusModSettingsHooks.getKeybinding(par1EnumOptions.getEnumString(), cir, ALL_MINIMAP_SETTINGS.get());
    }
}
