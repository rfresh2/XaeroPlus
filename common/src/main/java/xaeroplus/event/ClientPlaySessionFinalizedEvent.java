package xaeroplus.event;

public record ClientPlaySessionFinalizedEvent() {
    public static ClientPlaySessionFinalizedEvent INSTANCE = new ClientPlaySessionFinalizedEvent();
}
