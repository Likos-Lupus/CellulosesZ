package top.likoslupus.cellulosesz.common.command;

import net.minecraft.util.thread.BlockableEventLoop;
import top.likoslupus.cellulosesz.api.command.execution.ServerThreadExecutor;
import top.likoslupus.cellulosesz.common.lifecycle.MinecraftServerHandle;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public final class MinecraftServerThreadExecutor implements ServerThreadExecutor {

    private final MinecraftServerHandle server;

    public MinecraftServerThreadExecutor(MinecraftServerHandle server) {
        this.server = requireNonNull(server, "server");
    }

    @Override
    public boolean isServerThread() {
        return server.current()
                .map(BlockableEventLoop::isSameThread)
                .orElse(false);
    }

    @Override
    public void execute(Runnable task) {
        var current = server.requireRunning();
        var checked = requireNonNull(task, "task");

        if (current.isSameThread()) {
            checked.run();
        } else {
            current.execute(checked);
        }
    }

    @Override
    public <T> CompletableFuture<T> submit(Supplier<T> task) {
        var future = new CompletableFuture<T>();
        try {
            execute(() -> {
                try {
                    future.complete(requireNonNull(task, "task").get());
                } catch (Throwable failure) {
                    future.completeExceptionally(failure);
                }
            });
        } catch (RuntimeException failure) {
            future.completeExceptionally(failure);
        }
        return future;
    }

}
