package xaeroplus.mixin.client;

import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.map.animation.SlowingAnimation;
import xaero.map.gui.GuiMap;
import xaero.map.gui.IRightClickableElement;
import xaero.map.gui.ScreenBase;

@Mixin(value = GuiMap.class, remap = false)
public abstract class MixinGuiMap extends ScreenBase implements IRightClickableElement {

    protected MixinGuiMap(GuiScreen parent, GuiScreen escape) {
        super(parent, escape);
    }

    @Shadow
    protected abstract void closeDropdowns();

    @Shadow
    private static double destScale;
    @Shadow
    private int lastZoomMethod;
    @Shadow
    private SlowingAnimation cameraDestinationAnimX = null;
    @Shadow
    private SlowingAnimation cameraDestinationAnimZ = null;

    @Inject(method = "changeZoom", at = @At(value = "HEAD"), cancellable = true)
    private void changeZoom(double factor, int zoomMethod, CallbackInfo ci) {
        destScale *= Math.pow(1.2D, factor);
        ci.cancel();
    }
//    @Redirect(method = "changeZoom", at = @At(value = "HEAD"))
//    private void changeZoom(double factor, int zoomMethod) {
//        this.closeDropdowns();
//        this.lastZoomMethod = zoomMethod;
//        this.cameraDestinationAnimX = null;
//        this.cameraDestinationAnimZ = null;
//        if (isCtrlKeyDown()) {
//            double destScaleBefore = destScale;
//            if (destScale >= 1.0D) {
//                if (factor > 0.0D) {
//                    destScale = Math.ceil(destScale);
//                } else {
//                    destScale = Math.floor(destScale);
//                }
//
//                if (destScaleBefore == destScale) {
//                    destScale += factor > 0.0D ? 1.0D : -1.0D;
//                }
//
//                if (destScale == 0.0D) {
//                    destScale = 0.5D;
//                }
//            } else {
//                double reversedScale = 1.0D / destScale;
//                double log2 = Math.log(reversedScale) / Math.log(2.0D);
//                if (factor > 0.0D) {
//                    log2 = Math.floor(log2);
//                } else {
//                    log2 = Math.ceil(log2);
//                }
//
//                destScale = 1.0D / Math.pow(2.0D, log2);
//                if (destScaleBefore == destScale) {
//                    destScale = 1.0D / Math.pow(2.0D, log2 + (double) (factor > 0.0D ? -1 : 1));
//                }
//            }
//        } else {
//            destScale *= Math.pow(1.2D, factor);
//        }
//
//        if (destScale < 0.0025D) {
//            destScale = 0.0025D;
//        } else if (destScale > 50.0D) {
//            destScale = 50.0D;
//        }
//    }
}
