package xaeroplus.mixin.client.mc;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaeroplus.Globals;
import xaeroplus.XaeroPlus;
import xaeroplus.event.ClientTickEvent;
import xaeroplus.module.impl.FpsLimiter;
import xaeroplus.settings.Settings;

@Mixin(
    value = Minecraft.class,
    priority = 999 // MUST be before xaero mods mixins to handle dimension switch correctly
)
public class MixinMinecraftClient {
    @Shadow public ClientLevel level;

    @Inject(method = "runTick", at = @At("HEAD"))
    public void renderTickHead(final CallbackInfo ci) {
        XaeroPlus.EVENT_BUS.call(ClientTickEvent.RenderPre.INSTANCE);
    }

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

    @Inject(method = "setLevel", at = @At("HEAD"))
    public void onLevelChangePre(final ClientLevel newWorld, final ReceivingLevelScreen.Reason reason, final CallbackInfo ci) {
        var prev = this.level;
        if (prev != null && newWorld != null) {
            Globals.switchingDimension = true;
        }
    }

    @Inject(method = "setLevel", at = @At("RETURN"))
    public void onLevelChangePost(CallbackInfo info) {
        Globals.switchingDimension = false;
    }
}
