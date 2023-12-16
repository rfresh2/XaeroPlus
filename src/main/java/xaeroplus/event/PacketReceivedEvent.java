package xaeroplus.event;

import net.minecraft.network.packet.Packet;

public record PacketReceivedEvent(Packet<?> packet) { }
