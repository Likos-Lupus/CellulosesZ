package top.likoslupus.cellulosesz.communication.messaging

import java.util.*

/**
 * In-memory last-conversation pairing. Confined to the server thread.
 * Semantics: remembers the counterpart of the most recent successful private message.
 */
internal class ConversationState {

    private val lastPartner = HashMap<UUID, UUID>()

    fun record(first: UUID, second: UUID) {
        lastPartner[first] = second
        lastPartner[second] = first
    }

    fun partnerOf(playerId: UUID): UUID? =
        lastPartner[playerId]

    fun clear(playerId: UUID) {
        lastPartner.remove(playerId)
    }

}
