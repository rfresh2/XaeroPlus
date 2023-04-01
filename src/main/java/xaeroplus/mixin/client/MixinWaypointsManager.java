package xaeroplus.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.common.minimap.waypoints.WaypointsManager;
import xaeroplus.XaeroPlus;
import xaeroplus.settings.XaeroPlusSettingRegistry;
import xaeroplus.util.DataFolderResolveUtil;

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
    @Shadow
    public abstract Integer getDimensionForDirectoryName(String dirName);

    @Inject(method = "getMainContainer", at = @At("HEAD"), cancellable = true)
    private void getMainContainer(CallbackInfoReturnable<String> cir) {
        DataFolderResolveUtil.resolveDataFolder(cir);
    }

    @Inject(method = "getPotentialContainerID", at = @At("HEAD"), cancellable = true)
    private void getPotentialContainerID(CallbackInfoReturnable<String> cir) {
        if (!XaeroPlusSettingRegistry.owAutoWaypointDimension.getValue()) return;
        int dimension = this.mc.world.provider.getDimension();
        if (dimension == 0 || dimension == -1) {
            dimension = 0;
        }
        cir.setReturnValue(this.ignoreContainerCase(
                this.mainContainerID + "/" + this.getDimensionDirectoryName(dimension), this.containerIDIgnoreCaseCache)
        );
    }

    /**
     * @author rfresh2
     * @reason custom dimension support
     */
    @Overwrite
    public double getDimensionDivision(String worldContainerID) {
        if (worldContainerID != null && Minecraft.getMinecraft().world != null) {
            String dimPart = worldContainerID.substring(worldContainerID.lastIndexOf(47) + 1);
            Integer dimKey = this.getDimensionForDirectoryName(dimPart);
            if (dimKey != null && (dimKey == -1 || dimKey == 0 || dimKey == 1)) {
                double currentDimDiv = Minecraft.getMinecraft().world.provider.getDimensionType() == DimensionType.NETHER ? 8.0 : 1.0;
                double selectedDimDiv = dimKey == -1 ? 8.0 : 1.0;
                if (Minecraft.getMinecraft().world.provider.getDimension() != XaeroPlus.customDimensionId) {
                    double customDimDiv = XaeroPlus.customDimensionId == -1 ? 8.0 : 1.0;
                    return customDimDiv / selectedDimDiv;
                }
                return currentDimDiv / selectedDimDiv;
            } else {
                return 1.0;
            }
        } else {
            return 1.0;
        }
    }

}
