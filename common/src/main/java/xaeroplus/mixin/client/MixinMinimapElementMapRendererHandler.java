package xaeroplus.mixin.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xaero.common.minimap.render.radar.element.RadarRenderContext;
import xaero.common.minimap.render.radar.element.RadarRenderer;
import xaero.hud.HudSession;
import xaero.hud.minimap.element.render.MinimapElementRenderInfo;
import xaero.hud.minimap.element.render.MinimapElementRenderer;
import xaero.hud.minimap.element.render.map.MinimapElementMapRendererHandler;
import xaeroplus.Globals;

@Mixin(value = MinimapElementMapRendererHandler.class, remap = false)
public class MixinMinimapElementMapRendererHandler {

    @Redirect(method = "transformAndRenderForRenderer", at = @At(
        value = "INVOKE",
        target = "Lxaero/hud/minimap/element/render/MinimapElementRenderer;renderElement(Ljava/lang/Object;ZZDFDDLxaero/hud/minimap/element/render/MinimapElementRenderInfo;Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;)Z"
    ), remap = true) // $REMAP
    public boolean redirectRenderElement(final MinimapElementRenderer instance,
                                         final Object element,
                                         final boolean highlit,
                                         final boolean outOfBounds,
                                         final double optionalDepth,
                                         final float optionalScale,
                                         final double partialX,
                                         final double partialY,
                                         final MinimapElementRenderInfo minimapElementRenderInfo,
                                         final GuiGraphics guiGraphics,
                                         final MultiBufferSource.BufferSource bufferSource) {
        if (instance instanceof RadarRenderer) {
            ((RadarRenderContext) instance.getContext()).nameScale = HudSession.getCurrentSession().getHudMod().getSettings().getDotNameScale();
            return instance.renderElement(element, highlit, outOfBounds, optionalDepth,
                                          optionalScale * Globals.minimapScaleMultiplier,
                                          partialX, partialY, minimapElementRenderInfo, guiGraphics, bufferSource);
        } else {
            return instance.renderElement(element, highlit, outOfBounds, optionalDepth, optionalScale, partialX, partialY, minimapElementRenderInfo, guiGraphics, bufferSource);
        }
    }
}
