package top.likoslupus.cellulosesz.communication.messaging

import net.minecraft.network.chat.Component
import java.util.*

/** A minimal seam over the server's player list and player message delivery. Server-thread only. */
internal data class OnlinePlayerIdentity(
    val id: UUID,
    val name: String,
)

internal interface MessagingBackend {

    fun onlineIdentityByName(name: String): OnlinePlayerIdentity?

    fun onlineIdentityById(id: UUID): OnlinePlayerIdentity?

    fun deliverPrivateMessage(playerId: UUID, message: Component): Boolean

}
