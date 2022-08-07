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
        // todo: restore ctrl zoom
        destScale *= Math.pow(1.2D, factor);
        ci.cancel();
    }
}
