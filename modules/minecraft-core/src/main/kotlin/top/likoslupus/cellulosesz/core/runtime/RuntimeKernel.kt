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
 * Owns the application's coroutine tree and the server thread boundary. A dedicated server runs one
 * session per JVM, but an integrated client can start, stop and start a new server in the same JVM
 * (for example after leaving and reopening a world). [onServerStarting] therefore opens a fresh
 * session: once the previous one has stopped, the coroutine tree and state are rebuilt so the kernel
 * is reusable. Between sessions there is no live server reference and [launch] is inert.
 */
public class RuntimeKernel {

    @Volatile private var supervisor: CompletableJob = SupervisorJob()

    @Volatile private var scope: CoroutineScope = CoroutineScope(
        supervisor + Dispatchers.Default + CoroutineName("CellulosesZ")
    )

    private val stateRef = AtomicReference(KernelState.CREATED)
    private val serverRef = AtomicReference<MinecraftServer?>(null)
    private val dispatcherRef = AtomicReference<MinecraftServerDispatcher?>(null)

    public val state: KernelState get() = stateRef.get()

    public fun onServerStarting(server: MinecraftServer) {
        if (stateRef.get() == KernelState.STOPPED) {
            supervisor = SupervisorJob()
            scope = CoroutineScope(supervisor + Dispatchers.Default + CoroutineName("CellulosesZ"))
            stateRef.set(KernelState.CREATED)
        }
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

    /**
     * Launches an orchestration task on the runtime's default dispatcher, or returns null when the
     * runtime is shutting down. File access must switch to [Dispatchers.IO] inside the repository;
     * use [launchIo] only for tasks that are themselves IO-bound start to finish.
     */
    public fun launch(block: suspend CoroutineScope.() -> Unit): Job? {
        val current = stateRef.get()
        return if (current == KernelState.STOPPING
            || current == KernelState.STOPPED
        ) {
            null
        } else {
            scope.launch(block = block)
        }
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
