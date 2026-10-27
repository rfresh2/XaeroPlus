package xaeroplus.feature.render.async;

import xaeroplus.XaeroPlus;

import java.util.ArrayDeque;

/**
 * Render thread work that must stay on the render thread but is too large for one frame.
 * Steps run in submission order within a small time budget per frame while the map screen is open.
 * Once it closes, they run to completion.
 */
public final class FrameBudgetQueue {
    private static final long BUDGET_NANOS = 2_000_000L;
    private static final ArrayDeque<Step> QUEUE = new ArrayDeque<>();

    private FrameBudgetQueue() {}

    @FunctionalInterface
    public interface Step {
        /**
         * Does work until {@code deadline} ({@link System#nanoTime()}); returns true once finished.
         */
        boolean run(long deadline);
    }

    /**
     * Render thread only.
     */
    public static void submit(Step step) {
        QUEUE.add(step);
        if (!WorldMapScreenState.isOpen()) {
            drain();
        }
    }

    /**
     * Render thread: finishes every queued step now.
     */
    public static void drain() {
        while (!QUEUE.isEmpty()) {
            run(QUEUE.peek(), Long.MAX_VALUE);
            QUEUE.poll();
        }
    }

    /**
     * Render thread, once per frame.
     */
    public static void runFrame() {
        long deadline = System.nanoTime() + BUDGET_NANOS;
        while (!QUEUE.isEmpty() && System.nanoTime() < deadline) {
            if (run(QUEUE.peek(), deadline)) {
                QUEUE.poll();
            }
        }
    }

    private static boolean run(Step step, long deadline) {
        try {
            return step.run(deadline);
        } catch (final Throwable t) {
            XaeroPlus.LOGGER.error("Deferred render thread work failed", t);
            return true;
        }
    }

    /**
     * True every 256 items once the deadline has passed.
     */
    public static boolean outOfTime(int item, long deadline) {
        return (item & 255) == 0 && System.nanoTime() >= deadline;
    }
}
