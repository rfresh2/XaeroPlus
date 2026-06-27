package xaeroplus.mixin.client.mc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaeroplus.Globals;
import xaeroplus.XaeroPlus;
import xaeroplus.event.ClientStoppingEvent;
import xaeroplus.event.ClientTickEvent;

@Mixin(
    value = Minecraft.class,
    priority = 999 // MUST be before xaero mods mixins to handle dimension switch correctly
)
public class MixinMinecraft {
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

    @Inject(method = "setLevel", at = @At("HEAD"))
    public void onLevelChangePre(final ClientLevel newWorld, final CallbackInfo ci) {
        var prev = this.level;
        if (prev != null && newWorld != null) {
            Globals.switchingDimension = true;
        }
    }

    @Inject(method = "setLevel", at = @At("RETURN"))
    public void onLevelChangePost(CallbackInfo info) {
        Globals.switchingDimension = false;
    }

    @Inject(method = "exitWorldAndClose", at = @At(
        value = "INVOKE",
        target = "Lorg/slf4j/Logger;info(Ljava/lang/String;)V",
        shift = At.Shift.AFTER
    ))
    public void onDestroy(CallbackInfo info) {
        XaeroPlus.EVENT_BUS.call(ClientStoppingEvent.INSTANCE);
    }
}
