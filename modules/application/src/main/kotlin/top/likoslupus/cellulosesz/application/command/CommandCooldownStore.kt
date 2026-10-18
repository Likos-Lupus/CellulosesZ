package top.likoslupus.cellulosesz.application.command

import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Per-player command cooldowns. Reads are a server-thread map lookup; a write updates the map and,
 * when persistence is enabled, upserts one row off-thread. Rows are disposable: an expired row is
 * pruned at startup and ignored on read.
 */
internal class CommandCooldownStore(
    private val repository: JdbcCommandCooldownRepository,
) {

    private val expiries = ConcurrentHashMap<UUID, MutableMap<String, Long>>()

    /** Loads unexpired rows once during server start. A read failure yields an empty cache. */
    fun hydrateBlocking(now: Long = System.currentTimeMillis()) {
        expiries.clear()
        repository.loadAllBlocking(now).forEach { (playerId, commands) ->
            expiries[playerId] = ConcurrentHashMap(commands)
        }
    }

    fun remainingSeconds(
        playerId: UUID,
        command: String,
        now: Long
    ): Long {
        val byCommand = expiries[playerId] ?: return 0
        val expiresAt = byCommand[command] ?: return 0
        if (expiresAt <= now) {
            byCommand.remove(command)
            return 0
        }
        return ((expiresAt - now) + 999) / 1000
    }

    fun mark(
        playerId: UUID,
        command: String,
        expiresAt: Long
    ) {
        expiries.computeIfAbsent(playerId) { ConcurrentHashMap() }[command] = expiresAt
    }

    suspend fun persist(
        playerId: UUID,
        command: String,
        expiresAt: Long
    ) {
        repository.upsert(playerId, command, expiresAt)
    }

    fun clearPlayer(playerId: UUID) {
        expiries.remove(playerId)
    }

}
