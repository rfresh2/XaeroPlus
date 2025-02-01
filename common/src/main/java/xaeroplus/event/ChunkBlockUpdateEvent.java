package xaeroplus.event;

import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;

public class ChunkBlockUpdateEvent extends PhasedEvent {
    private final ClientboundBlockUpdatePacket packet;

    public ChunkBlockUpdateEvent(final ClientboundBlockUpdatePacket packet) {
        this.packet = packet;
    }

    public ClientboundBlockUpdatePacket packet() {
        return packet;
    }
}
