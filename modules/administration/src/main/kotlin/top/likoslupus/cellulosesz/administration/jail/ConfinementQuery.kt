package top.likoslupus.cellulosesz.administration.jail

import java.util.*

/**
 * Cross-module port exposing whether a player is currently confined. Movement consults an adapted
 * copy of this from its teleport path so a jailed player cannot teleport out.
 */
public fun interface ConfinementQuery {

    public fun isConfined(playerId: UUID): Boolean

}
