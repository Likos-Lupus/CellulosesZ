package top.likoslupus.cellulosesz.communication

import net.minecraft.network.chat.Component
import top.likoslupus.cellulosesz.communication.messaging.OnlinePlayerIdentity
import java.util.*

/**
 * Read-only view of online players plus message delivery, used by staff help and announcements.
 * Server-thread confined; the message services hop through a runner.
 */
internal interface PlayerDirectory {

    fun online(): List<OnlinePlayerIdentity>

    fun moderators(): List<OnlinePlayerIdentity>

    fun onlineIn(dimensionId: String): List<OnlinePlayerIdentity>

    fun send(playerId: UUID, message: Component): Boolean

}
