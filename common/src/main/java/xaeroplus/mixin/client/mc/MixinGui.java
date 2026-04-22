package xaeroplus.mixin.client.mc;

import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaeroplus.settings.Settings;
import xaeroplus.util.GuiMapHelper;

@Mixin(Gui.class)
public class MixinGui {

    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    public void cancelGuiRenderWhileInTransparentWorldMap(final CallbackInfo ci) {
        if (GuiMapHelper.isGuiMapLoaded() && Settings.REGISTRY.transparentWorldmapBackgroundSetting.get()) ci.cancel();
    }
}
