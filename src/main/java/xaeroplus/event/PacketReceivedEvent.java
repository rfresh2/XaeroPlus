package xaeroplus.event;

import net.minecraft.network.Packet;

public class PacketReceivedEvent {
    public Packet<?> packet;

    public PacketReceivedEvent(Packet<?> packet) {
        this.packet = packet;
    }
}
