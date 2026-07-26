package top.likoslupus.cellulosesz.api.messaging;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface MailService {

    CompletableFuture<Void> send(MailMessage message);

    CompletableFuture<Integer> sendAll(Collection<UUID> recipients, MailMessageFactory factory);

    CompletableFuture<List<MailMessage>> inbox(UUID recipient);

    CompletableFuture<Integer> unreadCount(UUID recipient);

    CompletableFuture<Void> markRead(UUID recipient, Collection<UUID> messageIds);

    CompletableFuture<Boolean> delete(UUID recipient, UUID messageId);

    CompletableFuture<Integer> clear(UUID recipient);

    CompletableFuture<Integer> purgeExpired(long now);

    @FunctionalInterface
    interface MailMessageFactory {

        MailMessage create(UUID recipient);

    }

}
