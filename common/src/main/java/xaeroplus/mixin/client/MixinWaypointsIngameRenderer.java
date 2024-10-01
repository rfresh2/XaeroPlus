package xaeroplus.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.doubles.DoubleArrayFIFOQueue;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.minimap.MinimapProcessor;
import xaero.common.minimap.render.MinimapRendererHelper;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.common.minimap.waypoints.WaypointVisibilityType;
import xaero.common.minimap.waypoints.render.WaypointsIngameRenderer;
import xaero.hud.minimap.BuiltInHudModules;
import xaero.hud.minimap.module.MinimapSession;
import xaero.hud.minimap.waypoint.render.WaypointFilterParams;
import xaeroplus.feature.extensions.CustomWaypointsIngameRenderer;
import xaeroplus.settings.Settings;
import xaeroplus.util.ChunkUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static net.minecraft.client.renderer.blockentity.BeaconRenderer.BEAM_LOCATION;
import static net.minecraft.world.level.Level.NETHER;
import static net.minecraft.world.level.Level.OVERWORLD;

@Mixin(value = WaypointsIngameRenderer.class, remap = false)
public class MixinWaypointsIngameRenderer implements CustomWaypointsIngameRenderer {
    @Shadow private List<Waypoint> sortingList;
    @Shadow private WaypointFilterParams filterParams;
    @Unique List<Waypoint> beaconWaypoints = new ArrayList<>();
    @Unique final Predicate<Waypoint> beaconViewFilter = (w) -> {
        boolean deathpoints = filterParams.deathpoints;
        if (!w.isDisabled()
            && w.getVisibility() != WaypointVisibilityType.WORLD_MAP_LOCAL
            && w.getVisibility() != WaypointVisibilityType.WORLD_MAP_GLOBAL
            && (!w.getPurpose().isDeath() || deathpoints)) {
            double offX = (double)w.getX(filterParams.dimDiv) - filterParams.cameraPos.x + 0.5;
            double offZ = (double)w.getZ(filterParams.dimDiv) - filterParams.cameraPos.z + 0.5;
            double distanceScale = filterParams.dimensionScaleDistance ? Minecraft.getInstance().level.dimensionType().coordinateScale() : 1.0;
            double unscaledDistance2D = Math.sqrt(offX * offX + offZ * offZ);
            double distance2D = unscaledDistance2D * distanceScale;
            double waypointsDistance = filterParams.waypointsDistance;
            double waypointsDistanceMin = filterParams.waypointsDistanceMin;
            return w.isDestination()
                || (
                w.getPurpose().isDeath()
                    || w.isGlobal()
                    || w.isTemporary() && filterParams.temporaryWaypointsGlobal
                    || waypointsDistance == 0.0
                    || !(distance2D > waypointsDistance)
            )
                && (waypointsDistanceMin == 0.0 || !(unscaledDistance2D < waypointsDistanceMin));
        } else {
            return false;
        }
    };

    @Inject(method = "render", at = @At(
        value = "INVOKE",
        target = "Lxaero/hud/minimap/waypoint/render/WaypointDeleter;begin()V"
    ))
    public void preferOwWaypointsRemoveSubworldText(final MinimapSession session, final float partial, final MinimapProcessor minimap, final Matrix4f waypointsProjection, final Matrix4f worldModelView, final CallbackInfo ci,
                                                    @Local(name = "subworldName") LocalRef<String> subWorldNameRef) {
        if (!Settings.REGISTRY.owAutoWaypointDimension.get()) return;
        if (subWorldNameRef.get() == null) return;
        ResourceKey<Level> actualDimension = ChunkUtils.getActualDimension();
        ResourceKey<Level> currentWpWorldDim = session.getWorldManager().getCurrentWorld().getDimId();
        if (actualDimension == NETHER && currentWpWorldDim == OVERWORLD) {
            subWorldNameRef.set(null);
        }
    }

    @Inject(method = "renderWaypointsIterator", at = @At("HEAD"))
    public void collectBeaconWaypointsList(final PoseStack matrixStack, final PoseStack matrixStackOverlay, final MinimapRendererHelper helper, final Iterator<Waypoint> iter, final double d3, final double d4, final double d5, final Entity entity, final Tesselator tessellator, final double dimDiv, final double actualEntityX, final double actualEntityY, final double actualEntityZ, final double smoothEntityY, final double fov, final int screenHeight, final float cameraAngleYaw, final float cameraAnglePitch, final Vector3f lookVector, final double clampDepth, final MultiBufferSource.BufferSource renderTypeBuffer, final VertexConsumer waypointBackgroundConsumer, final Font fontrenderer, final Matrix4f waypointsProjection, final int screenWidth, final boolean detailedDisplayAllowed, final double minDistance, final String subworldName, final CallbackInfo ci) {
        beaconWaypoints = sortingList.stream().filter(beaconViewFilter).sorted().collect(Collectors.toList());
    }

    @Override
    public void renderWaypointBeacons(final MinimapSession minimapSession, final PoseStack matrixStack, final float tickDelta) {
        double dimDiv = minimapSession.getDimensionHelper().getDimensionDivision(minimapSession.getWorldManager().getCurrentWorld());
        beaconWaypoints.forEach(w -> renderWaypointBeacon(w, dimDiv, tickDelta, matrixStack));
        beaconWaypoints.clear();
    }

    @Unique
    public void renderWaypointBeacon(final Waypoint waypoint, final double dimDiv, float tickDelta, PoseStack matrixStack) {
        final Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        final Vec3 playerVec = mc.player.position();
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
            matrixStack, entityVertexConsumers, BEAM_LOCATION, tickDelta,
            1.0f, time, 0, 355,
            color, 0.2f, 0.25f);
        matrixStack.popPose();
    }

    /**
     * todo: separate out rendering so it is independent of when distance text is rendered
     *  and put it on its own line
     */
    @ModifyArg(method = "renderWaypointIngame", at = @At(
        value = "INVOKE",
        target = "Lxaero/common/minimap/waypoints/render/WaypointsIngameRenderer;drawAsOverlay(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lxaero/common/minimap/render/MinimapRendererHelper;Lxaero/common/minimap/waypoints/Waypoint;Lxaero/common/settings/ModSettings;Lcom/mojang/blaze3d/vertex/Tesselator;Lnet/minecraft/client/gui/Font;Ljava/lang/String;Ljava/lang/String;FZLnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lorg/joml/Matrix4f;IIDDZLjava/lang/String;)V"),
    index = 8,
    remap = true) // $REMAP
    public String modifyDistanceText(final String text, @Local(argsOnly = true) Waypoint waypoint) {
        if (!Settings.REGISTRY.waypointEta.get()) return text;
        if (text.isBlank()) return text;
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
    @Unique long xaeroPlus$lastSpeedUpdate = 0;
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
