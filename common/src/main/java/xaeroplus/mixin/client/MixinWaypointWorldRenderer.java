package xaeroplus.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.BuiltInHudModules;
import xaero.hud.minimap.waypoint.WaypointPurpose;
import xaero.hud.minimap.waypoint.render.world.WaypointWorldRenderer;
import xaeroplus.feature.waypoint.eta.WaypointEtaManager;
import xaeroplus.settings.Settings;
import xaeroplus.util.ChunkUtils;

import static net.minecraft.world.level.Level.NETHER;
import static net.minecraft.world.level.Level.OVERWORLD;

@Mixin(value = WaypointWorldRenderer.class, remap = false)
public class MixinWaypointWorldRenderer {

    @Shadow private String subWorldName;

    @Shadow
    private double waypointsDistance;

    @Inject(method = "renderElement(Lxaero/common/minimap/waypoints/Waypoint;ZZDFDDLxaero/hud/minimap/element/render/MinimapElementRenderInfo;Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;)Z", at = @At(
        value = "INVOKE",
        target = "Lxaero/common/minimap/waypoints/Waypoint;isDestination()Z",
        ordinal = 0
    ), cancellable = true,
        remap = true) // $REMAP
    public void limitDeathpointsRenderDistance(
        final CallbackInfoReturnable<Boolean> cir,
        @Local(argsOnly = true) Waypoint w,
        @Local(name = "scaledDistance2D") double scaledDistance2D
    ) {
        var purpose = w.getPurpose();
        if (purpose == WaypointPurpose.DEATH && Settings.REGISTRY.limitDeathpointsRenderDistance.get()) {
            if (waypointsDistance != 0 && scaledDistance2D > waypointsDistance) {
                cir.setReturnValue(false);
            }
        }
    }

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
        var etaText = WaypointEtaManager.INSTANCE.getEtaTextSuffix(waypoint);
        return text + etaText;
    }
}
