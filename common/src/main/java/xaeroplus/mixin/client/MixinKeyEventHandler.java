package xaeroplus.mixin.client;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.IXaeroMinimap;
import xaero.common.XaeroMinimapSession;
import xaero.common.controls.event.KeyEventHandler;
import xaeroplus.settings.Settings;

@Mixin(value = KeyEventHandler.class, remap = false)
public class MixinKeyEventHandler {
    private static boolean init = false;
    @Inject(method = "onKeyInput", at = @At("HEAD"))
    public void onKeyInputInject(final Minecraft mc, final IXaeroMinimap modMain, final XaeroMinimapSession minimapSession, final CallbackInfo ci) {
        if (init) return;
        init = true;
        modMain.getControlsRegister().keybindings.addAll(Settings.REGISTRY.getKeybindings());
    }
}
