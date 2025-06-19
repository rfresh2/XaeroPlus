package xaeroplus.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.minimap.render.MinimapDepthTraceListener;
import xaeroplus.XaeroPlus;
import xaeroplus.event.MinimapRenderEvent;

@Mixin(value = MinimapDepthTraceListener.class, remap = false)
public class MixinMinimapDepthTraceListener {
    private MinimapRenderEvent minimapRenderEvent = null;

    @Inject(method = "accept", at = @At("HEAD"), cancellable = true)
    public void beforeMinimapRender(final float depth, final CallbackInfo ci) {
        minimapRenderEvent = new MinimapRenderEvent();
        XaeroPlus.EVENT_BUS.call(minimapRenderEvent);
        if (minimapRenderEvent.cancelled) ci.cancel();
    }

    @Inject(method = "accept", at = @At("RETURN"))
    public void afterMinimapRender(final float depth, final CallbackInfo ci) {
        if (minimapRenderEvent.postRenderCallback != null) minimapRenderEvent.postRenderCallback.run();
    }
}
