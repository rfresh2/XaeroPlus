package xaeroplus.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.culling.ClippingHelperImpl;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.tileentity.TileEntityBeaconRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.DimensionType;
import org.lwjgl.opengl.GL11;
import xaero.common.HudMod;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.common.minimap.waypoints.WaypointVisibilityType;
import xaero.hud.minimap.BuiltInHudModules;
import xaero.hud.minimap.common.config.option.MinimapProfiledConfigOptions;
import xaero.hud.minimap.module.MinimapSession;
import xaero.hud.minimap.world.MinimapWorld;
import xaero.lib.client.config.ClientConfigManager;
import xaero.lib.common.config.channel.ConfigChannel;
import xaeroplus.settings.Settings;

import java.util.ArrayList;
import java.util.List;

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

    private static final ResourceLocation BEACON_BEAM_TEXTURE = new ResourceLocation("textures/entity/beacon_beam.png");

    public void renderWaypointBeacons(float tickDelta) {
        MinimapSession session = BuiltInHudModules.MINIMAP.getCurrentSession();
        if (session == null) return;
        ConfigChannel hudConfigs = HudMod.INSTANCE.getHudConfigs();
        if (hudConfigs == null) return;
        ClientConfigManager clientConfigManager = hudConfigs.getClientConfigManager();
        if (!clientConfigManager.getEffective(MinimapProfiledConfigOptions.WAYPOINTS_IN_WORLD)) return;
        MinimapWorld currentWorld = session.getWorldManager().getCurrentWorld();
        if (currentWorld == null) return;
        if (System.currentTimeMillis() - lastWaypointRenderListUpdate > 50L) {
            updateWaypointRenderList(session, clientConfigManager);
            lastWaypointRenderListUpdate = System.currentTimeMillis();
        }
        double dimDiv = session.getDimensionHelper().getDimensionDivision(currentWorld);
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null || mc.getRenderViewEntity() == null) return;
        Vec3d cameraPos = mc.getRenderViewEntity().getPositionVector();
        double distanceScale = clientConfigManager.getEffective(MinimapProfiledConfigOptions.WAYPOINT_MAX_DISTANCE_DIMENSION_SCALE)
            && Minecraft.getMinecraft().world.provider.getDimensionType() == DimensionType.NETHER
            ? 8.0
            : 1.0;
        double waypointsDistance = clientConfigManager.getEffective(MinimapProfiledConfigOptions.WAYPOINT_MAX_DISTANCE);
        double waypointsDistanceMin = clientConfigManager.getEffective(MinimapProfiledConfigOptions.WAYPOINT_MIN_DISTANCE_IN_WORLD);
        GlStateManager.disableLighting(); // baritone goal rendering fix
        for (int i = 0; i < waypointList.size(); i++) {
            final Waypoint w = waypointList.get(i);
            double offX = (double) w.getX(dimDiv) - cameraPos.x + 0.5;
            double offZ = (double) w.getZ(dimDiv) - cameraPos.z + 0.5;
            double unscaledDistance2D = Math.sqrt(offX * offX + offZ * offZ);
            double distance2D = unscaledDistance2D * distanceScale;
            boolean shouldRender = w.isDestination()
                || (w.getPurpose().isDeath()
                || w.isGlobal()
                || w.isTemporary() && clientConfigManager.getEffective(MinimapProfiledConfigOptions.TEMPORARY_WAYPOINTS_GLOBAL)
                || waypointsDistance == 0.0
                || !(distance2D > waypointsDistance)
            ) && (waypointsDistanceMin == 0.0 || !(unscaledDistance2D < waypointsDistanceMin));
            if (shouldRender)
                renderWaypointBeacon(w, dimDiv, tickDelta);
        }
    }


    public void renderWaypointBeacon(final Waypoint waypoint, final double dimDiv, float partialTicks) {
        final Minecraft mc = Minecraft.getMinecraft();
        final RenderManager renderManager = mc.getRenderManager();
        Entity renderViewEntity = renderManager.renderViewEntity;
        if (renderViewEntity == null) return;
        final Vec3d playerVec = renderViewEntity.getPositionVector();
        Vec3d waypointVec = new Vec3d(waypoint.getX(dimDiv), playerVec.y, waypoint.getZ(dimDiv));
        final double xzDistance = playerVec.distanceTo(waypointVec);
        if (xzDistance < (int) Settings.REGISTRY.waypointBeaconDistanceMin.getValue()) return;
        final int farScale = (int) Settings.REGISTRY.waypointBeaconScaleMin.getValue();
        double maxRenderDistance = Math.min(mc.gameSettings.renderDistanceChunks << 4, farScale == 0 ? Integer.MAX_VALUE : farScale << 4);
        if (xzDistance > maxRenderDistance) {
            final Vec3d delta = waypointVec.subtract(playerVec).normalize();
            waypointVec = playerVec.add(new Vec3d(delta.x * maxRenderDistance, delta.y * maxRenderDistance, delta.z * maxRenderDistance));
        }
        final double x = waypointVec.x - renderManager.viewerPosX;
        final double z = waypointVec.z - renderManager.viewerPosZ;
        final double y = -renderManager.viewerPosY;
        final int color = waypoint.getWaypointColor().getHex();
        if (!ClippingHelperImpl.getInstance().isBoxInFrustum(x, y, z, x, y+256, z)) return;
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1f);
        mc.renderEngine.bindTexture(BEACON_BEAM_TEXTURE);
        final float time = mc.world.getTotalWorldTime();
        final float[] colorRGBA = ColorHelper.getColorRGBA(color);
        TileEntityBeaconRenderer.renderBeamSegment(x, y, z, partialTicks, 1.0f, time, 0, 256, colorRGBA);
    }
}
