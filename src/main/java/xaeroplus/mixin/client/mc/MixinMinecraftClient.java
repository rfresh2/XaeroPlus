package xaeroplus.mixin.client.mc;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaeroplus.XaeroPlus;
import xaeroplus.event.ClientTickEvent;

@Mixin(value = Minecraft.class)
public class MixinMinecraftClient {
    @Inject(method = "tick", at = @At("HEAD"))
    public void tickHead(final CallbackInfo ci) {
        XaeroPlus.EVENT_BUS.call(ClientTickEvent.Pre.INSTANCE);
    }

    @Inject(method = "tick", at = @At("RETURN"))
    public void tickReturn(final CallbackInfo ci) {
        XaeroPlus.EVENT_BUS.call(ClientTickEvent.Post.INSTANCE);
    }
}
