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
import xaero.common.minimap.waypoints.Waypoint;
import xaero.common.minimap.waypoints.WaypointWorld;
import xaero.common.minimap.waypoints.WaypointWorldRootContainer;
import xaero.common.minimap.waypoints.WaypointsManager;
import xaeroplus.XaeroPlus;
import xaeroplus.settings.XaeroPlusSettingRegistry;
import xaeroplus.util.DataFolderResolveUtil;
import xaeroplus.util.Shared;

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
        at = @At(value = "INVOKE", target = "Lxaero/common/minimap/waypoints/WaypointsManager;getDimensionDivision(Ljava/lang/String;)D"))
    public void teleportToWaypointBefore(final Waypoint selected, final WaypointWorld displayedWorld, final Screen screen, final boolean respectHiddenCoords, final CallbackInfo ci) {
        this.selected = selected;
        this.displayedWorld = displayedWorld;
    }

    boolean crossDimTeleport = false;

    @Redirect(method = "teleportToWaypoint(Lxaero/common/minimap/waypoints/Waypoint;Lxaero/common/minimap/waypoints/WaypointWorld;Lnet/minecraft/client/gui/screen/Screen;Z)V",
        at = @At(value = "INVOKE", target = "Lxaero/common/minimap/waypoints/WaypointWorldRootContainer;getServerTeleportCommandFormat()Ljava/lang/String;"))
    public String getTeleportCommandFormatRedirect(final WaypointWorldRootContainer instance) {
        if (XaeroPlusSettingRegistry.crossDimensionTeleportCommand.getValue()) {
            try {
                final String containerKey = displayedWorld.getContainer().getKey();
                final RegistryKey<World> dimId = getDimensionKeyForDirectoryName(containerKey);
                if (containerKey.contains("dim%")) {
                    if (dimId != MinecraftClient.getInstance().world.getRegistryKey()) {
                        crossDimTeleport = true;
                        return "/execute in " + dimId.getValue() + " run teleport @s " + selected.getX() + " {y} " + selected.getZ();
                    }
                }
            } catch (final Throwable e) {
                XaeroPlus.LOGGER.warn("Failed to get cross-dimension teleport command format for waypoint: {} in world: {}", selected.getName(), displayedWorld.getContainer().getKey());
            }
        }
        crossDimTeleport = false;
        return instance.getServerTeleportCommandFormat();
    }

    @Redirect(method = "teleportToWaypoint(Lxaero/common/minimap/waypoints/Waypoint;Lxaero/common/minimap/waypoints/WaypointWorld;Lnet/minecraft/client/gui/screen/Screen;Z)V",
        at = @At(value = "INVOKE", target = "Lxaero/common/minimap/waypoints/WaypointWorldRootContainer;isUsingDefaultTeleportCommand()Z"))
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
                if (MinecraftClient.getInstance().world.getRegistryKey() != Shared.customDimensionId) {
                    double customDimDiv = Shared.customDimensionId == NETHER ? 8.0 : 1.0;
                    return customDimDiv / selectedDimDiv;
                }
                return currentDimDiv / selectedDimDiv;
            }
        } else {
            return 1.0;
        }
    }
}
