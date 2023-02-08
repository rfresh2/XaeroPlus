package xaeroplus.event;

import net.minecraft.network.Packet;
import net.minecraftforge.fml.common.eventhandler.Event;

public class PacketReceivedEvent extends Event {
    public Packet<?> packet;

    public PacketReceivedEvent(Packet<?> packet) {
        this.packet = packet;
    }
}
