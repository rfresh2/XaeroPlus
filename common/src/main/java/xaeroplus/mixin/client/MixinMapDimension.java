package xaeroplus.mixin.client;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.map.file.RegionDetection;
import xaero.map.world.MapDimension;
import xaero.map.world.MapWorld;
import xaeroplus.settings.Settings;

@Mixin(value = MapDimension.class, remap = false)
public class MixinMapDimension {

    @Shadow
    @Final
    private MapWorld mapWorld;

    @Inject(method = "getWorldSaveRegionDetection", at = @At("HEAD"), cancellable = true)
    public void fastGetWorldSaveRegionDetection(final int x, final int z, final CallbackInfoReturnable<RegionDetection> cir) {
        if (Settings.REGISTRY.optimizeRegionDetectionLookups.get()) {
            if (this.mapWorld.isMultiplayer()) cir.setReturnValue(null);
            var currentDimension = this.mapWorld.getCurrentDimension();
            if (currentDimension != null && !currentDimension.isUsingWorldSave()) cir.setReturnValue(null);
        }
    }
}
