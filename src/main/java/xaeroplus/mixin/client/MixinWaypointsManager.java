package xaeroplus.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;
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
import xaeroplus.Globals;
import xaeroplus.XaeroPlus;
import xaeroplus.feature.extensions.IWaypointDimension;
import xaeroplus.settings.XaeroPlusSettingRegistry;
import xaeroplus.util.DataFolderResolveUtil;
import xaeroplus.util.WaypointsHelper;

import static net.minecraft.world.World.NETHER;
import static net.minecraft.world.World.OVERWORLD;

@Mixin(value = WaypointsManager.class, remap = false)
public abstract class MixinWaypointsManager {

    @Shadow
    private MinecraftClient mc;
    @Shadow
    private String mainContainerID;
    @Shadow
    private String containerIDIgnoreCaseCache;

    @Shadow
    public abstract String ignoreContainerCase(String potentialContainerID, String current);

    @Shadow
    public abstract String getDimensionDirectoryName(RegistryKey<World> dimKey);
    @Shadow
    public abstract RegistryKey<World> getDimensionKeyForDirectoryName(String dirName);

    @Inject(method = "isWorldTeleportable", at = @At("HEAD"), cancellable = true)
    public void isWorldTeleportable(final WaypointWorld displayedWorld, final CallbackInfoReturnable<Boolean> cir) {
        if (XaeroPlusSettingRegistry.crossDimensionTeleportCommand.getValue()) {
            cir.setReturnValue(true);
        }
    }

    private Waypoint selected = null;
    private WaypointWorld displayedWorld = null;
    @Inject(method = "teleportToWaypoint(Lxaero/common/minimap/waypoints/Waypoint;Lxaero/common/minimap/waypoints/WaypointWorld;Lnet/minecraft/client/gui/screen/Screen;Z)V",
        at = @At(value = "INVOKE", target = "Lxaero/common/minimap/waypoints/WaypointsManager;getDimensionDivision(Ljava/lang/String;)D"), remap = true)
    public void teleportToWaypointBefore(final Waypoint selected, final WaypointWorld displayedWorld, final Screen screen, final boolean respectHiddenCoords, final CallbackInfo ci) {
        this.selected = selected;
        this.displayedWorld = displayedWorld;
    }

    boolean crossDimTeleport = false;

    @Redirect(method = "teleportToWaypoint(Lxaero/common/minimap/waypoints/Waypoint;Lxaero/common/minimap/waypoints/WaypointWorld;Lnet/minecraft/client/gui/screen/Screen;Z)V",
        at = @At(value = "INVOKE", target = "Lxaero/common/minimap/waypoints/WaypointWorldRootContainer;getServerTeleportCommandFormat()Ljava/lang/String;"), remap = true)
    public String getTeleportCommandFormatRedirect(final WaypointWorldRootContainer instance) {
        if (XaeroPlusSettingRegistry.crossDimensionTeleportCommand.getValue()) {
            try {
                // todo: if the dest is a waystone:
                //  ideally we'd be teleported offset by 0.5 x or z in the direction the waystone is facing
                //  however, we don't have access to the facing state from the client directly
                //  and we can't send a specific waystone teleport request packet as our teleport source is non-standard
                //  perhaps if we had a deeper integration into the waystones codebase we could do this better
                //  we could increase the dest Y val by 0.5 or something as a slight improvement

                RegistryKey<World> waypointDimension = ((IWaypointDimension) selected).getDimension();
                RegistryKey<World> currentPlayerDim = MinecraftClient.getInstance().world.getRegistryKey();
                if (waypointDimension.getValue() != currentPlayerDim.getValue()) {
                    crossDimTeleport = true;
                    return "/execute in " + waypointDimension.getValue() + " run teleport @s " + selected.getX() + " {y} " + selected.getZ();
                }
            } catch (final Throwable e) {
                XaeroPlus.LOGGER.warn("Failed to get cross-dimension teleport command format for waypoint: {} in world: {}", selected.getName(), displayedWorld.getContainer().getKey(), e);
            }
        }
        crossDimTeleport = false;
        return instance.getServerTeleportCommandFormat();
    }

    @Inject(method = "isTeleportationSafe", at = @At("HEAD"), cancellable = true)
    public void isTeleportationSafeInject(final WaypointWorld displayedWorld, final CallbackInfoReturnable<Boolean> cir) {
        if (XaeroPlusSettingRegistry.crossDimensionTeleportCommand.getValue()) {
            // xD
            cir.setReturnValue(true);
        }
    }

    @Redirect(method = "teleportToWaypoint(Lxaero/common/minimap/waypoints/Waypoint;Lxaero/common/minimap/waypoints/WaypointWorld;Lnet/minecraft/client/gui/screen/Screen;Z)V",
        at = @At(value = "INVOKE", target = "Lxaero/common/minimap/waypoints/WaypointWorldRootContainer;isUsingDefaultTeleportCommand()Z"), remap = true)
    public boolean isUsingDefaultTeleportCommand(final WaypointWorldRootContainer instance) {
        if (XaeroPlusSettingRegistry.crossDimensionTeleportCommand.getValue()) {
            return !crossDimTeleport;
        }
        return instance.isUsingDefaultTeleportCommand();
    }

    @Inject(method = "getMainContainer", at = @At("HEAD"), cancellable = true)
    private void getMainContainer(ClientPlayNetworkHandler connection, CallbackInfoReturnable<String> cir) {
        DataFolderResolveUtil.resolveDataFolder(connection, cir);
    }

    @Inject(method = "getPotentialContainerID", at = @At("HEAD"), cancellable = true)
    private void getPotentialContainerID(CallbackInfoReturnable<String> cir) {
        if (!XaeroPlusSettingRegistry.owAutoWaypointDimension.getValue()) return;
        RegistryKey<World> dimension = mc.world.getRegistryKey();
        if (dimension == OVERWORLD || dimension == NETHER) {
            dimension = OVERWORLD;
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
        if (worldContainerID != null && MinecraftClient.getInstance().world != null) {
            String dimPart = worldContainerID.substring(worldContainerID.lastIndexOf(47) + 1);
            RegistryKey<World> dimKey = this.getDimensionKeyForDirectoryName(dimPart);
            if (dimKey != World.NETHER && dimKey != World.OVERWORLD && dimKey != World.END) {
                return 1.0;
            } else {
                double currentDimDiv = MinecraftClient.getInstance().world.getDimension().coordinateScale();
                double selectedDimDiv = dimKey == World.NETHER ? 8.0 : 1.0;
                if (MinecraftClient.getInstance().world.getRegistryKey() != Globals.customDimensionId) {
                    double customDimDiv = Globals.customDimensionId == NETHER ? 8.0 : 1.0;
                    return customDimDiv / selectedDimDiv;
                }
                return currentDimDiv / selectedDimDiv;
            }
        } else {
            return 1.0;
        }
    }

    @Inject(
        method = "createTemporaryWaypoints(Lxaero/common/minimap/waypoints/WaypointWorld;IIIZ)V",
        at = @At(
            value = "INVOKE",
            target = "Lxaero/common/AXaeroMinimap;getSettings()Lxaero/common/settings/ModSettings;",
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
        ((IWaypointDimension) waypoint).setDimension(WaypointsHelper.getDimensionKeyForWaypointWorldKey(wpw.getContainer().getKey()));
    }
}
