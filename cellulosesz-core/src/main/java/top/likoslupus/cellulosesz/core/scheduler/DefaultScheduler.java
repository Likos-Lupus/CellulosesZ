package top.likoslupus.cellulosesz.core.scheduler;

import top.likoslupus.cellulosesz.api.scheduler.Scheduler;
import top.likoslupus.cellulosesz.api.scheduler.TaskHandle;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

public final class DefaultScheduler implements Scheduler, AutoCloseable {

    private final List<ScheduledTask> tasks = new CopyOnWriteArrayList<>();
    private final ExecutorService asyncExecutor = Executors.newVirtualThreadPerTaskExecutor();
    private long currentTick;

    @Override
    public TaskHandle sync(Runnable task) {
        return syncLater(task, 0L);
    }

    @Override
    public TaskHandle syncLater(Runnable task, long ticks) {
        var scheduled = new ScheduledTask(
                task,
                currentTick + Math.max(0L, ticks),
                -1L
        );
        tasks.add(scheduled);
        return scheduled;
    }

    @Override
    public TaskHandle syncRepeating(Runnable task, long delayTicks, long periodTicks) {
        if (periodTicks <= 0L) {
            throw new IllegalArgumentException("Repeating task period must be greater than 0");
        }

        var scheduled = new ScheduledTask(
                task,
                currentTick + Math.max(0L, delayTicks),
                periodTicks
        );
        tasks.add(scheduled);
        return scheduled;
    }

    @Override
    public CompletableFuture<Void> async(Runnable task) {
        return CompletableFuture.runAsync(task, asyncExecutor);
    }

    @Override
    public <T> CompletableFuture<T> async(Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, asyncExecutor);
    }

    public void tick() {
        currentTick++;
        tasks.forEach(task -> {
            if (task.cancelled()) {
                tasks.remove(task);
                return;
            }
            if (task.nextRunTick <= currentTick) {
                task.runnable.run();
                if (task.periodTicks <= 0L || task.cancelled()) {
                    tasks.remove(task);
                } else {
                    task.nextRunTick = currentTick + task.periodTicks;
                }
            }
        });
    }

    @Override
    public void close() {
        tasks.clear();
        asyncExecutor.shutdownNow();
    }

    private static final class ScheduledTask implements TaskHandle {

        private final Runnable runnable;
        private final long periodTicks;
        private volatile boolean cancelled;
        private long nextRunTick;

        private ScheduledTask(
                Runnable runnable,
                long nextRunTick,
                long periodTicks
        ) {
            this.runnable = runnable;
            this.nextRunTick = nextRunTick;
            this.periodTicks = periodTicks;
        }

        @Override
        public void cancel() {
            cancelled = true;
        }

        @Override
        public boolean cancelled() {
            return cancelled;
        }

    }

}
