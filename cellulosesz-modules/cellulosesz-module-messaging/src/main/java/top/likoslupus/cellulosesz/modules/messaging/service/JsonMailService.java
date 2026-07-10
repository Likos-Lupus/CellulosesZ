package top.likoslupus.cellulosesz.modules.messaging.service;

import top.likoslupus.cellulosesz.api.messaging.MailMessage;
import top.likoslupus.cellulosesz.api.messaging.MailService;
import top.likoslupus.cellulosesz.api.storage.StorageService;
import top.likoslupus.cellulosesz.modules.messaging.MessagingConfig;
import top.likoslupus.cellulosesz.modules.messaging.data.MailDocument;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class JsonMailService implements MailService {

    private final StorageService storage;
    private final MessagingConfig config;
    private final Path directory;

    public JsonMailService(
            StorageService storage,
            MessagingConfig config,
            Path directory
    ) {
        this.storage = storage;
        this.config = config;
        this.directory = directory;
    }

    @Override
    public CompletableFuture<Void> send(UUID recipient, MailMessage message) {
        return load(recipient).thenCompose(document -> {
            document.messages.add(message);
            while (document.messages.size() > Math.max(1, config.maxMailPerPlayer)) {
                document.messages.removeFirst();
            }
            return save(recipient, document);
        });
    }

    @Override
    public CompletableFuture<List<MailMessage>> inbox(UUID recipient) {
        return load(recipient).thenApply(document -> List.copyOf(document.messages));
    }

    @Override
    public CompletableFuture<Void> markRead(UUID recipient) {
        return load(recipient).thenCompose(document -> {
            document.messages.forEach(message -> message.read = true);
            return save(recipient, document);
        });
    }

    @Override
    public CompletableFuture<Void> clear(UUID recipient) {
        var document = new MailDocument();
        return save(recipient, document);
    }

    private CompletableFuture<MailDocument> load(UUID recipient) {
        return storage.load(path(recipient), MailDocument.class, MailDocument::new);
    }

    private CompletableFuture<Void> save(UUID recipient, MailDocument document) {
        return storage.save(path(recipient), document);
    }

    private Path path(UUID recipient) {
        return directory.resolve(recipient + ".json");
    }

}
