package xaeroplus.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import xaero.common.gui.GuiWaypoints;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.common.minimap.waypoints.WaypointWorld;
import xaero.common.minimap.waypoints.WaypointsManager;
import xaero.common.minimap.waypoints.WaypointsSort;
import xaero.common.misc.KeySortableByOther;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;

@Mixin(value = GuiWaypoints.class, remap = false)
public class MixinGuiWaypoints {

    @Shadow
    private WaypointWorld displayedWorld;
    @Shadow
    private ArrayList<Waypoint> waypointsSorted;
    @Shadow
    private WaypointsManager waypointsManager;

    /**
     * @author rfresh2
     * @reason Always sort enabled waypoints before disabled waypoints
     */
    @Overwrite
    private void updateSortedList() {
        WaypointsSort sortType = this.displayedWorld.getContainer().getRootContainer().getSortType();
        ArrayList<Waypoint> waypointsList = this.displayedWorld.getCurrentSet().getList();
        GuiWaypoints.distanceDivided = this.waypointsManager.getDimensionDivision(this.displayedWorld.getContainer().getKey());
        Entity renderViewEntity = Minecraft.getMinecraft().getRenderViewEntity();
        Vec3d cameraPos = isNull(renderViewEntity)
                ? ActiveRenderInfo.getCameraPosition()
                : ActiveRenderInfo.getCameraPosition().addVector(renderViewEntity.posX, renderViewEntity.posY, renderViewEntity.posZ);
        Vec3d lookVector = isNull(renderViewEntity) ? new Vec3d(1.0, 0.0, 0.0) : renderViewEntity.getLookVec();
        if (Minecraft.getMinecraft().gameSettings.thirdPersonView == 2) {
            lookVector = lookVector.scale(-1.0);
        }

        final List<Waypoint> disabledWaypoints = waypointsList.stream()
                .filter(Waypoint::isDisabled)
                .collect(Collectors.toList());
        final List<Waypoint> enabledWaypoints = waypointsList.stream()
                .filter(waypoint -> !waypoint.isDisabled())
                .collect(Collectors.toList());
        this.waypointsSorted = new ArrayList<>();

        this.waypointsSorted.addAll(sortWaypoints(enabledWaypoints, sortType, cameraPos, lookVector));
        this.waypointsSorted.addAll(sortWaypoints(disabledWaypoints, sortType, cameraPos, lookVector));
    }

    private List<Waypoint> sortWaypoints(final List<Waypoint> waypointsList, WaypointsSort sortType, final Vec3d cameraPos, final Vec3d lookVector) {
        final ArrayList<Waypoint> waypointsSorted = new ArrayList<>();
        final ArrayList<KeySortableByOther<Waypoint>> sortableKeys = new ArrayList<>();
        for(Waypoint w : waypointsList) {
            Comparable sortVal = 0;
            switch (sortType) {
                case NONE:
                    break;
                case ANGLE:
                     sortVal = -w.getComparisonAngleCos(cameraPos, lookVector, GuiWaypoints.distanceDivided);
                     break;
                case NAME:
                    sortVal = w.getComparisonName();
                    break;
                case SYMBOL:
                    sortVal = w.getSymbol();
                    break;
                case DISTANCE:
                    sortVal = w.getComparisonDistance(cameraPos, GuiWaypoints.distanceDivided);
                    break;
            }
            sortableKeys.add(
                    new KeySortableByOther<>(
                            w,
                            sortVal));
        }
        Collections.sort(sortableKeys);
        for(KeySortableByOther<Waypoint> k : sortableKeys) {
            waypointsSorted.add(k.getKey());
        }
        if (this.displayedWorld.getContainer().getRootContainer().isSortReversed()) {
            Collections.reverse(waypointsSorted);
        }
        return waypointsSorted;
    }
}
