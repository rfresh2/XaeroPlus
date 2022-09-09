package xaeroplus.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.IXaeroMinimap;
import xaero.common.gui.GuiWaypoints;
import xaero.common.gui.MySmallButton;
import xaero.common.gui.ScreenBase;
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
public class MixinGuiWaypoints extends ScreenBase {

    private final int TOGGLE_ALL_ID = 69;
    @Shadow
    private WaypointWorld displayedWorld;
    @Shadow
    private ArrayList<Waypoint> waypointsSorted;
    @Shadow
    private WaypointsManager waypointsManager;

    protected MixinGuiWaypoints(IXaeroMinimap modMain, GuiScreen parent, GuiScreen escape) {
        super(modMain, parent, escape);
    }

    @Inject(method = "initGui()V", at = @At("TAIL"), remap = true)
    public void initGui(CallbackInfo ci) {
        // todo: this button is a bit larger than i want but cba to figure out exact size rn
        this.buttonList.add(new MySmallButton(TOGGLE_ALL_ID, this.width / 2 + 213, this.height - 53, "Toggle Enable All"));
    }

    @Inject(method = "actionPerformed", at = @At("TAIL"), remap = true)
    public void actionPerformed(GuiButton b, CallbackInfo ci) {
        if (b.enabled) {
            if (b.id == TOGGLE_ALL_ID) {
                waypointsSorted.stream().findFirst().ifPresent(firstWaypoint -> {
                    boolean firstIsEnabled = firstWaypoint.isDisabled();
                    waypointsSorted.forEach(waypoint -> waypoint.setDisabled(!firstIsEnabled));
                });
            }
        }
    }

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
