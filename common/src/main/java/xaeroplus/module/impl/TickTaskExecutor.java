package xaeroplus.module.impl;

import net.lenni0451.lambdaevents.EventHandler;
import org.jetbrains.annotations.NotNull;
import xaeroplus.XaeroPlus;
import xaeroplus.event.ClientTickEvent;
import xaeroplus.module.Module;

import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * Tasks to execute on the main mc thread
 *
 * Tasks are executed immediately if we are already on the main thread
 * otherwise executed on the following render tick
 *
 * Same as Minecraft.class executor, but we never drop tasks on disconnect
 */
public class TickTaskExecutor extends Module implements Executor {
    private final Queue<Runnable> tasks = new ConcurrentLinkedQueue<>();

    public TickTaskExecutor() {
        enable();
    }

    @EventHandler
    public void onRenderTick(ClientTickEvent.RenderPre event) {
        while (!tasks.isEmpty()) {
            try {
                tasks.poll().run();
            } catch (final Exception e) {
                XaeroPlus.LOGGER.error("Caught exception in tick task", e);
            }
        }
    }

    public <V> CompletableFuture<V> submit(Supplier<V> task) {
        Supplier<V> wrapped = wrap(task);
        if (mc.isSameThread()) {
            return CompletableFuture.completedFuture(wrapped.get());
        }
        CompletableFuture<V> future = new CompletableFuture<>();
        tasks.add(() -> {
            try {
                var result = wrapped.get();
                future.complete(result);
            } catch (final Throwable e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    public CompletableFuture<Void> submit(Runnable task) {
        Runnable wrapped = wrap(task);
        if (mc.isSameThread()) {
            wrapped.run();
            return CompletableFuture.completedFuture(null);
        }
        CompletableFuture<Void> future = new CompletableFuture<>();
        tasks.add(() -> {
            wrapped.run();
            future.complete(null);
        });
        return future;
    }

    @Override
    public void execute(@NotNull final Runnable command) {
        submit(command);
    }

    private <T> Supplier<T> wrap(Supplier<T> task) {
        return () -> {
            try {
                return task.get();
            } catch (final Throwable e) {
                XaeroPlus.LOGGER.error("Caught exception in tick task", e);
                return null;
            }
        };
    }

    private Runnable wrap(Runnable task) {
        return () -> {
            try {
                task.run();
            } catch (final Throwable e) {
                XaeroPlus.LOGGER.error("Caught exception in tick task", e);
            }
        };
    }
}
