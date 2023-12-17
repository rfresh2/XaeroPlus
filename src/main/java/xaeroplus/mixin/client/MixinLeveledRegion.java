package xaeroplus.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.map.region.LeveledRegion;
import xaero.map.region.texture.RegionTexture;

import java.io.*;
import java.util.zip.ZipOutputStream;

@Mixin(value = LeveledRegion.class, remap = false)
public abstract class MixinLeveledRegion<T extends RegionTexture<T>> {
    // todo: benchmark loadTexture zipfast vs current

    @Redirect(method = "saveCacheTextures", at = @At(
        value = "NEW",
        args = "class=java/io/DataOutputStream"
    ))
    public DataOutputStream replaceSaveCacheTexturesZipOutputStream(final OutputStream out,
                                                                    @Share("byteOut") final LocalRef<ByteArrayOutputStream> byteOutRef) {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        byteOutRef.set(byteOut);
        return new DataOutputStream(byteOut);
    }

    @Inject(method = "saveCacheTextures", at = @At(
        value = "INVOKE",
        target = "Ljava/util/zip/ZipOutputStream;closeEntry()V"
    ))
    public void writeSaveCacheTexturesZipOutputStream(final File tempFile, final int extraAttempts, final CallbackInfoReturnable<Boolean> cir,
                                     @Local(name = "zipOutput") LocalRef<ZipOutputStream> zipOutputRef,
                                     @Share("byteOut") final LocalRef<ByteArrayOutputStream> byteOutRef) {
        try {
            byteOutRef.get().writeTo(zipOutputRef.get());
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
