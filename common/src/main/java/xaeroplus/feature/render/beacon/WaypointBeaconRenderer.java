package xaeroplus.feature.render.beacon;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import xaero.common.HudMod;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.common.minimap.waypoints.WaypointVisibilityType;
import xaero.hud.minimap.BuiltInHudModules;
import xaero.hud.minimap.common.config.option.MinimapProfiledConfigOptions;
import xaero.hud.minimap.module.MinimapSession;
import xaero.hud.minimap.waypoint.WaypointPurpose;
import xaero.lib.client.config.ClientConfigManager;
import xaeroplus.settings.Settings;
import xaeroplus.util.ColorHelper;

import java.util.ArrayList;
import java.util.List;

import static net.minecraft.client.renderer.blockentity.BeaconRenderer.BEAM_LOCATION;

public class WaypointBeaconRenderer {
    public static final WaypointBeaconRenderer INSTANCE = new WaypointBeaconRenderer();
    private final List<Waypoint> waypointList = new ArrayList<>();
    private long lastWaypointRenderListUpdate = -1L;

    public void updateWaypointRenderList(final MinimapSession session, final ClientConfigManager settings) {
        waypointList.clear();
        session.getWaypointSession().getCollector().collect(waypointList);
        waypointList.removeIf(w -> {
            if (w.isDisabled()
                || w.getVisibility() == WaypointVisibilityType.WORLD_MAP_LOCAL
                || w.getVisibility() == WaypointVisibilityType.WORLD_MAP_GLOBAL) {
                return true;
            }
            return !settings.getEffective(MinimapProfiledConfigOptions.DEATHPOINTS) && w.getPurpose().isDeath();
        });
        waypointList.sort(Waypoint::compareTo);
    }

    public void renderWaypointBeacons(float tickDelta, PoseStack matrixStack) {
        var session = BuiltInHudModules.MINIMAP.getCurrentSession();
        if (session == null) return;
        var hudConfigs = HudMod.INSTANCE.getHudConfigs();
        if (hudConfigs == null) return;
        var clientConfigManager = hudConfigs.getClientConfigManager();
        if (!clientConfigManager.getEffective(MinimapProfiledConfigOptions.WAYPOINTS_IN_WORLD)) return;
        var currentWorld = session.getWorldManager().getCurrentWorld();
        if (currentWorld == null) return;
        if (System.currentTimeMillis() - lastWaypointRenderListUpdate > 50L) {
            updateWaypointRenderList(session, clientConfigManager);
            lastWaypointRenderListUpdate = System.currentTimeMillis();
        }
        var dimDiv = session.getDimensionHelper().getDimensionDivision(currentWorld);
        var mc = Minecraft.getInstance();
        if (mc.level == null || mc.cameraEntity == null) return;
        if (mc.player.level() != mc.level) return;
        var cameraPos = mc.getCameraEntity().position();
        double distanceScale = clientConfigManager.getEffective(MinimapProfiledConfigOptions.WAYPOINT_MAX_DISTANCE_DIMENSION_SCALE) ? mc.level.dimensionType().coordinateScale() : 1.0;
        double waypointsDistance = clientConfigManager.getEffective(MinimapProfiledConfigOptions.WAYPOINT_MAX_DISTANCE);
        double waypointsDistanceMin = clientConfigManager.getEffective(MinimapProfiledConfigOptions.WAYPOINT_MIN_DISTANCE_IN_WORLD);
        for (int i = 0; i < waypointList.size(); i++) {
            final var w = waypointList.get(i);
            double offX = (double)w.getX(dimDiv) - cameraPos.x + 0.5;
            double offZ = (double)w.getZ(dimDiv) - cameraPos.z + 0.5;
            double unscaledDistance2D = Math.sqrt(offX * offX + offZ * offZ);
            double distance2D = unscaledDistance2D * distanceScale;
            if (Settings.REGISTRY.limitDeathpointsRenderDistance.get()) {
                var purpose = w.getPurpose();
                if (purpose == WaypointPurpose.DEATH && Settings.REGISTRY.limitDeathpointsRenderDistance.get()) {
                    if (waypointsDistance != 0 && distance2D > waypointsDistance) {
                        continue;
                    }
                }
            }
            var shouldRender = w.isDestination()
                || (w.getPurpose().isDeath()
                || w.isGlobal()
                || w.isTemporary() && clientConfigManager.getEffective(MinimapProfiledConfigOptions.TEMPORARY_WAYPOINTS_GLOBAL)
                || waypointsDistance == 0.0
                || !(distance2D > waypointsDistance)
            ) && (waypointsDistanceMin == 0.0 || !(unscaledDistance2D < waypointsDistanceMin));;
            if (shouldRender)
                renderWaypointBeacon(w, dimDiv, tickDelta, matrixStack);
        }
    }

    public void renderWaypointBeacon(final Waypoint waypoint, final double dimDiv, float tickDelta, PoseStack matrixStack) {
        final Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.cameraEntity == null) return;
        final Vec3 playerVec = mc.cameraEntity.position();
        Vec3 waypointVec = new Vec3(waypoint.getX(dimDiv), playerVec.y, waypoint.getZ(dimDiv));
        final double xzDistance = playerVec.distanceTo(waypointVec);
        if (xzDistance < Settings.REGISTRY.waypointBeaconDistanceMin.getAsInt()) return;
        final int farScale = Settings.REGISTRY.waypointBeaconScaleMin.getAsInt();
        final double maxRenderDistance = Math.min(mc.options.renderDistance().get() << 4, farScale == 0 ? Integer.MAX_VALUE : farScale << 4);
        if (xzDistance > maxRenderDistance) {
            final Vec3 delta = waypointVec.subtract(playerVec).normalize();
            waypointVec = playerVec.add(new Vec3(delta.x * maxRenderDistance, delta.y * maxRenderDistance, delta.z * maxRenderDistance));
        }
        final EntityRenderDispatcher entityRenderDispatcher = mc.getEntityRenderDispatcher();
        final Camera camera = entityRenderDispatcher.camera;
        final Frustum frustum = mc.levelRenderer.cullingFrustum;
        if (camera == null || frustum == null) return;
        final double viewX = camera.getPosition().x();
        final double viewZ = camera.getPosition().z();
        final double x = waypointVec.x - viewX;
        final double z = waypointVec.z - viewZ;
        final double y = -100;
        if (!frustum.isVisible(new AABB(waypointVec.x-1, -100, waypointVec.z-1, waypointVec.x+1, 500, waypointVec.z+1))) return;
        final int color = waypoint.getWaypointColor().getHex();
        final MultiBufferSource.BufferSource entityVertexConsumers = mc.renderBuffers().bufferSource();
        final long time = mc.level.getGameTime();
        matrixStack.pushPose();
        matrixStack.translate(x, y, z);
        BeaconRenderer.renderBeaconBeam(
            matrixStack, entityVertexConsumers, BEAM_LOCATION, tickDelta, 1.0f, time, 0, 355,
            ColorHelper.getColorRGBA(color), 0.2f, 0.25f);
        matrixStack.popPose();
    }
}
