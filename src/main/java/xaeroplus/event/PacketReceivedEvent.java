package xaeroplus.event;

import net.minecraft.network.protocol.Packet;

public record PacketReceivedEvent(Packet<?> packet) { }
