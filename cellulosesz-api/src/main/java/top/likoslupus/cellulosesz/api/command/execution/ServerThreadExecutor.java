package top.likoslupus.cellulosesz.api.command.execution;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public interface ServerThreadExecutor {

    boolean isServerThread();

    void execute(Runnable task);

    <T> CompletableFuture<T> submit(Supplier<T> task);

}
