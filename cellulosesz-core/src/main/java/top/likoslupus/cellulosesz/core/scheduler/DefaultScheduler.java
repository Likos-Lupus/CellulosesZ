package top.likoslupus.cellulosesz.core.scheduler;

import top.likoslupus.cellulosesz.core.logging.CellulosesZLogger;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static top.likoslupus.cellulosesz.api.validation.Checks.requireNonBlank;

import static java.util.Objects.requireNonNull;

public final class DefaultScheduler implements Scheduler, AutoCloseable {

    private final List<ScheduledTask> tasks = new CopyOnWriteArrayList<>();
    private final ExecutorService asyncExecutor = Executors.newVirtualThreadPerTaskExecutor();
    private final CellulosesZLogger logger;
    private long currentTick;

    public DefaultScheduler(CellulosesZLogger logger) {
        this.logger = requireNonNull(logger, "logger");
    }

    @Override
    public TaskHandle sync(Runnable task, String owner) {
        return syncLater(task, 0L, owner);
    }

    @Override
    public TaskHandle syncLater(
            Runnable task,
            long ticks,
            String owner
    ) {
        var scheduled = new ScheduledTask(
                requireNonNull(task, "task"),
                currentTick + Math.max(0L, ticks),
                -1L,
                requireNonBlank(owner, "owner")
        );
        tasks.add(scheduled);
        return scheduled;
    }

    @Override
    public TaskHandle syncRepeating(
            Runnable task,
            long delayTicks,
            long periodTicks,
            String owner
    ) {
        if (periodTicks <= 0L) {
            throw new IllegalArgumentException("Repeating task period must be greater than 0");
        }

        var scheduled = new ScheduledTask(
                requireNonNull(task, "task"),
                currentTick + Math.max(0L, delayTicks),
                periodTicks,
                requireNonBlank(owner, "owner")
        );
        tasks.add(scheduled);
        return scheduled;
    }

    @Override
    public CompletableFuture<Void> async(Runnable task) {
        return CompletableFuture.runAsync(
                requireNonNull(task, "task"),
                asyncExecutor
        );
    }

    @Override
    public <T> CompletableFuture<T> async(Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(
                requireNonNull(supplier, "supplier"),
                asyncExecutor
        );
    }

    public void tick() {
        currentTick++;
        tasks.forEach(task -> {
            if (task.cancelled()) {
                tasks.remove(task);
                return;
            }

            if (task.nextRunTick > currentTick) {
                return;
            }

            try {
                task.runnable.run();
            } catch (Throwable failure) {
                logger.error(
                        "Scheduled task failed; other tasks will continue.",
                        failure
                );
            }

            if (task.periodTicks <= 0L || task.cancelled()) {
                task.complete();
                tasks.remove(task);
            } else {
                task.nextRunTick = currentTick + task.periodTicks;
            }
        });
    }

    @Override
    public void close() {
        tasks.forEach(TaskHandle::cancel);
        tasks.clear();
        asyncExecutor.shutdownNow();
    }

    private static final class ScheduledTask implements TaskHandle {

        private final Runnable runnable;
        private final long periodTicks;
        private final String owner;
        private final AtomicBoolean closed = new AtomicBoolean();
        private volatile boolean cancelled;
        private long nextRunTick;

        private ScheduledTask(
                Runnable runnable,
                long nextRunTick,
                long periodTicks,
                String owner
        ) {
            this.runnable = runnable;
            this.nextRunTick = nextRunTick;
            this.periodTicks = periodTicks;
            this.owner = owner;
        }

        @Override
        public String owner() {
            return owner;
        }

        @Override
        public boolean closed() {
            return closed.get();
        }

        @Override
        public boolean cancelled() {
            return cancelled;
        }

        @Override
        public void cancel() {
            cancelled = true;
            closed.set(true);
        }

        private void complete() {
            closed.set(true);
        }

    }

}
