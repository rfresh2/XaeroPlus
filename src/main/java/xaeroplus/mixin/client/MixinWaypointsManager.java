package xaeroplus.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.world.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.common.minimap.waypoints.WaypointWorld;
import xaero.common.minimap.waypoints.WaypointWorldRootContainer;
import xaero.common.minimap.waypoints.WaypointsManager;
import xaeroplus.XaeroPlus;
import xaeroplus.settings.XaeroPlusSettingRegistry;
import xaeroplus.util.DataFolderResolveUtil;
import xaeroplus.util.IWaypointDimension;
import xaeroplus.util.Shared;
import xaeroplus.util.WaypointsHelper;

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

    @Inject(method = "isWorldTeleportable", at = @At("HEAD"), cancellable = true)
    public void isWorldTeleportable(final WaypointWorld displayedWorld, final CallbackInfoReturnable<Boolean> cir) {
        if (XaeroPlusSettingRegistry.crossDimensionTeleportCommand.getValue()) {
            cir.setReturnValue(true);
        }
    }

    private Waypoint selected = null;
    private WaypointWorld displayedWorld = null;
    @Inject(method = "teleportToWaypoint(Lxaero/common/minimap/waypoints/Waypoint;Lxaero/common/minimap/waypoints/WaypointWorld;Lnet/minecraft/client/gui/GuiScreen;Z)V",
    at = @At(value = "INVOKE", target = "Lxaero/common/minimap/waypoints/WaypointsManager;getDimensionDivision(Ljava/lang/String;)D"))
    public void teleportToWaypointBefore(final Waypoint selected, final WaypointWorld displayedWorld, final GuiScreen screen, final boolean respectHiddenCoords, final CallbackInfo ci) {
        this.selected = selected;
        this.displayedWorld = displayedWorld;
    }

    boolean crossDimTeleport = false;

    @Redirect(method = "teleportToWaypoint(Lxaero/common/minimap/waypoints/Waypoint;Lxaero/common/minimap/waypoints/WaypointWorld;Lnet/minecraft/client/gui/GuiScreen;Z)V",
        at = @At(value = "INVOKE", target = "Lxaero/common/minimap/waypoints/WaypointWorldRootContainer;getServerTeleportCommandFormat()Ljava/lang/String;"))
    public String getTeleportCommandFormatRedirect(final WaypointWorldRootContainer instance) {
        if (XaeroPlusSettingRegistry.crossDimensionTeleportCommand.getValue()) {
            try {
                String containerKey = displayedWorld.getContainer().getKey();
                if (containerKey.contains("dim%")) {
                    int dimId = Integer.parseInt(containerKey.split("%")[1]);
                    if (dimId != Minecraft.getMinecraft().world.provider.getDimension()) {
                        crossDimTeleport = true;
                        return "/forge setdim " + Minecraft.getMinecraft().getSession().getUsername() + " " + dimId + " " + selected.getX() + " {y} " + selected.getZ();
                    }
                }
            } catch (final Throwable e) {
                XaeroPlus.LOGGER.warn("Failed to get cross-dimension teleport command format for waypoint: {} in world: {}", selected.getName(), displayedWorld.getContainer().getKey());
            }
        }
        crossDimTeleport = false;
        return instance.getServerTeleportCommandFormat();
    }

    @Redirect(method = "teleportToWaypoint(Lxaero/common/minimap/waypoints/Waypoint;Lxaero/common/minimap/waypoints/WaypointWorld;Lnet/minecraft/client/gui/GuiScreen;Z)V",
            at = @At(value = "INVOKE", target = "Lxaero/common/minimap/waypoints/WaypointWorldRootContainer;isUsingDefaultTeleportCommand()Z"))
    public boolean isUsingDefaultTeleportCommand(final WaypointWorldRootContainer instance) {
        if (XaeroPlusSettingRegistry.crossDimensionTeleportCommand.getValue()) {
            return !crossDimTeleport;
        }
        return instance.isUsingDefaultTeleportCommand();
    }

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
            int dimKey = WaypointsHelper.getDimensionForWaypointWorldKey(worldContainerID);
            if (dimKey == -1 || dimKey == 0 || dimKey == 1) {
                double currentDimDiv = Minecraft.getMinecraft().world.provider.getDimensionType() == DimensionType.NETHER ? 8.0 : 1.0;
                double selectedDimDiv = dimKey == -1 ? 8.0 : 1.0;
                if (Minecraft.getMinecraft().world.provider.getDimension() != Shared.customDimensionId) {
                    double customDimDiv = Shared.customDimensionId == -1 ? 8.0 : 1.0;
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

    @Inject(
        method = "createTemporaryWaypoints(Lxaero/common/minimap/waypoints/WaypointWorld;IIIZ)V",
        at = @At(
            value = "INVOKE",
            target = "Lxaero/common/IXaeroMinimap;getSettings()Lxaero/common/settings/ModSettings;",
            ordinal = 1
        ),
        locals = LocalCapture.CAPTURE_FAILHARD)
    public void createTempWaypointInject(final WaypointWorld wpw,
                                         final int x,
                                         final int y,
                                         final int z,
                                         final boolean yIncluded,
                                         final CallbackInfo ci,
                                         final double dimDiv,
                                         final Waypoint waypoint) {
        ((IWaypointDimension) waypoint).setDimension(WaypointsHelper.getDimensionForWaypointWorldKey(wpw.getContainer().getKey()));
    }
}
