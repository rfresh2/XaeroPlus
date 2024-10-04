package xaeroplus.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.map.WorldMap;
import xaero.map.file.MapSaveLoad;
import xaeroplus.util.Globals;

import java.nio.file.Path;
import java.util.Objects;

@Mixin(value = MapSaveLoad.class, remap = false)
public abstract class MixinMapSaveLoad {
    @Inject(method = "getOldFolder", at = @At(value = "HEAD"), cancellable = true)
    public void getOldFolder(final String oldUnfixedMainId, final String dim, final CallbackInfoReturnable<Path> cir) {
        if (!Globals.nullOverworldDimensionFolder) {
            if (oldUnfixedMainId == null) {
                cir.setReturnValue(null);
            }
            String dimIdFixed = Objects.equals(dim, "null") ? "0" : dim;
            cir.setReturnValue(WorldMap.saveFolder.toPath().resolve(oldUnfixedMainId + "_" + dimIdFixed));
        }
    }
}
