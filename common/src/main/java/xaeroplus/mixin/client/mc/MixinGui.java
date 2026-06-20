package xaeroplus.mixin.client.mc;

import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Gui.class)
public class MixinGui {

//    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
//    public void cancelGuiRenderWhileInTransparentWorldMap(final CallbackInfo ci) {
//        if (Settings.REGISTRY.transparentWorldmapBackgroundSetting.get() && GuiMapHelper.isGuiMapLoaded()) ci.cancel();
//    }
}
