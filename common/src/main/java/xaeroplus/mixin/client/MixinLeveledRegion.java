package xaeroplus.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.map.region.LeveledRegion;
import xaero.map.region.texture.RegionTexture;
import xaeroplus.Globals;
import xaeroplus.settings.XaeroPlusSettingRegistry;

import java.io.*;
import java.util.zip.ZipOutputStream;

@Mixin(value = LeveledRegion.class, remap = false)
public abstract class MixinLeveledRegion<T extends RegionTexture<T>> {
    @Redirect(method = "saveCacheTextures", at = @At(
        value = "NEW",
        args = "class=java/io/DataOutputStream"
    ))
    public DataOutputStream replaceSaveCacheTexturesZipOutputStream(final OutputStream out) {
        if (!XaeroPlusSettingRegistry.fastZipWrite.getValue()) return new DataOutputStream(out);
        Globals.zipFastByteBuffer.reset();
        return new DataOutputStream(Globals.zipFastByteBuffer);
    }

    @Inject(method = "saveCacheTextures", at = @At(
        value = "INVOKE",
        target = "Ljava/util/zip/ZipOutputStream;closeEntry()V"
    ))
    public void writeSaveCacheTexturesZipOutputStream(final File tempFile, final int extraAttempts, final CallbackInfoReturnable<Boolean> cir,
                                     @Local(name = "zipOutput") LocalRef<ZipOutputStream> zipOutputRef
    ) {
        if (!XaeroPlusSettingRegistry.fastZipWrite.getValue()) return;
        try {
            Globals.zipFastByteBuffer.writeTo(zipOutputRef.get());
            Globals.zipFastByteBuffer.reset();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
