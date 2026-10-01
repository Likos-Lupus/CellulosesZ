package top.likoslupus.cellulosesz.runtime

import kotlinx.coroutines.*
import net.minecraft.server.MinecraftServer
import java.util.concurrent.atomic.AtomicReference

internal enum class KernelState {

    CREATED,
    RUNNING,
    STOPPING,
    STOPPED,

}

internal class RuntimeKernel {

    private val supervisor: CompletableJob = SupervisorJob()
    private val scope: CoroutineScope = CoroutineScope(
        supervisor + Dispatchers.Default + CoroutineName("CellulosesZ")
    )

    private val stateRef = AtomicReference(KernelState.CREATED)
    private val serverRef = AtomicReference<MinecraftServer?>(null)
    private val dispatcherRef = AtomicReference<MinecraftServerDispatcher?>(null)

    val state: KernelState get() = stateRef.get()

    fun onServerStarting(server: MinecraftServer) {
        serverRef.set(server)
        dispatcherRef.set(MinecraftServerDispatcher(server))
    }

    fun onServerStarted() {
        check(stateRef.get() == KernelState.CREATED) {
            "cannot start runtime from state ${stateRef.get()}"
        }
        stateRef.set(KernelState.RUNNING)
    }

    fun onServerStopping() {
        stateRef.set(KernelState.STOPPING)
        supervisor.cancel()
    }

    fun onServerStopped() {
        stateRef.set(KernelState.STOPPED)
        dispatcherRef.set(null)
        serverRef.set(null)
    }

    fun launchIo(block: suspend CoroutineScope.() -> Unit): Job? {
        val current = stateRef.get()
        return if (current == KernelState.STOPPING
            || current == KernelState.STOPPED
        ) {
            null
        } else {
            scope.launch(Dispatchers.IO, block = block)
        }
    }

    fun requireServer(): MinecraftServer =
        serverRef.get() ?: error("runtime has no server reference in state ${stateRef.get()}")

    suspend fun <T> onServerThread(block: () -> T): T {
        val dispatcher = dispatcherRef.get()
            ?: error("server dispatcher unavailable in state ${stateRef.get()}")
        return withContext(dispatcher) { block() }
    }

}
