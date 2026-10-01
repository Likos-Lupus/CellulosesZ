package top.likoslupus.cellulosesz.core.runtime

import kotlinx.coroutines.CoroutineDispatcher
import net.minecraft.server.MinecraftServer
import kotlin.coroutines.CoroutineContext

/** Bridges coroutines onto the Minecraft server thread via [MinecraftServer.execute]. */
internal class MinecraftServerDispatcher(
    private val server: MinecraftServer,
) : CoroutineDispatcher() {

    override fun dispatch(
        context: CoroutineContext,
        block: Runnable
    ) {
        server.execute(block)
    }

}
