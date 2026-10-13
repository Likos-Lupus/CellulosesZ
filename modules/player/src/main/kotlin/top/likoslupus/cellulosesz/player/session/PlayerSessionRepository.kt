package top.likoslupus.cellulosesz.player.session

import java.time.Instant
import java.util.*

/**
 * Durable play sessions in `cz_player_sessions`. It owns all session SQL; callers stay on immutable
 * values. Every write is keyed by `session_uuid` and idempotent, so a retry never double-counts.
 */
internal interface PlayerSessionRepository {

    /** Inserts a new session; returns false when the session already exists. */
    suspend fun begin(session: PlayerSession): Boolean

    /** Advances the heartbeat of an open session; returns false when it is already closed. */
    suspend fun heartbeat(sessionId: UUID, at: Instant): Boolean

    /** Closes an open session; returns false when it is already closed. */
    suspend fun close(sessionId: UUID, at: Instant, kind: SessionCloseKind): Boolean

    /** Closes every open session; used for a bounded, blocking shutdown. */
    fun closeAllOpenBlocking(at: Instant, kind: SessionCloseKind): Int

    /** Closes sessions left open by a previous process, ending them at their last heartbeat. */
    fun recoverOpenBlocking(): Int

    /** Loads every session of one player, newest first. */
    suspend fun loadSessions(playerId: UUID): List<PlayerSession>

}
