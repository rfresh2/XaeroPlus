package xaeroplus.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
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

    @Inject(method = "isWorldTeleportable", at = @At("HEAD"), cancellable = true)
    public void isWorldTeleportable(final WaypointWorld displayedWorld, final CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(true);
    }

    private Waypoint selected = null;
    private WaypointWorld displayedWorld = null;
    @Inject(method = "teleportToWaypoint(Lxaero/common/minimap/waypoints/Waypoint;Lxaero/common/minimap/waypoints/WaypointWorld;Lnet/minecraft/client/gui/GuiScreen;Z)V",
    at = @At(value = "INVOKE", target = "Lxaero/common/minimap/waypoints/WaypointsManager;getDimensionDivision(Lxaero/common/minimap/waypoints/WaypointWorld;)D"))
    public void teleportToWaypointBefore(final Waypoint selected, final WaypointWorld displayedWorld, final GuiScreen screen, final boolean respectHiddenCoords, final CallbackInfo ci) {
        this.selected = selected;
        this.displayedWorld = displayedWorld;
    }

    boolean crossDimTeleport = false;

    @Redirect(method = "teleportToWaypoint(Lxaero/common/minimap/waypoints/Waypoint;Lxaero/common/minimap/waypoints/WaypointWorld;Lnet/minecraft/client/gui/GuiScreen;Z)V",
        at = @At(value = "INVOKE", target = "Lxaero/common/minimap/waypoints/WaypointWorldRootContainer;getServerTeleportCommandFormat()Ljava/lang/String;"))
    public String getTeleportCommandFormatRedirect(final WaypointWorldRootContainer instance) {
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
        crossDimTeleport = false;
        return instance.getServerTeleportCommandFormat();
    }

    @Redirect(method = "teleportToWaypoint(Lxaero/common/minimap/waypoints/Waypoint;Lxaero/common/minimap/waypoints/WaypointWorld;Lnet/minecraft/client/gui/GuiScreen;Z)V",
            at = @At(value = "INVOKE", target = "Lxaero/common/minimap/waypoints/WaypointWorldRootContainer;isUsingDefaultTeleportCommand()Z"))
    public boolean isUsingDefaultTeleportCommand(final WaypointWorldRootContainer instance) {
        return !crossDimTeleport;
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

    @Inject(
        method = "createTemporaryWaypoints(Lxaero/common/minimap/waypoints/WaypointWorld;IIIZD)V",
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
                                         double dimScale,
                                         final CallbackInfo ci,
                                         double waypointDestDimScale,
                                         final double dimDiv,
                                         final Waypoint waypoint) {
        ((IWaypointDimension) waypoint).setDimension(WaypointsHelper.getDimensionForWaypointWorldKey(wpw.getContainer().getKey()));
    }
}
