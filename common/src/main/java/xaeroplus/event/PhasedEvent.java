package xaeroplus.event;

public abstract class PhasedEvent {
    private Phase phase = Phase.PRE;

    public Phase phase() {
        return phase;
    }

    public void setPhase(Phase phase) {
        this.phase = phase;
    }
}
