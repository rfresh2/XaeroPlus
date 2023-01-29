package xaeroplus.event;

public class XaeroWorldChangeEvent {

    public final String worldId;
    public final String dimId;
    public final String mwId;

    public XaeroWorldChangeEvent(final String currentWorldId, final String currentDimId, final String currentMWId) {
        this.worldId = currentWorldId;
        this.dimId = currentDimId;
        this.mwId = currentMWId;
    }
}
