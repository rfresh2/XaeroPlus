package xaeroplus.event;

public record ClientStoppingEvent() {
    public static final ClientStoppingEvent INSTANCE = new ClientStoppingEvent();
}
