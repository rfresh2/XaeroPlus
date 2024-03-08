package xaeroplus.forge.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.XaeroMinimapSession;
import xaero.common.interfaces.render.InterfaceRenderer;
import xaeroplus.XaeroPlus;
import xaeroplus.event.MinimapRenderEvent;

@Mixin(value = InterfaceRenderer.class, remap = false)
public class MixinInterfaceRendererGuiOverlay {
    private MinimapRenderEvent minimapRenderEvent;
    @Inject(method = "renderInterfaces", at = @At("HEAD"), cancellable = true)
    public void renderMinimapHead(final XaeroMinimapSession minimapSession, final PoseStack matrixStack, final float partial, final CallbackInfo ci) {
        minimapRenderEvent = new MinimapRenderEvent();
        XaeroPlus.EVENT_BUS.call(minimapRenderEvent);
        if (minimapRenderEvent.cancelled) ci.cancel();
    }

    @Inject(method = "renderInterfaces", at = @At("RETURN"))
    public void renderMapReturn(final XaeroMinimapSession minimapSession, final PoseStack matrixStack, final float partial, final CallbackInfo ci) {
        if (minimapRenderEvent.postRenderCallback != null) minimapRenderEvent.postRenderCallback.run();
    }
}
