package top.likoslupus.cellulosesz.administration.moderation.identity

import top.likoslupus.cellulosesz.administration.moderation.PlayerIdentity
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel

/**
 * Resolves moderation targets without ever touching the network. Online players are resolved from
 * the server's player list; offline targets come from [KnownPlayerIndex], which is seeded from the
 * native ban list, mute records and every player seen since startup.
 *
 * Must be invoked on the server thread.
 */
internal class MinecraftAccountResolver(
    private val kernel: RuntimeKernel,
    private val known: KnownPlayerIndex,
) {

    fun resolve(rawTarget: String): PlayerIdentity? {
        val online = kernel.requireServer().playerList.getPlayerByName(rawTarget)
        if (online != null) {
            val identity = PlayerIdentity(online.uuid, online.gameProfile.name)
            known.record(identity)
            return identity
        }
        return known.resolveByName(rawTarget)
    }

    fun resolveOnline(rawTarget: String): PlayerIdentity? {
        val online = kernel.requireServer().playerList.getPlayerByName(rawTarget) ?: return null
        val identity = PlayerIdentity(online.uuid, online.gameProfile.name)
        known.record(identity)
        return identity
    }

}
