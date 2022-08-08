package xaeroplus.mixin.client;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.common.minimap.waypoints.WaypointsManager;

import static java.util.Objects.nonNull;

@Mixin(value = WaypointsManager.class, remap = false)
public class MixinWaypointsManager {

    @Shadow
    private Minecraft mc;

    @Inject(method = "getMainContainer", at = @At("HEAD"), cancellable = true)
    private void getMainContainer(CallbackInfoReturnable<String> cir) {
        if (nonNull(mc.getCurrentServerData())) {
            // use common directories based on server list name instead of IP
            // good for proxies
            cir.setReturnValue("Multiplayer_" + mc.getCurrentServerData().serverName);
            cir.cancel();
        }
    }
}
