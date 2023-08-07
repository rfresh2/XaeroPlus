package xaeroplus.mixin.client;

import net.minecraft.client.gui.GuiButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import xaero.common.gui.GuiAddWaypoint;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.common.minimap.waypoints.WaypointSet;
import xaero.common.minimap.waypoints.WaypointWorld;
import xaeroplus.XaeroPlus;
import xaeroplus.util.IWaypointDimension;
import xaeroplus.util.WaypointsHelper;

import java.util.ArrayList;
import java.util.Arrays;

@Mixin(value = GuiAddWaypoint.class, remap = true)
public class MixinGuiAddWaypoint {

    @Shadow private ArrayList<Waypoint> waypointsEdited;


    @Inject(
        method = "actionPerformed",
        at = @At(
            value = "INVOKE",
            target = "Lxaero/common/IXaeroMinimap;getSettings()Lxaero/common/settings/ModSettings;",
            ordinal = 3
        ),
        locals = LocalCapture.CAPTURE_FAILHARD)
    public void addWaypointInject(final GuiButton par1GuiButton,
                                  final CallbackInfo ci,
                                  int var2,
                                  boolean creatingAWaypoint,
                                  double dimDiv,
                                  int initialEditedWaypointsSize,
                                  WaypointWorld sourceWorld,
                                  WaypointSet sourceSet,
                                  String[] destinationWorldKeys,
                                  String destinationSetKey,
                                  WaypointWorld destinationWorld,
                                  WaypointSet destinationSet) {
        try {
            waypointsEdited.forEach(waypoint -> {
                ((IWaypointDimension) waypoint).setDimension(WaypointsHelper.getDimensionForWaypointWorldKey(destinationWorld.getContainer().getKey()));
            });
        } catch (Throwable e) {
            XaeroPlus.LOGGER.error("Failed setting waypoint dimension: {}", Arrays.toString(waypointsEdited.toArray()), e);
        }
    }
}
