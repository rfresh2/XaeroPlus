package xaeroplus.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.map.WorldMap;
import xaero.map.file.MapSaveLoad;
import xaero.map.region.LeveledRegion;
import xaero.map.region.MapRegion;
import xaeroplus.Globals;
import xaeroplus.XaeroPlus;
import xaeroplus.settings.XaeroPlusSettingRegistry;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Objects;
import java.util.zip.ZipOutputStream;

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

    @Redirect(method = "saveRegion", at = @At(
        value = "NEW",
        args = "class=java/io/DataOutputStream"
    ))
    public DataOutputStream replaceSaveRegionZipOutputStream(final OutputStream out,
                                                             @Local(name = "zipOut") final ZipOutputStream zipOut,
                                                             @Share("zipOutShare") final LocalRef<ZipOutputStream> zipOutShare) {
        if (!XaeroPlusSettingRegistry.fastZipWrite.getValue()) return new DataOutputStream(out);
        Globals.zipFastByteBuffer.reset();
        zipOutShare.set(zipOut);
        return new DataOutputStream(Globals.zipFastByteBuffer);
    }

    @Inject(method = "saveRegion", at = @At(
        value = "INVOKE",
        target = "Ljava/util/zip/ZipOutputStream;closeEntry()V"
    ))
    public void saveRegionWriteZipOutputStream(final MapRegion region, final int extraAttempts, final CallbackInfoReturnable<Boolean> cir,
                                               @Local(name = "zipOut") final ZipOutputStream zipOut) throws IOException {
        if (!XaeroPlusSettingRegistry.fastZipWrite.getValue()) return;
        Globals.zipFastByteBuffer.writeTo(zipOut);
        Globals.zipFastByteBuffer.reset();
    }

    @Inject(method = "saveRegion", at = @At(
        value = "INVOKE",
        target = "Ljava/io/DataOutputStream;close()V"
    ))
    public void closeZipOutputStream(final MapRegion region, final int extraAttempts, final CallbackInfoReturnable<Boolean> cir,
                                     @Share("zipOutShare") final LocalRef<ZipOutputStream> zipOutShare
    ) throws IOException {
        if (!XaeroPlusSettingRegistry.fastZipWrite.getValue()) return;
        zipOutShare.get().close();
    }

    @Redirect(method = "run", at = @At(value = "INVOKE", target = "Lxaero/map/region/LeveledRegion;isAllCachePrepared()Z", ordinal = 0))
    public boolean redirectCacheSaveFailCrash(final LeveledRegion instance) {
        final boolean value = instance.isAllCachePrepared();
        if (!value) {
            XaeroPlus.LOGGER.warn("LeveledRegion cache not prepared. Attempting to repair crash");
            instance.setRecacheHasBeenRequested(false, "crash fix");
            // See MixinMapProcessor for where we catch the exception (which is still thrown)
        }
        return value;
    }
}
