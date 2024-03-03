package xaeroplus.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xaero.common.minimap.render.radar.element.RadarRenderContext;
import xaero.map.element.MapElementRenderHandler;
import xaero.map.element.MapElementRenderer;
import xaeroplus.feature.extensions.IScreenRadarRenderContext;

@Mixin(value = MapElementRenderHandler.class, remap = false)
public class MixinMapElementRenderHandler {
    @WrapOperation(method = "renderWithRenderer", at = @At(
        value = "INVOKE",
        target = "Lxaero/map/element/MapElementRenderer;shouldBeDimScaled()Z"
    ))
    public boolean captureRenderContextType(final MapElementRenderer instance, final Operation<Boolean> original,
                                            @Local(name = "context") LocalRef<Object> contextRef) {
        if (contextRef.get() instanceof RadarRenderContext) {
            ((IScreenRadarRenderContext) contextRef.get()).setIsWorldMap(true);
        }
        return original.call(instance);
    }
}
