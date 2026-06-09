package xaeroplus.event;

import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;

public record ChunkBlockEntityUpdateEvent(ClientboundBlockEntityDataPacket packet) { }
