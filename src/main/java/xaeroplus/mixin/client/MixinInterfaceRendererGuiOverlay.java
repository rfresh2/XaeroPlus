package xaeroplus.mixin.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaeroplus.XaeroPlus;
import xaeroplus.event.MinimapRenderEvent;

@Mixin(targets = "xaero.common.interfaces.render.InterfaceRenderer$1", remap = false)
public class MixinInterfaceRendererGuiOverlay {
    private MinimapRenderEvent minimapRenderEvent;
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public void renderMinimapHead(final ForgeGui gui, final GuiGraphics guiGraphics, final float partialTicks, final int width, final int height, final CallbackInfo ci) {
        minimapRenderEvent = new MinimapRenderEvent();
        XaeroPlus.EVENT_BUS.call(minimapRenderEvent);
        if (minimapRenderEvent.cancelled) ci.cancel();
    }

    @Inject(method = "render", at = @At("RETURN"))
    public void renderMapReturn(final ForgeGui gui, final GuiGraphics guiGraphics, final float partialTicks, final int width, final int height, final CallbackInfo ci) {
        if (minimapRenderEvent.postRenderCallback != null) minimapRenderEvent.postRenderCallback.run();
    }
}
