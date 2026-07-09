package top.likoslupus.cellulosesz.api.scheduler;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public interface Scheduler {

    TaskHandle sync(Runnable task);

    TaskHandle syncLater(Runnable task, long ticks);

    TaskHandle syncRepeating(
            Runnable task,
            long delayTicks,
            long periodTicks
    );

    CompletableFuture<Void> async(Runnable task);

    <T> CompletableFuture<T> async(Supplier<T> supplier);

}
