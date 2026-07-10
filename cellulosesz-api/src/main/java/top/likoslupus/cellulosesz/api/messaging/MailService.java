package top.likoslupus.cellulosesz.api.messaging;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface MailService {

    CompletableFuture<Void> send(UUID recipient, MailMessage message);

    CompletableFuture<List<MailMessage>> inbox(UUID recipient);

    CompletableFuture<Void> markRead(UUID recipient);

    CompletableFuture<Void> clear(UUID recipient);

}
