package top.likoslupus.cellulosesz.communication.mail

import com.mojang.logging.LogUtils
import kotlinx.coroutines.CancellationException
import net.minecraft.network.chat.Component
import top.likoslupus.cellulosesz.communication.PrivateMessageSenderGate
import top.likoslupus.cellulosesz.communication.SendGateResult
import top.likoslupus.cellulosesz.communication.config.MailSettings
import top.likoslupus.cellulosesz.communication.messaging.model.MessageBody
import top.likoslupus.cellulosesz.communication.preferences.MessagingPreferencesRepository
import top.likoslupus.cellulosesz.core.player.KnownPlayerIdentity
import top.likoslupus.cellulosesz.core.player.KnownPlayerResolver
import top.likoslupus.cellulosesz.core.runtime.ServerThreadRunner
import top.likoslupus.cellulosesz.foundation.persistence.KeyedMutex
import java.time.Clock
import java.time.Duration
import java.util.*

internal sealed interface MailSendResult {

    data class Sent(val target: KnownPlayerIdentity) : MailSendResult
    data object Disabled : MailSendResult
    data object InvalidMessage : MailSendResult
    data class SenderRestricted(val feedback: Component) : MailSendResult
    data class TargetUnknown(val name: String) : MailSendResult
    data object SelfTarget : MailSendResult
    data object RateLimited : MailSendResult
    data object MailboxFull : MailSendResult
    data object DurationTooLong : MailSendResult
    data object StorageUnavailable : MailSendResult

}

internal sealed interface MailSummaryResult {

    data class Summary(val total: Int, val unread: Int) : MailSummaryResult
    data object StorageUnavailable : MailSummaryResult

}

internal sealed interface MailViewResult {

    data class Loaded(
        val page: List<MailMessage>,
        val pageIndex: Int,
        val pageCount: Int,
        val total: Int,
        val unread: Int,
    ) : MailViewResult

    data object Empty : MailViewResult
    data object StorageUnavailable : MailViewResult

}

internal sealed interface MailClearResult {

    data object Cleared : MailClearResult
    data object StorageUnavailable : MailClearResult

}

/**
 * Durable, offline-capable mail. Expiry uses wall-clock [Clock] because mail survives restarts;
 * transient rate limiting uses monotonic time. Recipient mailboxes are serialized per owner through
 * a keyed lock so concurrent sends never lose a message.
 */
