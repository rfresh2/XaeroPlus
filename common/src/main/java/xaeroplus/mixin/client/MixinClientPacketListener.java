package xaeroplus.mixin.client;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaeroplus.XaeroPlus;
import xaeroplus.event.RespawnObstructedEvent;

@Mixin(ClientPacketListener.class)
public class MixinClientPacketListener {
    @Inject(method = "handleGameEvent", at = @At("HEAD"))
    private void onGameEvent(ClientboundGameEventPacket packet, CallbackInfo ci) {
        if (packet.getEvent() == ClientboundGameEventPacket.NO_RESPAWN_BLOCK_AVAILABLE)
            XaeroPlus.EVENT_BUS.call(new RespawnObstructedEvent());
    }
}
