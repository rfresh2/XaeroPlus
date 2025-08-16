package xaeroplus.util.timer;

public interface Timer {
    // reset the last timer tick time to the current time
    void reset();
    // skip to a state where the next timer tick will always return true
    void skip();
    // returns true if the delay has been reached. resets the last tick timer
    boolean tick(long delay);
    // returns true if the delay has been reached. resets the last tick timer if resetIfTick is true
    boolean tick(long delay, boolean resetIfTick);
}
