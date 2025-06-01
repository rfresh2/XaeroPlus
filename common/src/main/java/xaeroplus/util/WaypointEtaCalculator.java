package xaeroplus.util;

import it.unimi.dsi.fastutil.doubles.DoubleArrayFIFOQueue;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.BuiltInHudModules;
import xaero.hud.minimap.module.MinimapSession;

public class WaypointEtaCalculator {
    public static final WaypointEtaCalculator INSTANCE = new WaypointEtaCalculator();

    // average out and smoothen speed updates so they aren't tied directly to fps
    private long lastSpeedUpdate = 0;
    private DoubleArrayFIFOQueue speedQueue = new DoubleArrayFIFOQueue(15);
    private int bufferLen = 15;

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

    public long getEtaSecondsToReachWaypoint(Waypoint waypoint) {
        final Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return 0;
        try {
            final Vec3 playerVec = mc.player.position();
            MinimapSession minimapSession = BuiltInHudModules.MINIMAP.getCurrentSession();
            if (minimapSession == null) return 0;
            double dimDiv = minimapSession.getDimensionHelper().getDimensionDivision(minimapSession.getWorldManager().getCurrentWorld());
            int wpX = waypoint.getX(dimDiv);
            int wpZ = waypoint.getZ(dimDiv);
            double directionX = wpX - playerVec.x;
            double directionZ = wpZ - playerVec.z;
            double movementX = playerVec.x - mc.player.xOld;
            double movementZ = playerVec.z - mc.player.zOld;
            double dot = directionX * movementX + directionZ * movementZ;
            double distance = Math.sqrt(directionX * directionX + directionZ * directionZ);
            double speed = speedQueue.isEmpty() ? 0.0 : xaeroPlus$avgSpeed(speedQueue);
            double cos = dot / (distance * speed);
            double time = distance / speed;
            double etaTicks = time / cos;
            double etaSeconds = etaTicks / 20.0;

            // update avg speed measurements
            var updateDeltaMs = System.currentTimeMillis() - lastSpeedUpdate;
            if (updateDeltaMs >= 50) {
                lastSpeedUpdate = System.currentTimeMillis();
                double s = Math.sqrt(movementX * movementX + movementZ * movementZ);
                if (s > 0 || mc.player.tickCount % 4 == 0) {
                    speedQueue.enqueue(s);
                } else if (!speedQueue.isEmpty()) {
                    speedQueue.dequeueDouble();
                }
                while (speedQueue.size() > bufferLen) {
                    speedQueue.dequeueDouble();
                }
            }
            if (etaSeconds == Double.POSITIVE_INFINITY || etaSeconds == Double.NEGATIVE_INFINITY || Double.isNaN(etaSeconds)) return 0;
            return (long) etaSeconds;
        } catch (final Exception e) {
            // fall through
        }
        return 0;
    }

    private double xaeroPlus$avgSpeed(final DoubleArrayFIFOQueue speedQueue) {
        double sum = 0;
        for (int i = 0; i < speedQueue.size(); i++) {
            var v = speedQueue.dequeueDouble();
            speedQueue.enqueue(v);
            sum += v;
        }
        var s = sum / speedQueue.size();
        if (s < 0.05) return 0.0; // floor very low speeds
        return s;
    }

    public void updateBufferLen(final double d) {
        bufferLen = Mth.clamp((int) d, 1, 100);
        speedQueue = new DoubleArrayFIFOQueue(bufferLen + 5);
    }
}
