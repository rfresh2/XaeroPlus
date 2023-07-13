package xaeroplus.mixin.client.mc;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.XaeroMinimapSession;
import xaero.common.minimap.waypoints.render.WaypointsIngameRenderer;
import xaeroplus.util.CustomWaypointsIngameRenderer;

@Mixin(value = WorldRenderer.class)
public class MixinWorldRenderer {

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/WorldRenderer;checkEmpty(Lnet/minecraft/client/util/math/MatrixStack;)V", ordinal = 1, shift = At.Shift.AFTER))
    public void renderBlockEntitiesInject(final MatrixStack matrixStack, final float tickDelta, final long limitTime, final boolean renderBlockOutline, final Camera camera, final GameRenderer gameRenderer, final LightmapTextureManager lightmapTextureManager, final Matrix4f positionMatrix, final CallbackInfo ci) {
        final XaeroMinimapSession minimapSession = XaeroMinimapSession.getCurrentSession();
        if (minimapSession == null) return;
        WaypointsIngameRenderer waypointsIngameRenderer = minimapSession.getModMain().getInterfaces().getMinimapInterface().getWaypointsIngameRenderer();
        ((CustomWaypointsIngameRenderer) waypointsIngameRenderer).renderWaypointBeacons(minimapSession, matrixStack, tickDelta);
    }
}
