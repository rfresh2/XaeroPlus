package xaeroplus.mixin.client.mc;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaeroplus.XaeroPlus;
import xaeroplus.event.ClientTickEvent;
import xaeroplus.module.impl.FpsLimiter;
import xaeroplus.settings.Settings;

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

    @Inject(method = "getMainRenderTarget", at = @At("HEAD"), cancellable = true)
    public void getMainRenderTarget(CallbackInfoReturnable<RenderTarget> ci) {
        if (!Settings.REGISTRY.minimapFpsLimiter.get()) return;
        var renderTargetOverwrite = FpsLimiter.renderTargetOverwrite;
        if (renderTargetOverwrite != null) ci.setReturnValue(renderTargetOverwrite);
    }
}
