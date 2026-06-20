package xaeroplus.fabric.mixin.client.mc;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaeroplus.feature.render.beacon.WaypointBeaconRenderer;

@Mixin(value = LevelRenderer.class)
public class MixinLevelRenderer {

    @Inject(
        method = "submitBlockEntities",
        at = @At("HEAD")
    )
    public void renderBlockEntitiesInject(final PoseStack poseStack, final LevelRenderState levelRenderState, final SubmitNodeCollector submitNodeCollector, final CallbackInfo ci) {
        WaypointBeaconRenderer.INSTANCE.renderHook(poseStack, levelRenderState, submitNodeCollector);
    }
}
