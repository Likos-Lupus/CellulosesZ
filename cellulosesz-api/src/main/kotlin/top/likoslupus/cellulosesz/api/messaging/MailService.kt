package top.likoslupus.cellulosesz.api.messaging

import java.util.*
import java.util.concurrent.CompletableFuture

public interface MailService {

    public fun send(message: MailMessage): CompletableFuture<Void>

    public fun sendAll(
        recipients: Collection<UUID>,
        factory: MailMessageFactory
    ): CompletableFuture<Int>

    public fun inbox(recipient: UUID): CompletableFuture<List<MailMessage>>

    public fun unreadCount(recipient: UUID): CompletableFuture<Int>

    public fun markRead(
        recipient: UUID,
        messageIds: Collection<UUID>
    ): CompletableFuture<Void>

    public fun delete(
        recipient: UUID,
        messageId: UUID
    ): CompletableFuture<Boolean>

    public fun clear(recipient: UUID): CompletableFuture<Int>

    public fun purgeExpired(now: Long): CompletableFuture<Int>

    public fun interface MailMessageFactory {

        public fun create(recipient: UUID): MailMessage

    }

}
