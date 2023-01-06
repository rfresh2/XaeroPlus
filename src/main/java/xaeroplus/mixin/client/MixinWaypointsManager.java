package xaeroplus.mixin.client;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.common.minimap.waypoints.WaypointsManager;

import static java.util.Objects.nonNull;

@Mixin(value = WaypointsManager.class, remap = false)
public abstract class MixinWaypointsManager {

    @Shadow
    private Minecraft mc;
    @Shadow
    private String mainContainerID;
    @Shadow
    private String containerIDIgnoreCaseCache;
    @Shadow
    public abstract String ignoreContainerCase(String potentialContainerID, String current);
    @Shadow
    public abstract String getDimensionDirectoryName(int dim);

    @Inject(method = "getMainContainer", at = @At("HEAD"), cancellable = true)
    private void getMainContainer(CallbackInfoReturnable<String> cir) {
        if (nonNull(mc.getCurrentServerData())) {
            if (nonNull(mc.getCurrentServerData().serverName) & mc.getCurrentServerData().serverName.length() > 0) {
                // use common directories based on server list name instead of IP
                // good for proxies
                cir.setReturnValue("Multiplayer_" + mc.getCurrentServerData().serverName);
                cir.cancel();
            }
        }
    }

    /**
     * @author rfresh2
     * @reason overworld waypoint container always
     */
    @Overwrite
    private String getPotentialContainerID() {
        return this.ignoreContainerCase(
                this.mainContainerID + "/" + this.getDimensionDirectoryName(0), this.containerIDIgnoreCaseCache
        );
    }

}
