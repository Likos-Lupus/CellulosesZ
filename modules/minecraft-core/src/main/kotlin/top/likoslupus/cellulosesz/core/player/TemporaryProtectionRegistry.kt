package top.likoslupus.cellulosesz.core.player

import java.time.Instant
import java.util.*

/**
 * Process-wide temporary damage immunity, keyed by player. Teleport invulnerability and login
 * protection both write here; the damage gate reads it. It is an object because the value is read at
 * damage time from any module and needs no injected owner; like [NicknameState] it is explicitly
 * cleared on server stop. Expired entries are dropped lazily on the next query.
 */
public object TemporaryProtectionRegistry {

    private val until = HashMap<UUID, Instant>()

    /** Grants immunity until [expiresAt]. A later call always wins over an earlier one. */
    public fun protect(playerId: UUID, expiresAt: Instant) {
        until[playerId] = expiresAt
    }

    public fun isProtected(playerId: UUID, now: Instant): Boolean {
        val expiresAt = until[playerId] ?: return false
        if (!expiresAt.isAfter(now)) {
            until.remove(playerId)
            return false
        }
        return true
    }

    public fun clear(playerId: UUID) {
        until.remove(playerId)
    }

    public fun clearAll() {
        until.clear()
    }

}
