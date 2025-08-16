package xaeroplus.util.timer;

public class SyncedTickTimer implements Timer {
    private long time = 0;

    SyncedTickTimer() {}

    public void reset() {
        this.time = TickTimerManager.INSTANCE.getTickTime();
    }

    public void skip() {
        this.time = 0L;
    }

    public boolean tick(final long delay) {
        return tick(delay, true);
    }

    public boolean tick(final long delay, final boolean resetIfTick) {
        if (TickTimerManager.INSTANCE.getTickTime() - this.time > delay) {
            if (resetIfTick) this.time = TickTimerManager.INSTANCE.getTickTime();
            return true;
        } else {
            return false;
        }
    }
}
