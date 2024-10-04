package xaeroplus.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.common.IXaeroMinimap;
import xaero.common.minimap.element.render.MinimapElementRenderer;
import xaero.common.minimap.element.render.MinimapElementRendererHandler;
import xaero.common.minimap.render.MinimapRendererHelper;
import xaero.common.minimap.render.radar.element.RadarRenderContext;
import xaeroplus.util.IScreenRadarRenderContext;

@Mixin(value = MinimapElementRendererHandler.class, remap = false)
public class MixinMinimapElementRendererHandler {

    @Inject(
        method = "renderForRenderer",
        at = @At(
            value = "INVOKE",
            target = "Lxaero/common/minimap/element/render/MinimapElementRenderer;preRender(ILnet/minecraft/entity/Entity;Lnet/minecraft/entity/player/EntityPlayer;DDDLnet/minecraft/client/gui/ScaledResolution;Lxaero/common/IXaeroMinimap;)V"
        ))
    public void renderForRendererInject(MinimapElementRenderer renderer,
                                        Entity renderEntity,
                                        EntityPlayer player,
                                        double renderX,
                                        double renderY,
                                        double renderZ,
                                        double playerDimDiv,
                                        double ps,
                                        double pc,
                                        double zoom,
                                        boolean cave,
                                        float partialTicks,
                                        int elementIndex,
                                        Framebuffer framebuffer,
                                        IXaeroMinimap modMain,
                                        MinimapRendererHelper helper,
                                        FontRenderer font,
                                        ScaledResolution scaledRes,
                                        int indexLimit,
                                        CallbackInfoReturnable<Integer> cir,
                                        @Local(name = "context") Object context) {
        if (context instanceof RadarRenderContext) {
            ((IScreenRadarRenderContext) context).setIsWorldMap(false);
        }
    }
}
