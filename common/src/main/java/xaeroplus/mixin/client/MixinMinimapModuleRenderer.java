package xaeroplus.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import xaero.hud.minimap.module.MinimapRenderer;

@Mixin(value = MinimapRenderer.class, remap = false)
public class MixinMinimapModuleRenderer {

//    @Inject(method = "render(Lxaero/hud/minimap/module/MinimapSession;Lxaero/hud/render/module/ModuleRenderContext;Lnet/minecraft/client/gui/GuiGraphicsExtractor;F)V", at = @At("HEAD"), cancellable = true)
//    public void cancelWhileInTransparentWm(CallbackInfo ci) {
//        if (GuiMapHelper.isGuiMapLoaded() && Settings.REGISTRY.transparentWorldmapBackgroundSetting.get()) ci.cancel();
//    }
}
