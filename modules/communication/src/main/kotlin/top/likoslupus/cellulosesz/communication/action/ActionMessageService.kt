package top.likoslupus.cellulosesz.communication.action

import com.mojang.logging.LogUtils
import top.likoslupus.cellulosesz.communication.CommunicationIntegration
import top.likoslupus.cellulosesz.communication.PlayerDirectory
import top.likoslupus.cellulosesz.communication.SendGateResult
import top.likoslupus.cellulosesz.communication.config.ActionAudience
import top.likoslupus.cellulosesz.communication.config.ActionMessageSettings
import top.likoslupus.cellulosesz.communication.format.CommunicationMessages
import top.likoslupus.cellulosesz.communication.messaging.RecipientPolicy
import top.likoslupus.cellulosesz.communication.messaging.model.MessageBody
import top.likoslupus.cellulosesz.core.runtime.ServerThreadRunner
import top.likoslupus.cellulosesz.core.text.presentation.TextPresentation
import java.util.*
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * Broadcasts literal action messages (`/me`). The audience is same-dimension by default. Abuse is
 * contained by the same mute gate private messages use, a per-sender fixed-window rate limit and a
 * length cap; user text is never parsed as formatting. Recipients the sender ignores, or that the
 * sender cannot see (for example vanished staff), are filtered out. Transient and monotonic, so it
 * resets on restart.
 */
internal class ActionMessageService(
    private val runner: ServerThreadRunner,
    private val directory: PlayerDirectory,
    private val presentation: TextPresentation,
    private val recipients: RecipientPolicy,
    private val integration: () -> CommunicationIntegration,
    private val settings: () -> ActionMessageSettings,
    private val timeSource: TimeSource = TimeSource.Monotonic,
) {

    private class Window(
        var start: TimeMark,
        var count: Int,
    )

    private val windows = HashMap<UUID, Window>()

    suspend fun emit(
        senderId: UUID?,
        senderName: String,
        dimensionId: String?,
        rawText: String,
    ): ActionMessageResult {
        val config = settings()
        if (!config.enabled) {
            return ActionMessageResult.Disabled
        }

        if (senderId != null) {
            when (val gate = integration().senderGate.check(senderId)) {
                SendGateResult.Allowed -> Unit
                is SendGateResult.Denied -> return ActionMessageResult.Muted
            }
        }

        val body = MessageBody.parse(rawText, config.maximumLength)
            ?: return ActionMessageResult.InvalidMessage

        if (senderId != null
            && !tryAcquire(senderId, config.messagesPerMinute)
        ) {
            return ActionMessageResult.RateLimited
        }

        val notice = CommunicationMessages.actionLine(senderName, body)
        val recipientCount = runner.run {
            val integration = integration()
            val audience = when {
                config.audience == ActionAudience.SAME_DIMENSION && dimensionId != null ->
                    directory.onlineIn(dimensionId)

                else -> directory.online()
            }.filter { recipient ->
                val reachable = senderId == null
                        || integration.targetReachability.canReach(senderId, recipient.id)
                val notIgnored = senderId == null
                        || !recipients.ignores(senderId, recipient.id)
                reachable && notIgnored
            }
            audience.forEach { recipient ->
                presentation.renderFor(recipient.id, notice).forEach { line ->
                    directory.send(recipient.id, line)
                }
            }
            audience.size
        }

        LOGGER.info("[Action] {}: {}", senderName, body.value)
        return ActionMessageResult.Delivered(notice, recipientCount)
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
