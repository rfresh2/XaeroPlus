package xaeroplus.util;

import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.waystones.api.IWaystone;
import net.blay09.mods.waystones.api.KnownWaystonesEvent;
import net.blay09.mods.waystones.core.WaystoneTypes;
import net.minecraft.world.item.DyeColor;
import xaero.hud.minimap.waypoint.WaypointColor;
import xaeroplus.module.impl.WaystoneSync;

import java.util.ArrayList;
import java.util.List;

public class BlayWaystonesHelper {
    public List<IWaystone> toSyncWaystones = new ArrayList<>();
    // cache of currently synced standard waystones
    public List<IWaystone> currentWaystones = new ArrayList<>();
    public boolean shouldSync = false;
    private boolean subscribed = false;

    public void subscribeWaystonesEvent() {
        if (subscribed) return;
        Balm.getEvents().onEvent(KnownWaystonesEvent.class, this::onKnownWaystonesEvent);
        subscribed = true;
    }

    public void onKnownWaystonesEvent(final KnownWaystonesEvent event) {
        toSyncWaystones = event.getWaystones();
        currentWaystones = event.getWaystones();
        shouldSync = true;
    }

    public List<WaystoneSync.Waystone> getToSyncWaystones() {
        return toSyncWaystones.stream()
            .map(waystone -> {
                WaypointColor color = null;
                // note: sharestones are not synced until MC 1.20.2
                // so this will not do anything on this mc version
                if (WaystoneTypes.isSharestone(waystone.getWaystoneType())) {
                    var keyPath = waystone.getWaystoneType().getPath();
                    int suffixIndex = keyPath.lastIndexOf("_sharestone");
                    if (suffixIndex != -1) {
                        String colorName = keyPath.substring(0, suffixIndex);
                        DyeColor dyeColor = DyeColor.byName(colorName, null);
                        if (dyeColor != null) {
                            color = switch (dyeColor) {
                                case WHITE -> WaypointColor.WHITE;
                                case ORANGE -> WaypointColor.GOLD;
                                case MAGENTA -> WaypointColor.DARK_PURPLE;
                                case LIGHT_BLUE -> WaypointColor.AQUA;
                                case YELLOW -> WaypointColor.YELLOW;
                                case LIME -> WaypointColor.GREEN;
                                case GREEN -> WaypointColor.DARK_GREEN;
                                case PINK-> WaypointColor.PURPLE;
                                case PURPLE -> WaypointColor.BLUE;
                                case GRAY ->  WaypointColor.DARK_GRAY;
                                case LIGHT_GRAY -> WaypointColor.GRAY;
                                case CYAN -> WaypointColor.DARK_AQUA;
                                case BLUE -> WaypointColor.DARK_BLUE;
                                case BROWN -> WaypointColor.DARK_RED;
                                case RED -> WaypointColor.RED;
                                case BLACK -> WaypointColor.BLACK;
                            };
                        }
                    }
                }
                return new WaystoneSync.Waystone(
                    waystone.getName(),
                    waystone.getDimension(),
                    waystone.getPos().getX(),
                    waystone.getPos().getY() + 1,// avoid teleporting directly into the waystone
                    waystone.getPos().getZ(),
                    color
                );
            }).toList();
    }
}
