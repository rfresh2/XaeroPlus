package xaeroplus.mixin.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xaero.common.XaeroMinimapSession;
import xaero.common.minimap.element.render.MinimapElementRenderer;
import xaero.common.minimap.element.render.over.MinimapElementOverMapRendererHandler;
import xaero.common.minimap.render.MinimapRendererHelper;
import xaero.common.minimap.render.radar.element.RadarRenderContext;
import xaero.common.minimap.render.radar.element.RadarRenderer;
import xaeroplus.Globals;

@Mixin(value = MinimapElementOverMapRendererHandler.class, remap = false)
public class MixinMinimapElementOverMapRendererHandler {

    @Redirect(method = "transformAndRenderForRenderer", at = @At(
        value = "INVOKE",
        target = "Lxaero/common/minimap/element/render/MinimapElementRenderer;renderElement(IZZLnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/gui/Font;Lcom/mojang/blaze3d/pipeline/RenderTarget;Lxaero/common/minimap/render/MinimapRendererHelper;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/player/Player;DDDIDFLjava/lang/Object;DDZF)Z"
    ), remap = true)
    public boolean redirectRenderElement(final MinimapElementRenderer instance,
                                         final int location,
                                         final boolean highlit,
                                         final boolean outOfBounds,
                                         final GuiGraphics drawContext,
                                         final MultiBufferSource.BufferSource immediate,
                                         final Font fontRenderer,
                                         final RenderTarget framebuffer,
                                         final MinimapRendererHelper minimapRendererHelper,
                                         final Entity renderEntity,
                                         final Player entityPlayer,
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
                    optionalScale / Globals.minimapScalingFactor,
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
