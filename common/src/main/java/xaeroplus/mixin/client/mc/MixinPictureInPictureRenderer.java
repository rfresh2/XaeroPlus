package xaeroplus.mixin.client.mc;

import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.hud.minimap.render.MinimapPipRenderer;
import xaeroplus.XaeroPlus;
import xaeroplus.event.MinimapRenderEvent;

@Mixin(PictureInPictureRenderer.class)
public class MixinPictureInPictureRenderer {
    @Inject(method = "textureIsReadyToBlit", at = @At("HEAD"), cancellable = true)
    protected void textureIsReadyToBlit(final CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof MinimapPipRenderer)) {
            return;
        }
        var event = new MinimapRenderEvent();
        XaeroPlus.EVENT_BUS.call(event);
        if (event.cancelled) {
            cir.setReturnValue(true);
        }
    }
}

