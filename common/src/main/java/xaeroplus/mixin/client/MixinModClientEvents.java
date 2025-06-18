package xaeroplus.mixin.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.events.ModClientEvents;
import xaeroplus.event.MinimapRenderEvent;

@Mixin(value = ModClientEvents.class, remap = false)
public class MixinModClientEvents {
    private MinimapRenderEvent minimapRenderEvent;

    @Inject(method = "handleRenderModOverlay", at = @At("HEAD"), cancellable = true)
    public void handleRenderModOverlayHead(final GuiGraphics guiGraphics, final DeltaTracker deltaTracker, final CallbackInfo ci) {
        // todo: Update for 1.21.6
        //  Rendering now happens in GuiElement interfaces
        //  this xaero event is now simply registering those interfaces for MC to call at a later point
        //  i.e. the rendering is no longer happening at this injection point
//        minimapRenderEvent = new MinimapRenderEvent(guiGraphics);
//        XaeroPlus.EVENT_BUS.call(minimapRenderEvent);
//        if (minimapRenderEvent.cancelled) ci.cancel();
    }

    @Inject(method = "handleRenderModOverlay", at = @At("RETURN"))
    public void handleRenderModOverlayReturn(final GuiGraphics guiGraphics, final DeltaTracker deltaTracker, final CallbackInfo ci) {
//        if (minimapRenderEvent.postRenderCallback != null) minimapRenderEvent.postRenderCallback.run();
    }
}
