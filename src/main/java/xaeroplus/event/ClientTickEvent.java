package xaeroplus.event;

import com.collarmc.pounce.EventInfo;

@EventInfo
public class ClientTickEvent {
    public static class Pre extends ClientTickEvent {
        public static Pre INSTANCE = new Pre();
    }

    public static class Post extends ClientTickEvent {
        public static Post INSTANCE = new Post();
    }
}
