package xaeroplus.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.lib.client.graphics.shader.WorldMapShader;
import xaeroplus.feature.extensions.CustomWorldMapShader;

import java.io.IOException;

@Mixin(value = WorldMapShader.class, remap = false)
public class MixinWorldMapShader extends ShaderInstance implements CustomWorldMapShader {

    @Unique
    final Uniform transparentBackgroundUniform = this.getUniform("TransparentBackground");

    public MixinWorldMapShader(final ResourceProvider resourceProvider, final String name, final VertexFormat vertexFormat) throws IOException {
        super(resourceProvider, name, vertexFormat);
    }

    @ModifyExpressionValue(
        method = "<init>",
        at = @At(
            value = "CONSTANT",
            args = "stringValue=xaerolib/map")
    )
    private static String editShader(final String constant) {
        return "xaeroplus/custom_map";
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    public void init(final CallbackInfo ci) {
        setTransparentBackground(false);
    }

    @Override
    public void setTransparentBackground(final boolean value) {
        final int intValue = value ? 1 : 0;
        if (this.transparentBackgroundUniform.getIntBuffer().get(0) != intValue) {
            this.transparentBackgroundUniform.set(intValue);
        }
    }
}
