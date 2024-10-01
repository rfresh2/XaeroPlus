package xaeroplus.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xaero.common.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider;
import xaero.common.minimap.render.radar.element.RadarRenderContext;
import xaero.hud.minimap.element.render.MinimapElementRenderInfo;
import xaero.hud.minimap.element.render.MinimapElementRenderer;
import xaero.hud.minimap.element.render.MinimapElementRendererHandler;
import xaeroplus.feature.extensions.IScreenRadarRenderContext;

@Mixin(value = MinimapElementRendererHandler.class, remap = false)
public class MixinMinimapElementRendererHandler {
    @WrapOperation(method = "renderForRenderer", at = @At(
        value = "INVOKE",
        target = "Lxaero/hud/minimap/element/render/MinimapElementRenderer;preRender(Lxaero/hud/minimap/element/render/MinimapElementRenderInfo;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lxaero/common/graphics/renderer/multitexture/MultiTextureRenderTypeRendererProvider;)V"
    ))
    public void captureRenderContext(final MinimapElementRenderer instance, final MinimapElementRenderInfo minimapElementRenderInfo, final MultiBufferSource.BufferSource bufferSource, final MultiTextureRenderTypeRendererProvider multiTextureRenderTypeRendererProvider, final Operation<Void> original,
                                     @Local(name = "context") LocalRef<Object> contextRef) {
        if (contextRef.get() instanceof RadarRenderContext) {
            ((IScreenRadarRenderContext) contextRef.get()).setIsWorldMap(false);
        }
        original.call(instance, minimapElementRenderInfo, bufferSource, multiTextureRenderTypeRendererProvider);
    }
}
