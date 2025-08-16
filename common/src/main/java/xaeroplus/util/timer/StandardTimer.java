package xaeroplus.util.timer;

public class StandardTimer implements Timer {
    private long time = System.currentTimeMillis();
    private final long tickTimeMs;

    StandardTimer() {
        this.tickTimeMs = 1L;
    }

    StandardTimer(final long tickTimeMs) {
        this.tickTimeMs = tickTimeMs;
    }

    public void reset() {
        this.time = System.currentTimeMillis();
    }

    public void skip() {
        this.time = 0L;
    }

    public boolean tick(final long delay) {
        return tick(delay, true);
    }

    public boolean tick(final long delay, final boolean resetIfTick) {
        if (System.currentTimeMillis() - this.time > delay * tickTimeMs) {
            if (resetIfTick) this.time = System.currentTimeMillis();
            return true;
        } else {
            return false;
        }
    }
}
