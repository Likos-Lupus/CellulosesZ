package top.likoslupus.cellulosesz.administration.moderation.identity

import top.likoslupus.cellulosesz.administration.moderation.PlayerIdentity
import java.util.*

/**
 * Server-thread confined index of identities this server already knows. It is never used to resolve
 * arbitrary internet usernames: offline moderation is limited to known server identity by design.
 */
internal class KnownPlayerIndex {

    private val byId = HashMap<UUID, String>()
    private val byName = HashMap<String, UUID>()

    fun record(identity: PlayerIdentity) {
        byId[identity.id] = identity.name
        byName[identity.name.lowercase()] = identity.id
    }

    fun resolveByUUID(id: UUID): PlayerIdentity? =
        byId[id]?.let { PlayerIdentity(id, it) }

    fun resolveByName(name: String): PlayerIdentity? {
        val id = byName[name.lowercase()] ?: return null
        val resolvedName = byId[id] ?: return null
        return PlayerIdentity(id, resolvedName)
    }

    fun clear() {
        byId.clear()
        byName.clear()
    }

}
