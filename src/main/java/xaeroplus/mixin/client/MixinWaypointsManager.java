package xaeroplus.mixin.client;

import com.google.common.net.InternetDomainName;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.common.minimap.waypoints.WaypointsManager;
import xaeroplus.XaeroPlus;
import xaeroplus.settings.XaeroPlusSettingRegistry;

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
        final XaeroPlus.DataFolderResolutionMode dataFolderResolutionMode = XaeroPlus.dataFolderResolutionMode;
        if (dataFolderResolutionMode == XaeroPlus.DataFolderResolutionMode.SERVER_NAME) {
            Minecraft mc = Minecraft.getMinecraft();
            if (nonNull(mc.getCurrentServerData()) && mc.getCurrentServerData().serverName.length() > 0) {
                // use common directories based on server list name instead of IP
                // good for proxies
                cir.setReturnValue("Multiplayer_" + mc.getCurrentServerData().serverName);
                cir.cancel();
            }
        } else if (dataFolderResolutionMode == XaeroPlus.DataFolderResolutionMode.BASE_DOMAIN) {
            Minecraft mc = Minecraft.getMinecraft();
            if (nonNull(mc.getCurrentServerData())) {
                // use the base domain name, e.g connect.2b2t.org -> 2b2t.org
                String id;
                try {
                    id = InternetDomainName.from(mc.getCurrentServerData().serverIP).topPrivateDomain().toString();
                } catch (IllegalArgumentException ex) { // not a domain
                    id = mc.getCurrentServerData().serverIP;
                }
                id = "Multiplayer_" + id;
                cir.setReturnValue(id);
                cir.cancel();
            }
        }
    }

    @Inject(method = "getPotentialContainerID", at = @At("HEAD"), cancellable = true)
    private void getPotentialContainerID(CallbackInfoReturnable<String> cir) {
        if (!XaeroPlusSettingRegistry.owAutoWaypointDimension.getBooleanSettingValue()) return;
        int dimension = this.mc.world.provider.getDimension();
        if (dimension == 0 || dimension == -1) {
            dimension = 0;
        }
        cir.setReturnValue(this.ignoreContainerCase(
                this.mainContainerID + "/" + this.getDimensionDirectoryName(dimension), this.containerIDIgnoreCaseCache)
        );
    }

}
