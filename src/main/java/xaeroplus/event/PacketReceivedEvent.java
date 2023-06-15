package xaeroplus.event;

import com.collarmc.pounce.EventInfo;
import net.minecraft.network.packet.Packet;

@EventInfo
public record PacketReceivedEvent(Packet<?> packet) { }
