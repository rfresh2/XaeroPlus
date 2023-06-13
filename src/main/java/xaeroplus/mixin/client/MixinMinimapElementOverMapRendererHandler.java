package xaeroplus.mixin.client;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xaero.common.XaeroMinimapSession;
import xaero.common.minimap.element.render.MinimapElementRenderer;
import xaero.common.minimap.element.render.over.MinimapElementOverMapRendererHandler;
import xaero.common.minimap.render.MinimapRendererHelper;
import xaero.common.minimap.render.radar.element.RadarRenderContext;
import xaero.common.minimap.render.radar.element.RadarRenderer;
import xaeroplus.util.Shared;

@Mixin(value = MinimapElementOverMapRendererHandler.class, remap = false)
public class MixinMinimapElementOverMapRendererHandler {

    @Redirect(method = "transformAndRenderForRenderer", at = @At(value = "INVOKE", target = "Lxaero/common/minimap/element/render/MinimapElementRenderer;renderElement(IZZLnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/client/gl/Framebuffer;Lxaero/common/minimap/render/MinimapRendererHelper;Lnet/minecraft/entity/Entity;Lnet/minecraft/entity/player/PlayerEntity;DDDIDFLjava/lang/Object;DDZF)Z"))
    public boolean redirectRenderElement(final MinimapElementRenderer instance,
                                         final int location,
                                         final boolean highlit,
                                         final boolean outOfBounds,
                                         final DrawContext drawContext,
                                         final VertexConsumerProvider.Immediate immediate,
                                         final TextRenderer fontRenderer,
                                         final Framebuffer framebuffer,
                                         final MinimapRendererHelper minimapRendererHelper,
                                         final Entity renderEntity,
                                         final PlayerEntity entityPlayer,
                                         final double renderX,
                                         final double renderY,
                                         final double renderZ,
                                         final int elementIndex,
                                         final double optionalDepth,
                                         final float optionalScale,
                                         final Object e,
                                         final double partialX,
                                         final double partialY,
                                         final boolean cave,
                                         final float partialTicks) {
        if (instance instanceof RadarRenderer) {
            ((RadarRenderContext) instance.getContext()).nameScale = XaeroMinimapSession.getCurrentSession().getModMain().getSettings().getDotNameScale();
            return instance.renderElement(location, highlit, outOfBounds, drawContext, immediate, fontRenderer,
                    framebuffer, minimapRendererHelper, renderEntity, entityPlayer, renderX, renderY, renderZ,
                    elementIndex, optionalDepth,
                    optionalScale / Shared.minimapScalingFactor,
                    e, partialX, partialY, cave, partialTicks);
        } else {
            return instance.renderElement(location, highlit, outOfBounds, drawContext, immediate, fontRenderer,
                                          framebuffer, minimapRendererHelper, renderEntity, entityPlayer, renderX, renderY, renderZ,
                                          elementIndex, optionalDepth,
                                          optionalScale,
                                          e, partialX, partialY, cave, partialTicks);
        }
    }
}
