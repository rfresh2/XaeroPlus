package xaeroplus.mixin.client;

import net.minecraft.client.gui.widget.ButtonWidget;
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
import xaeroplus.feature.extensions.IWaypointDimension;
import xaeroplus.util.WaypointsHelper;

import java.util.ArrayList;
import java.util.Arrays;

@Mixin(value = GuiAddWaypoint.class, remap = true)
public class MixinGuiAddWaypoint {

    @Shadow(remap = false) private ArrayList<Waypoint> waypointsEdited;


    @Inject(
        method = "lambda$init$3",
        at = @At(
            value = "INVOKE",
            target = "Lxaero/common/AXaeroMinimap;getSettings()Lxaero/common/settings/ModSettings;",
            ordinal = 1
        ),
        locals = LocalCapture.CAPTURE_FAILHARD)
    public void addWaypointInject(
        ButtonWidget b,
        final CallbackInfo ci,
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
                ((IWaypointDimension) waypoint).setDimension(WaypointsHelper.getDimensionKeyForWaypointWorldKey(destinationWorld.getContainer().getKey()));
            });
        } catch (Throwable e) {
            XaeroPlus.LOGGER.error("Failed setting waypoint dimension: {}", Arrays.toString(waypointsEdited.toArray()), e);
        }
    }
}
