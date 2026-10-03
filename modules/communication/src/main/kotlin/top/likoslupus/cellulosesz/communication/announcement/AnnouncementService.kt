package top.likoslupus.cellulosesz.communication.announcement

import com.mojang.logging.LogUtils
import top.likoslupus.cellulosesz.communication.PlayerDirectory
import top.likoslupus.cellulosesz.communication.config.AnnouncementSettings
import top.likoslupus.cellulosesz.communication.format.CommunicationMessages
import top.likoslupus.cellulosesz.communication.messaging.model.MessageBody
import top.likoslupus.cellulosesz.core.runtime.ServerThreadRunner

internal sealed interface AnnouncementResult {

    data class Delivered(val recipientCount: Int) : AnnouncementResult
    data object NoRecipients : AnnouncementResult
    data object Disabled : AnnouncementResult
    data object InvalidMessage : AnnouncementResult

}

/**
 * Privileged one-to-many literal announcements. No repository, no formatting language; the actor is
 * recorded in the server log rather than forced into the announcement text.
 */
internal class AnnouncementService(
    private val runner: ServerThreadRunner,
    private val directory: PlayerDirectory,
    private val settings: () -> AnnouncementSettings,
) {

    suspend fun broadcast(
        senderName: String,
        rawMessage: String,
    ): AnnouncementResult =
        deliver(senderName, rawMessage, null)

    suspend fun broadcastWorld(
        senderName: String,
        dimensionId: String,
        rawMessage: String,
    ): AnnouncementResult =
        deliver(senderName, rawMessage, dimensionId)

    private suspend fun deliver(
        senderName: String,
        rawMessage: String,
        dimensionId: String?,
    ): AnnouncementResult {
        val config = settings()
        if (!config.enabled) {
            return AnnouncementResult.Disabled
        }

        val body = MessageBody.parse(rawMessage, config.maxMessageLength)
            ?: return AnnouncementResult.InvalidMessage

        val payload = CommunicationMessages.broadcastLine(body)
        val recipientCount = runner.run {
            val recipients = when (dimensionId) {
                null -> directory.online()
                else -> directory.onlineIn(dimensionId)
            }
            recipients.forEach { directory.send(it.id, payload) }
            recipients.size
        }

        when (dimensionId) {
            null -> LOGGER.info(
                "[Broadcast] {}: {}",
                senderName,
                body.value
            )

            else -> LOGGER.info(
                "[Broadcast] {} dimension={}: {}",
                senderName,
                dimensionId,
                body.value
            )
        }

        return when {
            recipientCount == 0 -> AnnouncementResult.NoRecipients
            else -> AnnouncementResult.Delivered(recipientCount)
        }
    }

    private companion object {

        private val LOGGER = LogUtils.getLogger()

    }

}
