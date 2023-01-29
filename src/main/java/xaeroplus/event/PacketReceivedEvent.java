package xaeroplus.event;

import com.collarmc.pounce.EventInfo;
import com.collarmc.pounce.Preference;
import net.minecraft.network.Packet;

@EventInfo(preference = Preference.CALLER)
public class PacketReceivedEvent {
    public Packet<?> packet;

    public PacketReceivedEvent(Packet<?> packet) {
        this.packet = packet;
    }
}
