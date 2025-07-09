package xaeroplus.mixin.client;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xaero.hud.minimap.module.MinimapRenderer;
import xaero.hud.minimap.module.MinimapSession;
import xaeroplus.feature.extensions.DrawOrderScreen;

@Mixin(value = MinimapRenderer.class, remap = false)
public class MixinHudMinimapRenderer {
    @Redirect(method = "render(Lxaero/hud/minimap/module/MinimapSession;Lxaero/hud/render/module/ModuleRenderContext;Lnet/minecraft/client/gui/GuiGraphics;F)V", at = @At(
        value = "INVOKE",
        target = "Lxaero/hud/minimap/module/MinimapSession;getHideMinimapUnderScreen()Z"
    ), remap = true) // $REMAP
    public boolean allowMinimapRenderOnTopOf(final MinimapSession instance) {
        var original = instance.getHideMinimapUnderScreen();
        if (!original) return original;

        // awkward mixin, invert condition on screens we want to render minimap under
        var screen = Minecraft.getInstance().screen;
        if (screen instanceof DrawOrderScreen) {
            return false;
        }

        return original;
    }
}
