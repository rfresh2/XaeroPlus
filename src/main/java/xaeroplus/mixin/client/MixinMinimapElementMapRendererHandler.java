package xaeroplus.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xaero.common.minimap.render.radar.element.RadarRenderContext;
import xaero.common.minimap.render.radar.element.RadarRenderer;
import xaero.hud.HudSession;
import xaero.hud.minimap.element.render.MinimapElementRenderInfo;
import xaero.hud.minimap.element.render.MinimapElementRenderer;
import xaero.hud.minimap.element.render.map.MinimapElementMapRendererHandler;
import xaeroplus.util.Globals;

@Mixin(value = MinimapElementMapRendererHandler.class, remap = false)
public class MixinMinimapElementMapRendererHandler {

    @Redirect(method = "transformAndRenderForRenderer", at = @At(
        value = "INVOKE",
        target = "Lxaero/hud/minimap/element/render/MinimapElementRenderer;renderElement(Ljava/lang/Object;ZZDFDDLxaero/hud/minimap/element/render/MinimapElementRenderInfo;)Z"
    ))
    public boolean redirectRenderElement(final MinimapElementRenderer instance,
                                         Object element,
                                         boolean highlit,
                                         boolean outOfBounds,
                                         double optionalDepth,
                                         float optionalScale,
                                         double partialX,
                                         double partialY,
                                         MinimapElementRenderInfo renderInfo) {
        if (instance instanceof RadarRenderer) {
            ((RadarRenderContext) instance.getContext()).nameScale = HudSession.getCurrentSession().getHudMod().getSettings().getDotNameScale();
            return instance.renderElement(element, highlit, outOfBounds, optionalDepth,
                                          optionalScale * Globals.minimapScalingFactor,
                                          partialX, partialY, renderInfo);
        } else {
            return instance.renderElement(element, highlit, outOfBounds, optionalDepth, optionalScale, partialX, partialY, renderInfo);
        }
    }
}
