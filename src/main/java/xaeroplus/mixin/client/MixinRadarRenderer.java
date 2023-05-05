package xaeroplus.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.common.minimap.render.radar.element.RadarRenderer;
import xaeroplus.util.Shared;

import static java.util.Objects.nonNull;

@Mixin(value = RadarRenderer.class, remap = false)
public class MixinRadarRenderer {

    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    public void shouldRender(final int location, final CallbackInfoReturnable<Boolean> cir) {
        WorldClient world = Minecraft.getMinecraft().world;
        if (nonNull(world) && Shared.customDimensionId != world.provider.getDimension()) {
            cir.setReturnValue(false);
        }
    }
}
