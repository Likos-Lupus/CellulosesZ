package top.likoslupus.cellulosesz.core.scheduler;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public interface Scheduler {

    default TaskHandle sync(Runnable task) {
        return sync(task, "global");
    }

    TaskHandle sync(Runnable task, String owner);

    default TaskHandle syncLater(Runnable task, long ticks) {
        return syncLater(task, ticks, "global");
    }

    TaskHandle syncLater(
            Runnable task,
            long ticks,
            String owner
    );

    default TaskHandle syncRepeating(
            Runnable task,
            long delayTicks,
            long periodTicks
    ) {
        return syncRepeating(task, delayTicks, periodTicks, "global");
    }

    TaskHandle syncRepeating(
            Runnable task,
            long delayTicks,
            long periodTicks,
            String owner
    );

    CompletableFuture<Void> async(Runnable task);

    <T> CompletableFuture<T> async(Supplier<T> supplier);

}
