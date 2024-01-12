package xaeroplus.mixin.client;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.XaeroMinimapSession;
import xaero.common.minimap.render.MinimapRendererHelper;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.common.minimap.waypoints.WaypointsManager;
import xaero.common.minimap.waypoints.render.WaypointFilterParams;
import xaero.common.minimap.waypoints.render.WaypointsIngameRenderer;
import xaero.common.settings.ModSettings;
import xaeroplus.feature.extensions.CustomWaypointsIngameRenderer;
import xaeroplus.feature.render.ColorHelper;
import xaeroplus.mixin.client.mc.AccessorWorldRenderer;
import xaeroplus.settings.XaeroPlusSettingRegistry;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static net.minecraft.client.renderer.blockentity.BeaconRenderer.BEAM_LOCATION;

@Mixin(value = WaypointsIngameRenderer.class, remap = false)
public class MixinWaypointsIngameRenderer implements CustomWaypointsIngameRenderer {
    @Shadow private List<Waypoint> sortingList;
    @Shadow private WaypointFilterParams filterParams;
    List<Waypoint> beaconWaypoints = new ArrayList<>();
    final Predicate<Waypoint> beaconViewFilter = new Predicate<Waypoint>() {
        @Override
        public boolean test(final Waypoint w) {
            boolean deathpoints = filterParams.deathpoints;
            if (!w.isDisabled()
                    && w.getVisibilityType() != 2
                    && w.getVisibilityType() != 3
                    && (w.getWaypointType() != 1 && w.getWaypointType() != 2 || deathpoints)) {
                double offX = (double)w.getX(filterParams.dimDiv) - filterParams.cameraX + 0.5;
                double offZ = (double)w.getZ(filterParams.dimDiv) - filterParams.cameraZ + 0.5;
                double distanceScale = filterParams.dimensionScaleDistance ? Minecraft.getInstance().level.dimensionType().coordinateScale() : 1.0;
                double unscaledDistance2D = Math.sqrt(offX * offX + offZ * offZ);
                double distance2D = unscaledDistance2D * distanceScale;
                double waypointsDistance = filterParams.waypointsDistance;
                double waypointsDistanceMin = filterParams.waypointsDistanceMin;
                return w.isOneoffDestination()
                        || (
                        w.getWaypointType() == 1
                                || w.isGlobal()
                                || w.isTemporary() && filterParams.temporaryWaypointsGlobal
                                || waypointsDistance == 0.0
                                || !(distance2D > waypointsDistance)
                )
                    && (waypointsDistanceMin == 0.0 || !(unscaledDistance2D < waypointsDistanceMin));
            } else {
                return false;
            }
        }
    };

    @Inject(method = "renderWaypointsIterator", at = @At("HEAD"))
    public void injectRenderWaypoints(final PoseStack matrixStack, final PoseStack matrixStackOverlay, final MinimapRendererHelper helper, final Iterator<Waypoint> iter, final double d3, final double d4, final double d5, final Entity entity, final BufferBuilder bufferbuilder, final Tesselator tessellator, final double dimDiv, final double actualEntityX, final double actualEntityY, final double actualEntityZ, final double smoothEntityY, final double fov, final int screenHeight, final float cameraAngleYaw, final float cameraAnglePitch, final Vector3f lookVector, final double clampDepth, final MultiBufferSource.BufferSource renderTypeBuffer, final VertexConsumer waypointBackgroundConsumer, final Font fontrenderer, final Matrix4f waypointsProjection, final int screenWidth, final boolean detailedDisplayAllowed, final double minDistance, final String subworldName, final CallbackInfo ci) {
        beaconWaypoints = sortingList.stream().filter(beaconViewFilter).sorted().collect(Collectors.toList());
    }

    @Override
    public void renderWaypointBeacons(final XaeroMinimapSession minimapSession, final PoseStack matrixStack, final float tickDelta) {
        if (!XaeroPlusSettingRegistry.waypointBeacons.getValue()) return;
        final WaypointsManager waypointsManager = minimapSession.getWaypointsManager();
        double dimDiv = waypointsManager.getDimensionDivision(waypointsManager.getCurrentContainerID());
        beaconWaypoints.forEach(w -> renderWaypointBeacon(w, dimDiv, tickDelta, matrixStack));
        beaconWaypoints.clear();
    }

    public void renderWaypointBeacon(final Waypoint waypoint, final double dimDiv, float tickDelta, PoseStack matrixStack) {
        final Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        final Vec3 playerVec = mc.player.position();
        Vec3 waypointVec = new Vec3(waypoint.getX(dimDiv), playerVec.y, waypoint.getZ(dimDiv));
        final double xzDistance = playerVec.distanceTo(waypointVec);
        if (xzDistance < (int) XaeroPlusSettingRegistry.waypointBeaconDistanceMin.getValue()) return;
        final int farScale = (int) XaeroPlusSettingRegistry.waypointBeaconScaleMin.getValue();
        final double maxRenderDistance = Math.min(mc.options.renderDistance().get() << 4, farScale == 0 ? Integer.MAX_VALUE : farScale << 4);
        if (xzDistance > maxRenderDistance) {
            final Vec3 delta = waypointVec.subtract(playerVec).normalize();
            waypointVec = playerVec.add(new Vec3(delta.x * maxRenderDistance, delta.y * maxRenderDistance, delta.z * maxRenderDistance));
        }
        final EntityRenderDispatcher entityRenderDispatcher = mc.getEntityRenderDispatcher();
        final Camera camera = entityRenderDispatcher.camera;
        final Frustum frustum = ((AccessorWorldRenderer) mc.levelRenderer).getFrustum();
        if (camera == null || frustum == null) return;
        final double viewX = camera.getPosition().x();
        final double viewZ = camera.getPosition().z();
        final double x = waypointVec.x - viewX;
        final double z = waypointVec.z - viewZ;
        final double y = -100;
        if (!frustum.isVisible(new AABB(waypointVec.x-1, -100, waypointVec.z-1, waypointVec.x+1, 500, waypointVec.z+1))) return;
        final int color = ModSettings.COLORS[waypoint.getColor()];
        final MultiBufferSource.BufferSource entityVertexConsumers = mc.renderBuffers().bufferSource();
        final long time = mc.level.getGameTime();
        matrixStack.pushPose();
        matrixStack.translate(x, y, z);
        BeaconRenderer.renderBeaconBeam(matrixStack, entityVertexConsumers, BEAM_LOCATION, tickDelta, 1.0f, time, 0, 355,
                                             ColorHelper.getColorRGBA(color), 0.2f, 0.25f);
        matrixStack.popPose();
    }
}
