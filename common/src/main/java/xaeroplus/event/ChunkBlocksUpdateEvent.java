package xaeroplus.event;

import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;

public class ChunkBlocksUpdateEvent extends PhasedEvent {
    private final ClientboundSectionBlocksUpdatePacket packet;

    public ChunkBlocksUpdateEvent(final ClientboundSectionBlocksUpdatePacket packet) {
        this.packet = packet;
    }

    public ClientboundSectionBlocksUpdatePacket packet() {
        return packet;
    }
}
