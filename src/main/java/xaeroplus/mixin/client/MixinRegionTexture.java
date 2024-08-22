package xaeroplus.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xaero.map.graphics.PixelBuffers;
import xaero.map.region.texture.RegionTexture;

import java.nio.ByteBuffer;

@Mixin(value = RegionTexture.class, remap = false)
public class MixinRegionTexture {

    @Redirect(method = "writeToUnpackPBO", at = @At(
        value = "INVOKE",
        target = "Lxaero/map/graphics/PixelBuffers;glMapBuffer(IIJLjava/nio/ByteBuffer;)Ljava/nio/ByteBuffer;"
    ))
    public ByteBuffer redirectGlMapBuffer(final int target, final int access, final long length, final ByteBuffer buffer) {
        ByteBuffer result = PixelBuffers.glMapBuffer(target, access, length, buffer);
        if (result == null && System.getenv("XP_CI_TEST") != null) {
            return ByteBuffer.allocate((int) length);
        }
        return result;
    }
}
