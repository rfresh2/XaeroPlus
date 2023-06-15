package xaeroplus.event;

import com.collarmc.pounce.EventInfo;

@EventInfo
public record XaeroWorldChangeEvent(String worldId, String dimId, String mwId) { }
