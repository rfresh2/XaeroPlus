package xaeroplus.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.common.events.ForgeEventHandler;
import xaeroplus.XaeroPlus;
import xaeroplus.event.MinimapRenderEvent;
import xaeroplus.settings.XaeroPlusSettingRegistry;

@Mixin(value = ForgeEventHandler.class, remap = false)
public abstract class MixinForgeEventHandler {
    @Inject(method = "handleClientPlayerChatReceivedEvent", at = @At("HEAD"), cancellable = true)
    public void onPlayerChatReceived(final ChatType chatType, final Component component, final GameProfile gameProfile, final CallbackInfoReturnable<Boolean> cir) {
        if (component == null) return;
        if (XaeroPlusSettingRegistry.disableReceivingWaypoints.getValue()) {
            // cancelling at head so we avoid hitting the logic to parse the waypoint string
            cir.setReturnValue(false); // false will show the raw message in chat to the player
        }
    }

    @Inject(method = "handleClientSystemChatReceivedEvent", at = @At("HEAD"), cancellable = true)
    public void onSystemChatReceived(final Component component, final CallbackInfoReturnable<Boolean> cir) {
        if (component == null) return;
        if (XaeroPlusSettingRegistry.disableReceivingWaypoints.getValue()) {
            // cancelling at head so we avoid hitting the logic to parse the waypoint string
            cir.setReturnValue(false); // false will show the raw message in chat to the player
        }
    }

    @WrapOperation(method = "handleRenderGameOverlayEventPre", at =
    @At(
        value = "INVOKE",
        target = "Lxaero/common/events/ForgeEventHandler;handleRenderGameOverlayEventPreOverridable(Lnet/minecraft/client/gui/GuiGraphics;F)V"
    ), remap = true)
    public void handleRenderGameOverlayEventPre(final ForgeEventHandler instance, final GuiGraphics guiGraphics, final float partialTicks, final Operation<Void> original) {
        MinimapRenderEvent event = new MinimapRenderEvent();
        XaeroPlus.EVENT_BUS.call(event);
        if (!event.cancelled) original.call(instance, guiGraphics, partialTicks);
        if (event.postRenderCallback != null) event.postRenderCallback.run();
    }
}
