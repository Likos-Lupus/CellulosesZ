package top.likoslupus.cellulosesz.player.presence

import kotlinx.coroutines.CancellationException
import top.likoslupus.cellulosesz.core.player.KnownPlayerIdentity
import top.likoslupus.cellulosesz.core.player.KnownPlayerResolver
import top.likoslupus.cellulosesz.core.player.identity.PersistedPlayerIdentity
import top.likoslupus.cellulosesz.core.player.identity.PlayerIdentityRepository
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.player.session.PlayerSessionService
import java.time.Clock
import java.util.*
import kotlin.time.Duration

/**
 * Resolves seen/playtime/whois queries. The in-memory [KnownPlayerResolver] is the fast path (online
 * plus the hydrated identity index); the durable identity repository supplies first/last seen and
 * covers historical names the index never loaded.
 */
internal class PlayerPresenceService(
    private val kernel: RuntimeKernel,
    private val known: KnownPlayerResolver,
    private val identities: PlayerIdentityRepository,
    private val sessions: PlayerSessionService,
    private val clock: Clock,
) {

    /** Resolves one player and gathers presence, or reports absence/storage failure. */
    suspend fun lookup(rawName: String): PresenceLookup {
        val resolved = kernel.onServerThread {
            known.onlineByName(rawName) ?: known.knownByName(rawName)
        }
        return try {
            val durable = when (resolved) {
                null -> identities.findCandidatesByName(rawName).firstOrNull()
                else -> identities.findById(resolved.id)
            }
            val identity = when {
                resolved != null -> resolved
                durable != null -> KnownPlayerIdentity(durable.id, durable.name)
                else -> return PresenceLookup.NotFound
            }
            PresenceLookup.Found(build(identity, durable))
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            PresenceLookup.StorageUnavailable
        }
    }

    /** Total play time of one player, including an in-progress session. */
    suspend fun playtime(playerId: UUID): Duration =
        sessions.playtime(playerId, clock.instant())

    private suspend fun build(
        identity: KnownPlayerIdentity,
        durable: PersistedPlayerIdentity?,
    ): PlayerPresence {
        val online = kernel.onServerThread { known.onlineById(identity.id) != null }
        val playtime = sessions.playtime(identity.id, clock.instant())
        return PlayerPresence(
            id = identity.id,
            name = identity.name,
            firstSeen = durable?.firstSeen,
            lastSeen = durable?.lastSeen,
            online = online,
            playtime = playtime,
        )
    }

}
