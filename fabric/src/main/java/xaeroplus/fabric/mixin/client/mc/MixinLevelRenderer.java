package xaeroplus.fabric.mixin.client.mc;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaeroplus.feature.render.beacon.WaypointBeaconRenderer;

@Mixin(value = LevelRenderer.class)
public class MixinLevelRenderer {

    @Inject(
        method = "renderBlockEntities",
        at = @At("HEAD")
    )
    public void renderBlockEntitiesInject(final PoseStack matrix, final MultiBufferSource.BufferSource bufferSource, final MultiBufferSource.BufferSource bufferSource2, final Camera camera, final float tickDelta, final CallbackInfo ci) {
        WaypointBeaconRenderer.INSTANCE.renderHook(matrix, tickDelta);
    }
}
