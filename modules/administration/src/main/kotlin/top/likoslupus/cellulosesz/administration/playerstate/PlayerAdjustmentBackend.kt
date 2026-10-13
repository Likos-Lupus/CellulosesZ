package top.likoslupus.cellulosesz.administration.playerstate

import java.util.*

/** A snapshot of a player's experience state, read on the server thread. */
internal data class ExperienceSnapshot(
    val level: Int,
    val total: Int,
    val needed: Int,
)

/**
 * Server-thread confined read/write seam for the experience and speed of online players. Every
 * method returns null when the player is no longer online.
 */
internal interface PlayerAdjustmentBackend : SpeedAttributeAccess {

    fun experience(playerId: UUID): ExperienceSnapshot?

    /** Applies [delta] to the running total and returns the resulting state. */
    fun applyExperienceDelta(playerId: UUID, delta: Int): ExperienceSnapshot?

}
