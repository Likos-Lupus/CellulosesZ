package top.likoslupus.cellulosesz.administration.playerstate

import top.likoslupus.cellulosesz.administration.operator.PlayerGameMode
import java.util.*

/**
 * Server-thread confined read/write seam for player-state toggles: health, invulnerability and
 * flight, plus the current dimension and game mode used by the god/fly world policy. Every method
 * returns null (or false) when the player is no longer online.
 */
internal interface PlayerStateBackend {

    fun applyHeal(playerId: UUID, removeEffects: Boolean): Boolean

    fun isInvulnerable(playerId: UUID): Boolean

    fun setInvulnerable(playerId: UUID, value: Boolean): Boolean

    fun mayFly(playerId: UUID): Boolean

    fun setFly(playerId: UUID, value: Boolean): Boolean

    fun dimension(playerId: UUID): String?

    fun gameMode(playerId: UUID): PlayerGameMode?

}
