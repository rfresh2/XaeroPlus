package xaeroplus.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.events.ModClientEvents;
import xaeroplus.XaeroPlus;
import xaeroplus.event.MinimapRenderEvent;

@Mixin(value = ModClientEvents.class, remap = false)
public class MixinModClientEvents {
    private MinimapRenderEvent minimapRenderEvent;

    @Inject(method = "handleRenderModOverlay", at = @At("HEAD"), cancellable = true)
    public void handleRenderModOverlayHead(final PoseStack guiGraphics, final float partialTicks, final CallbackInfo ci) {
        minimapRenderEvent = new MinimapRenderEvent();
        XaeroPlus.EVENT_BUS.call(minimapRenderEvent);
        if (minimapRenderEvent.cancelled) ci.cancel();
    }

    @Inject(method = "handleRenderModOverlay", at = @At("RETURN"))
    public void handleRenderModOverlayReturn(final PoseStack guiGraphics, final float partialTicks, final CallbackInfo ci) {
        if (minimapRenderEvent.postRenderCallback != null) minimapRenderEvent.postRenderCallback.run();
    }
}
