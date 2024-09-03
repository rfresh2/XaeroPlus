package xaeroplus.mixin.client;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xaero.common.minimap.element.render.MinimapElementRenderer;
import xaero.common.minimap.element.render.over.MinimapElementOverMapRendererHandler;
import xaero.common.minimap.render.MinimapRendererHelper;
import xaero.common.minimap.render.radar.element.RadarRenderContext;
import xaero.common.minimap.render.radar.element.RadarRenderer;
import xaero.hud.HudSession;
import xaeroplus.util.Globals;

@Mixin(value = MinimapElementOverMapRendererHandler.class, remap = false)
public class MixinMinimapElementOverMapRendererHandler {

    @Redirect(method = "transformAndRenderForRenderer", at = @At(value = "INVOKE", target = "Lxaero/common/minimap/element/render/MinimapElementRenderer;renderElement(IZZLnet/minecraft/client/gui/FontRenderer;Lnet/minecraft/client/shader/Framebuffer;Lxaero/common/minimap/render/MinimapRendererHelper;Lnet/minecraft/entity/Entity;Lnet/minecraft/entity/player/EntityPlayer;DDDIDFLjava/lang/Object;DDZFLnet/minecraft/client/gui/ScaledResolution;)Z"))
    public boolean redirectRenderElement(final MinimapElementRenderer instance,
                                         final int location,
                                         final boolean highlit,
                                         final boolean outOfBounds,
                                         final FontRenderer fontRenderer,
                                         final Framebuffer framebuffer,
                                         final MinimapRendererHelper minimapRendererHelper,
                                         final Entity renderEntity,
                                         final EntityPlayer entityPlayer,
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
                                         final float partialTicks,
                                         final ScaledResolution scaledResolution) {
        if (instance instanceof RadarRenderer) {
            ((RadarRenderContext) instance.getContext()).nameScale = HudSession.getCurrentSession().getHudMod().getSettings().getDotNameScale();
            return instance.renderElement(location, highlit, outOfBounds, fontRenderer,
                    framebuffer, minimapRendererHelper, renderEntity, entityPlayer, renderX, renderY, renderZ,
                    elementIndex, optionalDepth,
                    optionalScale / Globals.minimapScalingFactor,
                    e, partialX, partialY, cave, partialTicks, scaledResolution);
        } else {
            return instance.renderElement(location, highlit, outOfBounds, fontRenderer, framebuffer, minimapRendererHelper, renderEntity, entityPlayer, renderX, renderY, renderZ, elementIndex, optionalDepth, optionalScale, e, partialX, partialY, cave, partialTicks, scaledResolution);
        }
    }
}
