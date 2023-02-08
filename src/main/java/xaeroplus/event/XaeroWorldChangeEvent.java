package xaeroplus.event;

import net.minecraftforge.fml.common.eventhandler.Event;

public class XaeroWorldChangeEvent extends Event {

    public final String worldId;
    public final String dimId;
    public final String mwId;

    public XaeroWorldChangeEvent(final String currentWorldId, final String currentDimId, final String currentMWId) {
        this.worldId = currentWorldId;
        this.dimId = currentDimId;
        this.mwId = currentMWId;
    }
}