internal class MailService(
    private val runner: ServerThreadRunner,
    private val mailboxes: MailboxRepository,
    private val preferences: MessagingPreferencesRepository,
    private val identities: KnownPlayerResolver,
    private val senderGate: () -> PrivateMessageSenderGate,
    private val rateLimiter: MailRateLimiter,
    private val clock: Clock,
    private val settings: () -> MailSettings,
    private val locks: KeyedMutex<UUID>,
) {

    suspend fun send(
        sender: MailSender,
        rawTarget: String,
        rawBody: String,
        duration: Duration? = null,
    ): MailSendResult {
        val config = settings()
        if (!config.enabled) {
            return MailSendResult.Disabled
        }

        val body = MessageBody.parse(rawBody, config.maxMessageLength)
            ?: return MailSendResult.InvalidMessage

        val maxTemporarySeconds = config.maxTemporaryMailSeconds
        if (duration != null
            && maxTemporarySeconds != null
            && duration.seconds > maxTemporarySeconds
        ) {
            return MailSendResult.DurationTooLong
        }

        if (sender is MailSender.Player) {
            when (val gate = senderGate().check(sender.id)) {
                SendGateResult.Allowed -> Unit
                is SendGateResult.Denied -> return MailSendResult.SenderRestricted(gate.feedback)
            }
        }

        val target = runner.run {
            identities.onlineByName(rawTarget) ?: identities.knownByName(rawTarget)
        } ?: return MailSendResult.TargetUnknown(rawTarget)

        if (sender is MailSender.Player
            && sender.id == target.id
            ) {
            return MailSendResult.SelfTarget
        }

        if (sender is MailSender.Player) {
            val allowed = runner.run {
                rateLimiter.tryAcquire(
                    sender.id,
                    config.maxSendsPerMinute
                )
            }
            if (!allowed) {
                return MailSendResult.RateLimited
            }
        }

        if (sender is MailSender.Player
            && ignores(sender.id, target.id)
            ) {
            // Privacy: never let a sender reliably probe another player's ignore list.
            return MailSendResult.Sent(target)
        }

        return locks.withLock(target.id) {
            persistSend(sender, target, body, config, duration)
        }
    }

    suspend fun summary(owner: KnownPlayerIdentity): MailSummaryResult =
        locks.withLock(owner.id) {
            val now = clock.instant()
            val mailbox = loadOrDefault(owner)
                ?: return@withLock MailSummaryResult.StorageUnavailable
            val pruned = mailbox.pruneExpired(now)
            if (pruned !== mailbox) {
                saveQuietly(pruned)
            }
            MailSummaryResult.Summary(
                pruned.messages.size,
                pruned.unreadCount
            )
        }

    suspend fun read(
        owner: KnownPlayerIdentity,
        requestedPage: Int,
    ): MailViewResult =
        locks.withLock(owner.id) {
            val now = clock.instant()
            val mailbox = loadOrDefault(owner)
                ?: return@withLock MailViewResult.StorageUnavailable
            val pruned = mailbox.pruneExpired(now)
            val ordered = pruned.newestFirst()
            if (ordered.isEmpty()) {
                if (pruned !== mailbox) {
                    saveQuietly(pruned)
                }
                return@withLock MailViewResult.Empty
            }

            val pageCount = ((ordered.size + PAGE_SIZE - 1) / PAGE_SIZE).coerceAtLeast(1)
            val pageIndex = requestedPage.coerceIn(1, pageCount)
            val from = (pageIndex - 1) * PAGE_SIZE
            val to = minOf(from + PAGE_SIZE, ordered.size)
            val slice = ordered.subList(from, to)

            val displayedUnread = slice.filter { it.readAt == null }.map { it.id }.toSet()
            val updated = pruned.markRead(displayedUnread, now)
            if (!save(updated)) {
                return@withLock MailViewResult.StorageUnavailable
            }

            MailViewResult.Loaded(
                page = slice,
                pageIndex = pageIndex,
                pageCount = pageCount,
                total = updated.messages.size,
                unread = updated.unreadCount,
            )
        }

    suspend fun clear(owner: KnownPlayerIdentity): MailClearResult =
        locks.withLock(owner.id) {
            val mailbox = loadOrDefault(owner)
                ?: return@withLock MailClearResult.StorageUnavailable
            if (!save(mailbox.clear())) {
                return@withLock MailClearResult.StorageUnavailable
            }
            MailClearResult.Cleared
        }

    private suspend fun persistSend(
        sender: MailSender,
        target: KnownPlayerIdentity,
        body: MessageBody,
        config: MailSettings,
        duration: Duration?,
    ): MailSendResult {
        val now = clock.instant()
        val mailbox = loadOrDefault(target)
            ?: return MailSendResult.StorageUnavailable
        val pruned = mailbox.pruneExpired(now)

        if (pruned.messages.size >= config.maxMessagesPerMailbox) {
            if (pruned !== mailbox) {
                saveQuietly(pruned)
            }
            return MailSendResult.MailboxFull
        }

        val message = MailMessage(
            id = UUID.randomUUID(),
            sender = sender,
            body = body,
            sentAt = now,
            expiresAt = duration?.let { now.plus(it) },
            readAt = null,
        )
        if (!save(pruned + message)) {
            return MailSendResult.StorageUnavailable
        }
        return MailSendResult.Sent(target)
    }

    private suspend fun ignores(
        senderId: UUID,
        targetId: UUID,
    ): Boolean =
        try {
            preferences.load(targetId)
                    ?.contains(senderId) == true
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            LOGGER.error(
                "failed to read mail recipient preferences for {}",
                targetId,
                exception,
            )
            false
        }

    private suspend fun loadOrDefault(owner: KnownPlayerIdentity): Mailbox? =
        try {
            mailboxes.load(owner.id)
                ?: Mailbox.empty(owner)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            LOGGER.error(
                "failed to load mailbox for {}",
                owner.id,
                exception,
            )
            null
        }

    private suspend fun save(mailbox: Mailbox): Boolean =
        try {
            mailboxes.save(mailbox)
            true
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            LOGGER.error(
                "failed to persist mailbox for {}",
                mailbox.ownerId,
                exception,
            )
            false
        }

    private suspend fun saveQuietly(mailbox: Mailbox) {
        try {
            mailboxes.save(mailbox)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            LOGGER.warn(
                "failed to store pruned mailbox for {}",
                mailbox.ownerId,
                exception,
            )
        }
    }

    private companion object {

        private const val PAGE_SIZE: Int = 10
        private val LOGGER = LogUtils.getLogger()

    }

}
