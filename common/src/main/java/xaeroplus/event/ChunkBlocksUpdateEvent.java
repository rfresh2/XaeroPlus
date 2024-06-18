package xaeroplus.event;

import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;

// fired right before block updates are applied to mc.level
public record ChunkBlocksUpdateEvent(ClientboundSectionBlocksUpdatePacket packet) { }
