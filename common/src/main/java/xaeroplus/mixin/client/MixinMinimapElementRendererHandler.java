package xaeroplus.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xaero.common.minimap.element.render.MinimapElementRenderer;
import xaero.common.minimap.element.render.MinimapElementRendererHandler;
import xaero.common.minimap.render.radar.element.RadarRenderContext;
import xaeroplus.feature.extensions.IScreenRadarRenderContext;

@Mixin(value = MinimapElementRendererHandler.class, remap = false)
public class MixinMinimapElementRendererHandler {
    @WrapOperation(method = "renderForRenderer", at = @At(
        value = "INVOKE",
        target = "Lxaero/common/minimap/element/render/MinimapElementRenderer;shouldRender(I)Z"
    ))
    public boolean captureRenderContext(final MinimapElementRenderer instance, final int i, final Operation<Boolean> original,
                                        @Local(name = "context") LocalRef<Object> contextRef) {
        if (contextRef.get() instanceof RadarRenderContext) {
            ((IScreenRadarRenderContext) contextRef.get()).setIsWorldMap(false);
        }
        return original.call(instance, i);
    }
}
