package xaeroplus.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import it.unimi.dsi.fastutil.doubles.DoubleArrayFIFOQueue;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.BuiltInHudModules;
import xaero.hud.minimap.module.MinimapSession;
import xaero.hud.minimap.waypoint.render.world.WaypointWorldRenderer;
import xaeroplus.settings.Settings;
import xaeroplus.util.ChunkUtils;

import static net.minecraft.world.level.Level.NETHER;
import static net.minecraft.world.level.Level.OVERWORLD;

@Mixin(value = WaypointWorldRenderer.class, remap = false)
public class MixinWaypointWorldRenderer {

    @Shadow private String subWorldName;

    @ModifyArg(method = "renderElement(Lxaero/common/minimap/waypoints/Waypoint;ZZDFDDLxaero/hud/minimap/element/render/MinimapElementRenderInfo;Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;)Z", at = @At(
        value = "INVOKE",
        target = "Lxaero/hud/minimap/waypoint/render/world/WaypointWorldRenderer;renderIconWithLabels(Lxaero/common/minimap/waypoints/Waypoint;ZLjava/lang/String;Ljava/lang/String;Ljava/lang/String;FIILnet/minecraft/client/gui/Font;ILcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;)V"),
        index = 4,
    remap = true) // $REMAP
    public String preferOwWaypointsRemoveSubworldText(final String name) {
        if (!Settings.REGISTRY.owAutoWaypointDimension.get()) return name;
        if (this.subWorldName == null) return name;
        ResourceKey<Level> actualDimension = ChunkUtils.getActualDimension();
        ResourceKey<Level> currentWpWorldDim = BuiltInHudModules.MINIMAP.getCurrentSession().getWorldManager().getCurrentWorld().getDimId();
        if (actualDimension == NETHER && currentWpWorldDim == OVERWORLD) {
            return null;
        }
        return name;
    }

    /**
     * todo: separate out rendering so it is independent of when distance text is rendered
     *  and put it on its own line
     */
    @ModifyArg(method = "renderElement(Lxaero/common/minimap/waypoints/Waypoint;ZZDFDDLxaero/hud/minimap/element/render/MinimapElementRenderInfo;Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;)Z", at = @At(
        value = "INVOKE",
        target = "Lxaero/hud/minimap/waypoint/render/world/WaypointWorldRenderer;renderIconWithLabels(Lxaero/common/minimap/waypoints/Waypoint;ZLjava/lang/String;Ljava/lang/String;Ljava/lang/String;FIILnet/minecraft/client/gui/Font;ILcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;)V"),
        index = 3,
        remap = true) // $REMAP
    public String modifyDistanceText(final String text, @Local(argsOnly = true) Waypoint waypoint) {
        if (!Settings.REGISTRY.waypointEta.get()) return text;
        if (text == null || text.isBlank()) return text;
        var eta = getEtaSecondsToReachWaypoint(waypoint);
        if (eta <= 0) return text;
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
        return text + etaText;
    }

    // average out and smoothen speed updates so they aren't tied directly to fps
    @Unique
    long xaeroPlus$lastSpeedUpdate = 0;
    @Unique public final DoubleArrayFIFOQueue xaeroPlus$speedQueue = new DoubleArrayFIFOQueue(15);

    @Unique
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
            double speed = xaeroPlus$speedQueue.isEmpty() ? 0.0 : xaeroPlus$avgSpeed(xaeroPlus$speedQueue);
            double cos = dot / (distance * speed);
            double time = distance / speed;
            double etaTicks = time / cos;
            double etaSeconds = etaTicks / 20.0;

            // update avg speed measurements
            var updateDeltaMs = System.currentTimeMillis() - xaeroPlus$lastSpeedUpdate;
            if (updateDeltaMs > 50) {
                xaeroPlus$lastSpeedUpdate = System.currentTimeMillis();
                double s = Math.sqrt(movementX * movementX + movementZ * movementZ);
                if (s > 0 || mc.player.tickCount % 4 == 0) {
                    xaeroPlus$speedQueue.enqueue(s);
                } else if (!xaeroPlus$speedQueue.isEmpty()) {
                    xaeroPlus$speedQueue.dequeueDouble();
                }
                while (xaeroPlus$speedQueue.size() > 10) xaeroPlus$speedQueue.dequeueDouble();
            }
            if (etaSeconds == Double.POSITIVE_INFINITY || etaSeconds == Double.NEGATIVE_INFINITY || Double.isNaN(etaSeconds)) return 0;
            return (long) etaSeconds;
        } catch (final Exception e) {
            // fall through
        }
        return 0;
    }

    @Unique
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
}
