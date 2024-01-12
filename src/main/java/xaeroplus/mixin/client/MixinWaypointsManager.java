package xaeroplus.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
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
import xaero.common.minimap.waypoints.WaypointsManager;
import xaeroplus.Globals;
import xaeroplus.feature.extensions.IWaypointDimension;
import xaeroplus.settings.XaeroPlusSettingRegistry;
import xaeroplus.util.DataFolderResolveUtil;

import static net.minecraft.world.level.Level.NETHER;
import static net.minecraft.world.level.Level.OVERWORLD;

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
    public abstract String getDimensionDirectoryName(ResourceKey<Level> dimKey);

    @Inject(method = "getMainContainer", at = @At("HEAD"), cancellable = true)
    private void getMainContainer(boolean preIp6Fix, ClientPacketListener connection, CallbackInfoReturnable<String> cir) {
        DataFolderResolveUtil.resolveDataFolder(connection, cir);
    }

    Waypoint selected = null;
    WaypointWorld displayedWorld = null;
    @Inject(
        method = "teleportToWaypoint(Lxaero/common/minimap/waypoints/Waypoint;Lxaero/common/minimap/waypoints/WaypointWorld;Lnet/minecraft/client/gui/screens/Screen;Z)V",
        at = @At("HEAD"),
        remap = false)
    public void teleportToWaypointHead(final Waypoint selected, final WaypointWorld displayedWorld, final Screen screen, final boolean respectHiddenCoords, final CallbackInfo ci) {
        this.selected = selected;
        this.displayedWorld = displayedWorld;
    }

    @Redirect(
        method = "teleportToWaypoint(Lxaero/common/minimap/waypoints/Waypoint;Lxaero/common/minimap/waypoints/WaypointWorld;Lnet/minecraft/client/gui/screens/Screen;Z)V",
        at = @At(
            value = "INVOKE",
            target = "Lxaero/common/minimap/waypoints/WaypointsManager;getAutoWorld()Lxaero/common/minimap/waypoints/WaypointWorld;"
        ),
        remap = false
    )
    public WaypointWorld getAutoWorldRedirect(final WaypointsManager instance) {
        if (!XaeroPlusSettingRegistry.owAutoWaypointDimension.getValue()) return instance.getAutoWorld();
        ResourceKey<Level> waypointDimension = ((IWaypointDimension) selected).getDimension();
        if (waypointDimension != Globals.getCurrentDimensionId()) {
            return null;
        } else {
            return instance.getAutoWorld();
        }
    }

    @Inject(method = "getPotentialContainerID", at = @At("HEAD"), cancellable = true)
    private void getPotentialContainerID(CallbackInfoReturnable<String> cir) {
        if (!XaeroPlusSettingRegistry.owAutoWaypointDimension.getValue()) return;
        ResourceKey<Level> dimension = mc.level.dimension();
        if (dimension == OVERWORLD || dimension == NETHER) {
            dimension = OVERWORLD;
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
                                         final double dimScale,
                                         final CallbackInfo ci,
                                         double waypointDestDimScale,
                                         final double dimDiv,
                                         final Waypoint waypoint) {
        ((IWaypointDimension) waypoint).setDimension(wpw.getDimId());
    }
}
