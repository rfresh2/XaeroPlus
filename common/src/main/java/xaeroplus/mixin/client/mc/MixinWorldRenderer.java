package xaeroplus.mixin.client.mc;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.HudMod;
import xaero.common.minimap.waypoints.render.WaypointsIngameRenderer;
import xaero.hud.minimap.BuiltInHudModules;
import xaero.hud.minimap.Minimap;
import xaero.hud.minimap.module.MinimapSession;
import xaeroplus.XaeroPlus;
import xaeroplus.feature.extensions.CustomWaypointsIngameRenderer;
import xaeroplus.settings.Settings;

@Mixin(value = LevelRenderer.class)
public class MixinWorldRenderer {

    private int errorCount = 0;
    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;checkPoseStack(Lcom/mojang/blaze3d/vertex/PoseStack;)V", ordinal = 1, shift = At.Shift.AFTER))
    public void renderBlockEntitiesInject(final DeltaTracker deltaTracker, final boolean renderBlockOutline, final Camera camera, final GameRenderer gameRenderer, final LightTexture lightTexture, final Matrix4f projectionMatrix, final Matrix4f frustumMatrix, final CallbackInfo ci,
                                          @Local PoseStack poseStack) {
        if (!Settings.REGISTRY.waypointBeacons.get()) return;
        HudMod hudMod = HudMod.INSTANCE;
        if (hudMod == null) return;
        Minimap minimap = hudMod.getMinimap();
        if (minimap == null) return;
        WaypointsIngameRenderer waypointsIngameRenderer = minimap.getWaypointsIngameRenderer();
        if (waypointsIngameRenderer == null) return;
        MinimapSession minimapSession = BuiltInHudModules.MINIMAP.getCurrentSession();
        if (minimapSession == null) return;
        try {
            ((CustomWaypointsIngameRenderer) waypointsIngameRenderer).renderWaypointBeacons(minimapSession, poseStack, deltaTracker.getGameTimeDeltaPartialTick(false));
        } catch (final Exception e) {
            if (errorCount++ < 2) XaeroPlus.LOGGER.error("Error rendering waypoints", e);
        }
    }
}
