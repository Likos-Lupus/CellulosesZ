package top.likoslupus.cellulosesz.common.command;

import org.junit.jupiter.api.Test;
import top.likoslupus.cellulosesz.common.lifecycle.MinecraftServerHandle;

import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

final class MinecraftServerThreadExecutorTest {

    @Test
    void execute_whenServerNotAttached_failsFast() {
        var handle = new MinecraftServerHandle();
        var executor = new MinecraftServerThreadExecutor(handle);

        assertFalse(executor.isServerThread());
        assertThrows(
                IllegalStateException.class, () -> executor.execute(() -> {
                })
        );
        var future = executor.submit(() -> "result");
        assertTrue(future.isCompletedExceptionally());
        assertThrows(ExecutionException.class, future::get);
    }

}
