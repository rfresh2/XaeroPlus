package xaeroplus.feature.waypoint.eta;

import kaptainwutax.mathutils.util.Mth;
import net.lenni0451.lambdaevents.EventHandler;
import net.minecraft.client.Minecraft;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.BuiltInHudModules;
import xaero.hud.minimap.module.MinimapSession;
import xaeroplus.XaeroPlus;
import xaeroplus.event.ClientTickEvent;
import xaeroplus.util.timer.Timer;
import xaeroplus.util.timer.Timers;

public class WaypointEtaManager {
    public static final WaypointEtaManager INSTANCE = new WaypointEtaManager();

    private final Timer measurementTimer = Timers.tickTimer();
    private int measurementInterval = 20; // ticks
    private double lastX = 0;
    private double lastZ = 0;
    private double speedBlocksPerSecond = 0;

    private WaypointEtaManager() {
        XaeroPlus.EVENT_BUS.register(this);
    }

    @EventHandler
    public void onClickTick(ClientTickEvent.Post event) {
        if (!measurementTimer.tick(measurementInterval)) return;
        final Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        double currentX = mc.player.getX();
        double currentZ = mc.player.getZ();
        double dist = Math.hypot(currentX - lastX, currentZ - lastZ);
        lastX = currentX;
        lastZ = currentZ;
        double measurementMs = (measurementInterval + 1) * 50.0;
        double measurementSeconds = measurementMs / 1000.0;
        if (measurementSeconds <= 0) return;
        speedBlocksPerSecond = dist / measurementSeconds;
    }

    public long getEtaSecondsToReachWaypoint(Waypoint waypoint) {
        final Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return 0;
        MinimapSession minimapSession = BuiltInHudModules.MINIMAP.getCurrentSession();
        if (minimapSession == null) return 0;
        double dimDiv = minimapSession.getDimensionHelper().getDimensionDivision(minimapSession.getWorldManager().getCurrentWorld());
        int wpX = waypoint.getX(dimDiv);
        int wpZ = waypoint.getZ(dimDiv);
        double wpDist = Math.hypot(wpX - mc.player.getX(), wpZ - mc.player.getZ());
        double etaSeconds = wpDist / speedBlocksPerSecond;
        if (etaSeconds == Double.POSITIVE_INFINITY || etaSeconds == Double.NEGATIVE_INFINITY || Double.isNaN(etaSeconds)) return 0;
        return (long) etaSeconds;
    }

    public String getEtaTextSuffix(Waypoint waypoint) {
        var eta = getEtaSecondsToReachWaypoint(waypoint);
        if (eta <= 0) return "";
        String etaText = " - ";
        if (eta > 86400) {
            int days = (int) (eta / 86400);
            int hours = (int) ((eta % 86400) / 3600);
            etaText += days + "d";
            if (hours > 0) etaText += " " + hours + "h";
        } else if (eta > 3600) {
            int hours = (int) (eta / 3600);
            int minutes = (int) ((eta % 3600) / 60);
            etaText += hours + "h";
            if (minutes > 0) etaText += " " + minutes + "m";
        } else if (eta > 60) {
            int minutes = (int) (eta / 60);
            int seconds = (int) (eta % 60);
            etaText += minutes + "m";
            if (seconds > 0) etaText += " " + seconds + "s";
        } else {
            etaText += eta + "s";
        }
        return etaText;
    }

    public void updateMeasurementInterval(final int interval) {
        measurementInterval = Mth.max(0, interval);
        measurementTimer.reset();
        speedBlocksPerSecond = 0;
    }
}
