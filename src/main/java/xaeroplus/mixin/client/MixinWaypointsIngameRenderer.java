package xaeroplus.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.tileentity.TileEntityBeaconRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.XaeroMinimapSession;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.common.minimap.waypoints.WaypointsManager;
import xaero.common.minimap.waypoints.render.WaypointFilterParams;
import xaero.common.minimap.waypoints.render.WaypointsIngameRenderer;
import xaero.common.settings.ModSettings;
import xaeroplus.settings.XaeroPlusSettingRegistry;
import xaeroplus.util.ColorHelper;
import xaeroplus.util.CustomWaypointsIngameRenderer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static xaeroplus.util.Shared.customDimensionId;

@Mixin(value = WaypointsIngameRenderer.class, remap = false)
public class MixinWaypointsIngameRenderer implements CustomWaypointsIngameRenderer {
    @Shadow private List<Waypoint> sortingList;
    @Shadow private WaypointFilterParams filterParams;
    List<Waypoint> beaconWaypoints = new ArrayList<>();
    private static final ResourceLocation BEACON_BEAM_TEXTURE = new ResourceLocation("textures/entity/beacon_beam.png");
    final Predicate<Waypoint> beaconViewFilter = new Predicate<Waypoint>() {
        @Override
        public boolean test(final Waypoint w) {
            boolean deathpoints = filterParams.deathpoints;
            if (!w.isDisabled()
                    && w.getVisibilityType() != 2
                    && w.getVisibilityType() != 3
                    && (w.getWaypointType() != 1 && w.getWaypointType() != 2 || deathpoints)) {
                double wpRenderX = (double)w.getX(filterParams.dimDiv) + 0.5 - filterParams.actualEntityX;
                double wpRenderZ = (double)w.getZ(filterParams.dimDiv) + 0.5 - filterParams.actualEntityZ;
                double offX = wpRenderX - filterParams.cameraX;
                double offZ = wpRenderZ - filterParams.cameraZ;
                double distance2D = Math.sqrt(offX * offX + offZ * offZ);
                double waypointsDistance = filterParams.waypointsDistance;
                double waypointsDistanceMin = filterParams.waypointsDistanceMin;
                return w.isOneoffDestination()
                        || (
                        w.getWaypointType() == 1
                                || w.isGlobal()
                                || w.isTemporary() && filterParams.temporaryWaypointsGlobal
                                || waypointsDistance == 0.0
                                || !(distance2D > waypointsDistance))
                        && (waypointsDistanceMin == 0.0 || !(distance2D < waypointsDistanceMin));
            } else {
                return false;
            }
        }
    };

    @Inject(method = "renderWaypointsIterator", at = @At("HEAD"))
    public void injectRenderWaypoints(final float[] worldModelView, final Iterator<Waypoint> iter, final double d3, final double d4, final double d5, final Entity entity, final BufferBuilder bufferbuilder, final Tessellator tessellator, final double dimDiv, final double actualEntityX, final double actualEntityY, final double actualEntityZ, final double smoothEntityY, final double fov, final int screenHeight, final float cameraAngleYaw, final float cameraAnglePitch, final Vec3d lookVector, final double clampDepth, final FontRenderer fontrenderer, final float[] waypointsProjection, final int screenWidth, final boolean detailedDisplayAllowed, final double uiScale, final double minDistance, final String subworldName, final CallbackInfo ci) {
        beaconWaypoints = sortingList.stream().filter(beaconViewFilter).sorted().collect(Collectors.toList());
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lxaero/common/minimap/waypoints/WaypointsManager;getDimensionDivision(Ljava/lang/String;)D"))
    public double redirectDimensionDivision(final WaypointsManager waypointsManager, final String worldContainerID) {
        if (worldContainerID != null && Minecraft.getMinecraft().world != null) {
            try {
                int dim = Minecraft.getMinecraft().world.provider.getDimension();
                if (!Objects.equals(dim, customDimensionId)) {
                    double currentDimDiv = Objects.equals(dim, -1) ? 8.0 : 1.0;
                    String dimPart = worldContainerID.substring(worldContainerID.lastIndexOf(47) + 1);
                    Integer dimKey = waypointsManager.getDimensionForDirectoryName(dimPart);
                    double selectedDimDiv = dimKey == -1 ? 8.0 : 1.0;
                    return currentDimDiv / selectedDimDiv;
                }
            } catch (final Exception e) {
                // fall through
            }
        }
        return waypointsManager.getDimensionDivision(worldContainerID);
    }

    @Override
    public void renderWaypointBeacons(final XaeroMinimapSession minimapSession, final RenderGlobal renderGlobal, final float partialTicks) {
        if (!XaeroPlusSettingRegistry.waypointBeacons.getValue()) return;
        final WaypointsManager waypointsManager = minimapSession.getWaypointsManager();
        double dimDiv = redirectDimensionDivision(waypointsManager, waypointsManager.getCurrentContainerID());
        beaconWaypoints.forEach(w -> renderWaypointBeacon(w, dimDiv, partialTicks));
    }

    public void renderWaypointBeacon(final Waypoint waypoint, final double dimDiv, float partialTicks) {
        final Minecraft mc = Minecraft.getMinecraft();
        final RenderManager renderManager = mc.getRenderManager();
        Entity renderViewEntity = renderManager.renderViewEntity;
        if (renderViewEntity == null) return;
        final Vec3d playerVec = renderViewEntity.getPositionVector();
        Vec3d waypointVec = new Vec3d(waypoint.getX(dimDiv), playerVec.y, waypoint.getZ(dimDiv));
        final double xzDistance = playerVec.distanceTo(waypointVec);
        if (xzDistance < (int) XaeroPlusSettingRegistry.waypointBeaconDistanceMin.getValue()) return;
        final int farScale = (int) XaeroPlusSettingRegistry.waypointBeaconScaleMin.getValue();
        double maxRenderDistance = Math.min(mc.gameSettings.renderDistanceChunks << 4, farScale == 0 ? Integer.MAX_VALUE : farScale << 4);
        if (xzDistance > maxRenderDistance) {
            final Vec3d delta = waypointVec.subtract(playerVec).normalize();
            waypointVec = playerVec.add(new Vec3d(delta.x * maxRenderDistance, delta.y * maxRenderDistance, delta.z * maxRenderDistance));
        }
        final double x = waypointVec.x - renderManager.viewerPosX;
        final double z = waypointVec.z - renderManager.viewerPosZ;
        final double y = -renderManager.viewerPosY;
        final int color = ModSettings.COLORS[waypoint.getColor()];
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1f);
        mc.renderEngine.bindTexture(BEACON_BEAM_TEXTURE);
        final float time = mc.world.getTotalWorldTime();
        final float[] colorRGBA = ColorHelper.getColorRGBA(color);
        TileEntityBeaconRenderer.renderBeamSegment(x, y, z, partialTicks, 1.0f, time, 0, 256, colorRGBA);
    }
}
