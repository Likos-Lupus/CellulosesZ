package top.likoslupus.cellulosesz.core.runtime

import kotlinx.coroutines.*
import net.minecraft.server.MinecraftServer
import java.util.concurrent.atomic.AtomicReference

/** Lifecycle states of the single application runtime. */
public enum class KernelState {

    CREATED,
    RUNNING,
    STOPPING,
    STOPPED,

}

/**
 * Owns the application's coroutine tree and the server thread boundary. One JVM server lifecycle
 * corresponds to exactly one kernel lifecycle; there is no implicit restart.
 */
public class RuntimeKernel {

    private val supervisor: CompletableJob = SupervisorJob()
    private val scope: CoroutineScope = CoroutineScope(
        supervisor + Dispatchers.Default + CoroutineName("CellulosesZ")
    )

    private val stateRef = AtomicReference(KernelState.CREATED)
    private val serverRef = AtomicReference<MinecraftServer?>(null)
    private val dispatcherRef = AtomicReference<MinecraftServerDispatcher?>(null)

    public val state: KernelState get() = stateRef.get()

    public fun onServerStarting(server: MinecraftServer) {
        serverRef.set(server)
        dispatcherRef.set(MinecraftServerDispatcher(server))
    }

    public fun onServerStarted() {
        check(stateRef.get() == KernelState.CREATED) {
            "cannot start runtime from state ${stateRef.get()}"
        }
        stateRef.set(KernelState.RUNNING)
    }

    public fun onServerStopping() {
        stateRef.set(KernelState.STOPPING)
        supervisor.cancel()
    }

    public fun onServerStopped() {
        stateRef.set(KernelState.STOPPED)
        dispatcherRef.set(null)
        serverRef.set(null)
    }

    /** Launches an IO-owned task, or returns null when the runtime is shutting down. */
    public fun launchIo(block: suspend CoroutineScope.() -> Unit): Job? {
        val current = stateRef.get()
        return if (current == KernelState.STOPPING
            || current == KernelState.STOPPED
        ) {
            null
        } else {
            scope.launch(Dispatchers.IO, block = block)
        }
    }

    public fun requireServer(): MinecraftServer =
        serverRef.get()
            ?: error("runtime has no server reference in state ${stateRef.get()}")

    public suspend fun <T> onServerThread(block: () -> T): T {
        val dispatcher = dispatcherRef.get()
            ?: error("server dispatcher unavailable in state ${stateRef.get()}")
        return withContext(dispatcher) { block() }
    }

}
