package xaeroplus.fabric.mixin.client.mc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaeroplus.fabric.util.compat.IncompatibleMinimapWarningScreen;

@Mixin(Minecraft.class)
public class MixinMinecraftClientFabric {
    @Shadow
    public Screen screen;

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    public void preventMinimapIncompatibleScreenFromClosing(final Screen guiScreen, final CallbackInfo ci) {
        if (this.screen instanceof IncompatibleMinimapWarningScreen) {
            ci.cancel();
        }
    }
}
