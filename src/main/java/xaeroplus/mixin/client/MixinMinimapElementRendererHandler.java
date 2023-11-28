package xaeroplus.mixin.client;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import xaero.common.IXaeroMinimap;
import xaero.common.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider;
import xaero.common.minimap.element.render.MinimapElementReader;
import xaero.common.minimap.element.render.MinimapElementRenderProvider;
import xaero.common.minimap.element.render.MinimapElementRenderer;
import xaero.common.minimap.element.render.MinimapElementRendererHandler;
import xaero.common.minimap.render.MinimapRendererHelper;
import xaero.common.minimap.render.radar.element.RadarRenderContext;
import xaeroplus.feature.extensions.IScreenRadarRenderContext;

@Mixin(value = MinimapElementRendererHandler.class, remap = false)
public class MixinMinimapElementRendererHandler {

    @Inject(
        method = "renderForRenderer",
        at = @At(
            value = "INVOKE",
            target = "Lxaero/common/minimap/element/render/MinimapElementRenderer;preRender(ILnet/minecraft/entity/Entity;Lnet/minecraft/entity/player/PlayerEntity;DDDLxaero/common/IXaeroMinimap;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lxaero/common/graphics/renderer/multitexture/MultiTextureRenderTypeRendererProvider;)V"
        ), locals = LocalCapture.CAPTURE_FAILHARD, remap = true)
    public void renderForRendererInject(MinimapElementRenderer renderer,
                                        DrawContext guiGraphics,
                                        Entity renderEntity,
                                        PlayerEntity player,
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
                                        VertexConsumerProvider.Immediate renderTypeBuffers,
                                        TextRenderer font,
                                        MultiTextureRenderTypeRendererProvider multiTextureRenderTypeRenderers,
                                        int indexLimit,
                                        CallbackInfoReturnable<Integer> cir,
                                        MinimapElementReader elementReader,
                                        MinimapElementRenderProvider provider,
                                        Object context,
                                        int location) {
        if (context instanceof RadarRenderContext) {
            ((IScreenRadarRenderContext) context).setIsWorldMap(false);
        }
    }
}
