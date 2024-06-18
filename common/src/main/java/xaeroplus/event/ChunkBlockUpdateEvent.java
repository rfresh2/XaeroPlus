package xaeroplus.event;

import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;

// fired right before block update is applied to mc.level
public record ChunkBlockUpdateEvent(ClientboundBlockUpdatePacket packet) { }
