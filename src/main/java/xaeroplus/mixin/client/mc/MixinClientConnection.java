package xaeroplus.mixin.client.mc;

import net.minecraft.network.ClientConnection;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaeroplus.XaeroPlus;
import xaeroplus.event.PacketReceivedEvent;

@Mixin(value = ClientConnection.class)
public class MixinClientConnection {

    @Inject(method = "handlePacket", at = @At("HEAD"))
    private static void receivePacket(final Packet<?> packet, final PacketListener listener, final CallbackInfo ci) {
        XaeroPlus.EVENT_BUS.call(new PacketReceivedEvent(packet));
    }
}
