package top.likoslupus.cellulosesz.administration.moderation.identity

import top.likoslupus.cellulosesz.administration.moderation.PlayerIdentity
import top.likoslupus.cellulosesz.core.player.KnownPlayerResolver

/**
 * Resolves moderation targets without ever touching the network. Online players are resolved from
 * the server's player list; offline targets come from the shared [KnownPlayerResolver], which the
 * composition root seeds from the native ban list, mute records and every player seen since startup.
 *
 * Must be invoked on the server thread.
 */
internal class MinecraftAccountResolver(
    private val known: KnownPlayerResolver,
) {

    fun resolve(rawTarget: String): PlayerIdentity? =
        (known.onlineByName(rawTarget) ?: known.knownByName(rawTarget))
            ?.let { PlayerIdentity(it.id, it.name) }

    fun resolveOnline(rawTarget: String): PlayerIdentity? =
        known.onlineByName(rawTarget)?.let {
            PlayerIdentity(it.id, it.name)
        }

}
