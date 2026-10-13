package top.likoslupus.cellulosesz.administration.jail

import top.likoslupus.cellulosesz.core.player.WorldPosition
import java.util.*

/**
 * Reads a live player's current position so a jail sentence can record where to return them. Must be
 * invoked on the server thread; returns `null` when the player is offline.
 */
internal fun interface JailPlayerPositionSource {

    fun position(playerId: UUID): WorldPosition?

}
