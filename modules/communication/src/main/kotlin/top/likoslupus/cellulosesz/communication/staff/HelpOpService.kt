package top.likoslupus.cellulosesz.communication.staff

import com.mojang.logging.LogUtils
import top.likoslupus.cellulosesz.communication.PlayerDirectory
import top.likoslupus.cellulosesz.communication.config.HelpOpSettings
import top.likoslupus.cellulosesz.communication.format.CommunicationMessages
import top.likoslupus.cellulosesz.communication.messaging.model.MessageBody
import top.likoslupus.cellulosesz.core.runtime.ServerThreadRunner
import java.util.*
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeMark
import kotlin.time.TimeSource

internal sealed interface HelpOpResult {

    data class Delivered(val recipientCount: Int) : HelpOpResult
    data object LoggedOnly : HelpOpResult
    data object Disabled : HelpOpResult
    data object InvalidMessage : HelpOpResult
    data object RateLimited : HelpOpResult

}

/**
 * Staff support channel. It deliberately does not use the private-message sender gate: a muted
 * player may still appeal or ask staff for help. Abuse is contained by rate limiting, a length cap,
 * and server logging.
 */
internal class HelpOpService(
    private val runner: ServerThreadRunner,
    private val directory: PlayerDirectory,
    private val settings: () -> HelpOpSettings,
    private val timeSource: TimeSource = TimeSource.Monotonic,
) {

    private class Window(
        var start: TimeMark,
        var count: Int,
    )

    private val windows = HashMap<UUID, Window>()

    suspend fun submit(
        senderId: UUID?,
        senderName: String,
        rawMessage: String,
    ): HelpOpResult {
        val config = settings()
        if (!config.enabled) {
            return HelpOpResult.Disabled
        }

        val body = MessageBody.parse(rawMessage, config.maxMessageLength)
            ?: return HelpOpResult.InvalidMessage

        if (senderId != null
            && !tryAcquire(senderId, config.maxMessagesPerMinute)
        ) {
            return HelpOpResult.RateLimited
        }

        val payload = CommunicationMessages.helpOpIncoming(senderName, body)
        val recipientCount = runner.run {
            val recipients = directory.moderators()
            recipients.forEach { directory.send(it.id, payload) }
            recipients.size
        }

        LOGGER.info(
            "[HelpOp] {}: {}",
            senderName,
            body.value,
        )
        return when {
            recipientCount == 0 -> HelpOpResult.LoggedOnly
            else -> HelpOpResult.Delivered(recipientCount)
        }
    }

    private fun tryAcquire(
        senderId: UUID,
        limitPerMinute: Int,
    ): Boolean {
        if (limitPerMinute <= 0) {
            return false
        }

        val now = timeSource.markNow()
        val window = windows.getOrPut(senderId) { Window(now, 0) }
        if (window.start.elapsedNow() >= 1.minutes) {
            window.start = now
            window.count = 0
        }
        if (window.count >= limitPerMinute) {
            return false
        }

        window.count++
        return true
    }

    private companion object {

        private val LOGGER = LogUtils.getLogger()

    }

}
