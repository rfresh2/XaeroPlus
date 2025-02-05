package xaeroplus.event;

public class ClientTickEvent {
    public static class Pre extends ClientTickEvent {
        public static final Pre INSTANCE = new Pre();
    }

    public static class Post extends ClientTickEvent {
        public static final Post INSTANCE = new Post();
    }

    // called on every frame tick, not regular 50ms client ticks
    public static class RenderPre extends ClientTickEvent {
        public static final RenderPre INSTANCE = new RenderPre();
    }
}
