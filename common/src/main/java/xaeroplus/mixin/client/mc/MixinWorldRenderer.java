package xaeroplus.mixin.client.mc;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
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
import xaeroplus.settings.XaeroPlusSettingRegistry;

@Mixin(value = LevelRenderer.class)
public class MixinWorldRenderer {

    private int errorCount = 0;
    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;checkPoseStack(Lcom/mojang/blaze3d/vertex/PoseStack;)V", ordinal = 1, shift = At.Shift.AFTER))
    public void renderBlockEntitiesInject(final PoseStack matrixStack, final float tickDelta, final long limitTime, final boolean renderBlockOutline, final Camera camera, final GameRenderer gameRenderer, final LightTexture lightmapTextureManager, final Matrix4f positionMatrix, final CallbackInfo ci) {
        if (!XaeroPlusSettingRegistry.waypointBeacons.getValue()) return;
        HudMod hudMod = HudMod.INSTANCE;
        if (hudMod == null) return;
        Minimap minimap = hudMod.getMinimap();
        if (minimap == null) return;
        WaypointsIngameRenderer waypointsIngameRenderer = minimap.getWaypointsIngameRenderer();
        if (waypointsIngameRenderer == null) return;
        MinimapSession minimapSession = BuiltInHudModules.MINIMAP.getCurrentSession();
        if (minimapSession == null) return;
        try {
            ((CustomWaypointsIngameRenderer) waypointsIngameRenderer).renderWaypointBeacons(minimapSession, matrixStack, tickDelta);
        } catch (final Exception e) {
            if (errorCount++ < 2) XaeroPlus.LOGGER.error("Error rendering waypoints", e);
        }
    }
}
