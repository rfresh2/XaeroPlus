package xaeroplus.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.common.minimap.render.radar.element.RadarRenderer;
import xaeroplus.Globals;

import static java.util.Objects.nonNull;

@Mixin(value = RadarRenderer.class, remap = false)
public class MixinRadarRenderer {

    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    public void shouldRender(final int location, final CallbackInfoReturnable<Boolean> cir) {
        ClientWorld world = MinecraftClient.getInstance().world;
        if (nonNull(world) && Globals.customDimensionId != world.getRegistryKey()) {
            cir.setReturnValue(false);
        }
    }
}
