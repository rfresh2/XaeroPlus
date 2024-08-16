package xaeroplus.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.authlib.GameProfile;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.common.events.ClientEvents;
import xaero.common.minimap.waypoints.WaypointSharingHandler;
import xaeroplus.XaeroPlus;
import xaeroplus.settings.XaeroPlusSettingRegistry;

@Mixin(value = ClientEvents.class, remap = false)
public abstract class MixinClientEvents {
    @Inject(method = "handleClientSystemChatReceivedEvent", at = @At("HEAD"), cancellable = true)
    public void onSystemChatReceived(final Component component, final CallbackInfoReturnable<Boolean> cir) {
        if (component == null) return;
        if (XaeroPlusSettingRegistry.disableReceivingWaypoints.getValue()) {
            // cancelling at head so we avoid hitting the logic to parse the waypoint string
            cir.setReturnValue(false); // false will show the raw message in chat to the player
        }
    }

    @Inject(method = "handleClientPlayerChatReceivedEvent", at = @At("HEAD"), cancellable = true)
    public void onPlayerChatReceived(final ChatType.Bound chatType, final Component component, final GameProfile gameProfile, final CallbackInfoReturnable<Boolean> cir) {
        if (component == null) return;
        if (XaeroPlusSettingRegistry.disableReceivingWaypoints.getValue()) {
            // cancelling at head so we avoid hitting the logic to parse the waypoint string
            cir.setReturnValue(false); // false will show the raw message in chat to the player
        }
    }

    @WrapOperation(method = "handleChatMessage", at = @At(
        value = "INVOKE",
        target = "Lxaero/common/minimap/waypoints/WaypointSharingHandler;onWaypointReceived(Ljava/lang/String;Ljava/lang/String;)V"
    ))
    public void preventInvalidWpFormattingCrash(final WaypointSharingHandler instance, final String playerName, final String textString, final Operation<Void> original) {
        try {
            original.call(instance, playerName, textString);
        } catch (final Exception e) {
            XaeroPlus.LOGGER.info("Caught exception in waypoint sharing handler: ", e);
        }
    }
}
