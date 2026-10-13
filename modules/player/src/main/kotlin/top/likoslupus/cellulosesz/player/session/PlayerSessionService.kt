package top.likoslupus.cellulosesz.player.session

import com.mojang.logging.LogUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.foundation.persistence.KeyedMutex
import top.likoslupus.cellulosesz.player.PlayerDataException
import top.likoslupus.cellulosesz.player.config.SessionSettings
import java.time.Clock
import java.time.Instant
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Owns play-session lifecycle and playtime. The in-memory open-session map is confined to the server
 * thread; persistence is serialized per player and every write is idempotent by session uuid. A
 * heartbeat coroutine keeps open sessions fresh so a crash can be recovered from the last heartbeat.
 */
internal class PlayerSessionService(
    private val kernel: RuntimeKernel,
    private val repository: PlayerSessionRepository,
    private val settings: () -> SessionSettings,
    private val clock: Clock,
) {

    private val open = HashMap<UUID, UUID>()
    private val locks = KeyedMutex<UUID>()

    /** Blocking recovery of sessions left open by a previous process, then start heartbeats. */
    fun onServerStarting() {
        open.clear()
        try {
            repository.recoverOpenBlocking()
        } catch (exception: PlayerDataException) {
            LOGGER.error("failed to recover open player sessions", exception)
        }
        startHeartbeatLoop()
    }

    /** Server-thread confined: begin a fresh session for a joining player. */
    fun begin(playerId: UUID) {
        if (!settings().enabled) {
            return
        }
        val now = clock.instant()
        val sessionId = UUID.randomUUID()
        open[playerId] = sessionId
        val session = PlayerSession(
            sessionId = sessionId,
            playerId = playerId,
            loginAt = now,
            lastHeartbeatAt = now,
            closedAt = null,
            closeKind = null,
        )
        kernel.launch {
            try {
                locks.withLock(playerId) { repository.begin(session) }
            } catch (exception: PlayerDataException) {
                LOGGER.error("failed to begin session for {}", playerId, exception)
            }
        }
    }

    /** Server-thread confined: close the open session for a leaving player. */
    fun end(playerId: UUID, kind: SessionCloseKind) {
        val sessionId = open.remove(playerId) ?: return
        val now = clock.instant()
        kernel.launch {
            try {
                locks.withLock(playerId) { repository.close(sessionId, now, kind) }
            } catch (exception: PlayerDataException) {
                LOGGER.error("failed to close session for {}", playerId, exception)
            }
        }
    }

    /**
     * Bounded blocking shutdown. Closes every still-open session before the composition root closes
     * the database, so a clean stop never leaves a session to be recovered as a crash.
     */
    fun finishAllBlocking() {
        open.clear()
        try {
            repository.closeAllOpenBlocking(
                clock.instant(),
                SessionCloseKind.SERVER_STOP
            )
        } catch (exception: PlayerDataException) {
            LOGGER.error("failed to close open player sessions", exception)
        }
    }

    /** Total play time of one player, including an in-progress session. */
    suspend fun playtime(playerId: UUID, now: Instant): Duration {
        val sessions = repository.loadSessions(playerId)
        return sessions.fold(Duration.ZERO) { total, session ->
            total + session.durationAt(now)
        }
    }

    private fun startHeartbeatLoop() {
        kernel.launch {
            while (isActive) {
                val seconds = settings().heartbeatIntervalSeconds.coerceAtLeast(1)
                delay((seconds * 1000L).milliseconds)
                heartbeatAll()
            }
        }
    }

    private suspend fun heartbeatAll() {
        val snapshot = kernel.onServerThread { open.toMap() }
        if (snapshot.isEmpty()) {
            return
        }
        val now = clock.instant()
        snapshot.forEach { (playerId, sessionId) ->
            try {
                locks.withLock(playerId) { repository.heartbeat(sessionId, now) }
            } catch (exception: PlayerDataException) {
                LOGGER.warn("failed to heartbeat session for {}", playerId, exception)
            }
        }
    }

    private companion object {

        private val LOGGER = LogUtils.getLogger()

    }

}
