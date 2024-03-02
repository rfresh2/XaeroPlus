package xaeroplus.mixin.client.mc;

import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaeroplus.XaeroPlus;
import xaeroplus.event.PacketReceivedEvent;

@Mixin(value = Connection.class)
public class MixinClientConnection {

    @Inject(method = "genericsFtw", at = @At("HEAD"))
    private static void receivePacket(final Packet<?> packet, final PacketListener listener, final CallbackInfo ci) {
        XaeroPlus.EVENT_BUS.call(new PacketReceivedEvent(packet));
    }
}
