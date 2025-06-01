package xaeroplus.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.element.render.MinimapElementRenderInfo;
import xaero.hud.minimap.waypoint.WaypointPurpose;
import xaero.hud.minimap.waypoint.render.WaypointMapRenderer;
import xaeroplus.settings.Settings;

@Mixin(value = WaypointMapRenderer.class, remap = false)
public class MixinWaypointMapRenderer {

    @Shadow
    private double waypointsDistance;

    @Inject(method = "renderElement(Lxaero/common/minimap/waypoints/Waypoint;ZZDFDDLxaero/hud/minimap/element/render/MinimapElementRenderInfo;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;)Z", at = @At(
        value = "INVOKE",
        target = "Lxaero/common/minimap/waypoints/Waypoint;isDestination()Z",
        ordinal = 0
    ), cancellable = true,
        remap = true) // $REMAP
    public void limitDeathpointsRenderDistance(
        final Waypoint w, final boolean highlighted, final boolean outOfBounds, final double optionalDepth, final float optionalScale, final double partialX, final double partialY, final MinimapElementRenderInfo renderInfo, final PoseStack guiGraphics, final MultiBufferSource.BufferSource vanillaBufferSource, final CallbackInfoReturnable<Boolean> cir,
        @Local(name = "scaledDistance2D") double scaledDistance2D
    ) {
        if (Settings.REGISTRY.limitDeathpointsRenderDistance.get()) {
            if (w.getPurpose() == WaypointPurpose.DEATH
                && waypointsDistance != 0
                && scaledDistance2D > waypointsDistance
            ) {
                cir.setReturnValue(false);
            }
        }
    }
}
