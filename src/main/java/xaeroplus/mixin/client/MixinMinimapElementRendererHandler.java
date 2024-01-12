package xaeroplus.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.common.AXaeroMinimap;
import xaero.common.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider;
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
            target = "Lxaero/common/minimap/element/render/MinimapElementRenderer;preRender(ILnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/player/Player;DDDLxaero/common/AXaeroMinimap;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lxaero/common/graphics/renderer/multitexture/MultiTextureRenderTypeRendererProvider;)V"
        ), remap = true)
    public void renderForRendererInject(MinimapElementRenderer renderer,
                                        GuiGraphics guiGraphics,
                                        Entity renderEntity,
                                        Player player,
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
                                        RenderTarget framebuffer,
                                        AXaeroMinimap modMain,
                                        MinimapRendererHelper helper,
                                        MultiBufferSource.BufferSource renderTypeBuffers,
                                        Font font,
                                        MultiTextureRenderTypeRendererProvider multiTextureRenderTypeRenderers,
                                        int indexLimit,
                                        CallbackInfoReturnable<Integer> cir,
                                        @Local(name = "context") LocalRef<Object> contextRef) {
        if (contextRef.get() instanceof RadarRenderContext) {
            ((IScreenRadarRenderContext) contextRef.get()).setIsWorldMap(false);
        }
    }
}
