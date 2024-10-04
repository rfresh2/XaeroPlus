package xaeroplus.mixin.client;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xaero.common.minimap.render.radar.element.RadarRenderContext;
import xaero.hud.minimap.element.render.MinimapElementRenderInfo;
import xaero.hud.minimap.element.render.MinimapElementRenderer;
import xaero.hud.minimap.element.render.MinimapElementRendererHandler;
import xaeroplus.util.IScreenRadarRenderContext;

@Mixin(value = MinimapElementRendererHandler.class, remap = false)
public class MixinMinimapElementRendererHandler {

    @WrapWithCondition(
        method = "renderForRenderer",
        at = @At(
            value = "INVOKE",
            target = "Lxaero/hud/minimap/element/render/MinimapElementRenderer;preRender(Lxaero/hud/minimap/element/render/MinimapElementRenderInfo;)V"
        ))
    public boolean renderForRendererInject(final MinimapElementRenderer instance, final MinimapElementRenderInfo minimapElementRenderInfo, @Local(name = "context") Object context) {
        if (context instanceof RadarRenderContext) {
            ((IScreenRadarRenderContext) context).setIsWorldMap(false);
        }
        return true;
    }
}
