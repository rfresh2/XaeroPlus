package xaeroplus.mixin.client;

import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import xaero.common.AXaeroMinimap;
import xaero.common.XaeroMinimapSession;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.common.minimap.waypoints.WaypointSet;
import xaero.common.minimap.waypoints.WaypointWorld;
import xaero.common.settings.ModOptions;
import xaero.common.settings.ModSettings;
import xaeroplus.XaeroPlus;
import xaeroplus.feature.extensions.IWaypointDimension;
import xaeroplus.settings.XaeroPlusModSettingsHooks;
import xaeroplus.settings.XaeroPlusSettingRegistry;
import xaeroplus.settings.XaeroPlusSettingsReflectionHax;

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
    protected AXaeroMinimap modMain;

    @Shadow public boolean allowInternetAccess;

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

    @Inject(
        method = "checkWaypointsLine",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/ArrayList;add(Ljava/lang/Object;)Z"),
        locals = LocalCapture.CAPTURE_FAILHARD)
    public void createWaypointInject(final String[] args, final WaypointWorld wpw, final CallbackInfoReturnable<Boolean> cir, final String setName, final WaypointSet set, boolean yIncluded, int yCoord, Waypoint waypoint) {
        try {
            ((IWaypointDimension) waypoint).setDimension(wpw.getDimId());
        } catch (final Throwable e) {
            XaeroPlus.LOGGER.error("Failed setting waypoint dimension: {}", waypoint, e);
        }
    }

    @Inject(method = "saveSettings", at = @At("RETURN"))
    public void saveSettings(final CallbackInfo ci) throws IOException {
        XaeroPlusModSettingsHooks.saveSettings(this.modMain.getConfigFile(), ALL_MINIMAP_SETTINGS.get());
    }

    @Inject(method = "loadSettingsFile", at = @At("RETURN"))
    public void loadSettings(final File file, CallbackInfo ci) throws IOException {
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

