package xaeroplus.mixin.client;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xaero.common.core.IPictureInPictureRenderer;
import xaero.hud.minimap.render.MinimapPipRenderState;
import xaero.hud.minimap.render.MinimapPipRenderer;
import xaeroplus.XaeroPlus;
import xaeroplus.event.MinimapRenderEvent;

@Mixin(value = MinimapPipRenderer.class, remap = false)
public class MixinMinimapPipRenderer {

    @WrapWithCondition(method = "prepare(Lxaero/hud/minimap/render/MinimapPipRenderState;Lnet/minecraft/client/renderer/state/gui/GuiRenderState;Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher;I)V", at = @At(
        value = "INVOKE",
        target = "Lxaero/common/core/IPictureInPictureRenderer;xaero_mm_prepareTexturesAndProjection(ZII)V"
    ))
    public boolean prepareTexturesCheckRenderCancel(
        final IPictureInPictureRenderer instance, final boolean needsResize, final int width, final int height,
        @Share(value = "cancelRender") LocalBooleanRef cancelRenderRef
    ) {
        if (needsResize) {
            cancelRenderRef.set(false);
        } else {
            var event = new MinimapRenderEvent();
            XaeroPlus.EVENT_BUS.call(event);
            cancelRenderRef.set(event.cancelled);
        }
        return !cancelRenderRef.get();
    }

    @WrapWithCondition(method = "prepare(Lxaero/hud/minimap/render/MinimapPipRenderState;Lnet/minecraft/client/renderer/state/gui/GuiRenderState;Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher;I)V", at = @At(
        value = "INVOKE",
        target = "Lxaero/hud/minimap/render/MinimapPipRenderer;renderToTexture(Lxaero/hud/minimap/render/MinimapPipRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;)V"
    ))
    public boolean checkRenderToTextureCancel(
        final MinimapPipRenderer instance, final MinimapPipRenderState state, final PoseStack unused1, final SubmitNodeCollector unused2,
        @Share(value = "cancelRender") LocalBooleanRef cancelRenderRef
    ) {
        return !cancelRenderRef.get();
    }

}
