package top.likoslupus.cellulosesz.runtime

import kotlinx.coroutines.CoroutineDispatcher
import net.minecraft.server.MinecraftServer
import kotlin.coroutines.CoroutineContext

internal class MinecraftServerDispatcher(
    private val server: MinecraftServer
) : CoroutineDispatcher() {

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        server.execute(block)
    }

}
