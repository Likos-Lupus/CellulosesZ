package top.likoslupus.cellulosesz.administration.jail

import kotlinx.coroutines.CoroutineScope
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel

/**
 * The two runtime operations the jail service needs: hop onto the server thread, and launch a
 * cancellable task. It exists so the state machine can be unit-tested without a live Minecraft
 * server; production wires the [RuntimeKernel].
 */
internal interface JailRuntime {

    suspend fun <T> onServerThread(block: () -> T): T

    fun launch(block: suspend CoroutineScope.() -> Unit)

}

/** Production [JailRuntime] backed by the application's [RuntimeKernel]. */
internal class KernelJailRuntime(
    private val kernel: RuntimeKernel,
) : JailRuntime {

    override suspend fun <T> onServerThread(block: () -> T): T =
        kernel.onServerThread(block)

    override fun launch(block: suspend CoroutineScope.() -> Unit) {
        kernel.launch(block)
    }

}
