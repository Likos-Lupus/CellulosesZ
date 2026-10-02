package top.likoslupus.cellulosesz.administration.operator

import top.likoslupus.cellulosesz.administration.moderation.PlayerIdentity

/**
 * The only seam that may kill a player, change their game mode, or execute a command as them.
 * Version/mapping differences stay in the implementation.
 */
internal interface PlayerControlBackend {

    /** Returns false when the target is no longer online. */
    fun kill(target: PlayerIdentity): Boolean

    /** Returns false when the target is no longer online. */
    fun setGameMode(target: PlayerIdentity, mode: PlayerGameMode): Boolean

    /** Executes [command] through the target's own command source. False when offline. */
    fun executeAsPlayer(target: PlayerIdentity, command: String): Boolean

}
