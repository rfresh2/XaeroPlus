package xaeroplus.util;


import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Supplier;

public class Wait {
    public static void wait(int seconds) {
        try {
            Thread.sleep(TimeUnit.SECONDS.toMillis(seconds));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static void waitMs(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("InfiniteLoopStatement")
    public static void waitSpinLoop() {
        while (true) {
            try {
                Thread.sleep(2147483647L);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static boolean waitUntil(final Supplier<Boolean> conditionSupplier, int secondsToWait) {
        return waitUntil(conditionSupplier, 50, secondsToWait, TimeUnit.SECONDS);
    }

    public static boolean waitUntil(final Supplier<Boolean> conditionSupplier, int checkIntervalMs, long timeout, TimeUnit unit) {
        final var beforeTime = System.nanoTime();
        while (!conditionSupplier.get() && TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - beforeTime) < unit.toMillis(timeout)) {
            if (Thread.currentThread().isInterrupted()) {
                throw new RuntimeException("Wait Interrupted");
            }
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(checkIntervalMs));
        }
        return conditionSupplier.get();
    }

    public static void waitRandomMs(final int ms) {
        Wait.waitMs((int) (ThreadLocalRandom.current().nextDouble(ms)));
    }
}
