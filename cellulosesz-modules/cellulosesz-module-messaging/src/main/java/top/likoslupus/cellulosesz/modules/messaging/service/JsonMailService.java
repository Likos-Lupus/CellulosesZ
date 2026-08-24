package top.likoslupus.cellulosesz.modules.messaging.service;

import top.likoslupus.cellulosesz.api.messaging.MailMessage;
import top.likoslupus.cellulosesz.api.messaging.MailService;
import top.likoslupus.cellulosesz.core.lifecycle.legacy.AsyncInitializable;
import top.likoslupus.cellulosesz.core.storage.StorageService;
import top.likoslupus.cellulosesz.modules.messaging.MessagingConfig;
import top.likoslupus.cellulosesz.modules.messaging.persistence.MailDocument;
import top.likoslupus.cellulosesz.modules.messaging.persistence.MailMapper;
import top.likoslupus.cellulosesz.modules.messaging.persistence.MailMessageDocument;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static java.util.Objects.requireNonNull;

public final class JsonMailService implements MailService, AsyncInitializable {

    private final StorageService storage;
    private final Path path;
    private final Object queueLock = new Object();
    private MailDocument document;
    private CompletableFuture<Boolean> tail = CompletableFuture.completedFuture(Boolean.TRUE);
    private volatile MessagingConfig config;

    public JsonMailService(
            StorageService storage,
            MessagingConfig config,
            Path path
    ) {
        this.storage = requireNonNull(storage, "storage");
        this.config = requireNonNull(config, "config").validatedCopy();
        this.path = requireNonNull(path, "path");
        this.document = new MailDocument();
    }

    @Override
    public CompletableFuture<Void> initialize() {
        return storage.createIfMissing(
                        path,
                        MailDocument.class,
                        MailDocument::new
                )
                .thenApply(JsonMailService::validate)
                .thenAccept(loaded -> {
                    synchronized (queueLock) {
                        document = loaded;
                    }
                });
    }

    private static MailDocument validate(MailDocument document) {
        requireNonNull(document, "document");
        requireNonNull(document.inboxes, "inboxes");

        document.inboxes.forEach((recipient, messages) -> {
            var uuid = UUID.fromString(recipient);
            requireNonNull(messages, "messages");

            messages.forEach(message -> {
                var domain = MailMapper.toDomain(requireNonNull(message, "message"));
                if (!domain.toUuid().equals(uuid)) {
                    throw new IllegalArgumentException("Mail recipient mismatch");
                }
            });
        });

        return document;
    }

    public void configure(MessagingConfig config) {
        this.config = requireNonNull(config, "config").validatedCopy();
    }

    @Override
    public CompletableFuture<Void> send(MailMessage message) {
        requireNonNull(message, "message");
        return mutate(() -> {
            add(message);
            return Boolean.TRUE;
        }).thenAccept(_ -> {
        });
    }

    @Override
    public CompletableFuture<Integer> sendAll(
            Collection<UUID> recipients,
            MailMessageFactory factory
    ) {
        requireNonNull(recipients, "recipients");
        requireNonNull(factory, "factory");

        var unique = new LinkedHashSet<>(recipients);
        return mutate(() -> {
            unique.forEach(recipient ->
                    add(factory.create(requireNonNull(recipient, "recipient")))
            );
            return unique.size();
        });
    }

    @Override
    public CompletableFuture<List<MailMessage>> inbox(UUID recipient) {
        requireNonNull(recipient, "recipient");

        return mutate(
                () -> {
                    purgeExpiredInternal(System.currentTimeMillis());
                    return document.inboxes
                            .getOrDefault(
                                    recipient.toString(),
                                    List.of()
                            ).stream()
                            .map(MailMapper::toDomain)
                            .sorted(Comparator.comparingLong(MailMessage::sentAt).reversed())
                            .toList();
                },
                false
        );
    }

    @Override
    public CompletableFuture<Integer> unreadCount(UUID recipient) {
        requireNonNull(recipient, "recipient");

        return mutate(
                () -> {
                    purgeExpiredInternal(System.currentTimeMillis());
                    return (int) document.inboxes
                            .getOrDefault(
                                    recipient.toString(),
                                    List.of()
                            ).stream()
                            .map(MailMapper::toDomain)
                            .filter(message -> !message.read())
                            .count();
                },
                false
        );
    }

    @Override
    public CompletableFuture<Void> markRead(UUID recipient, Collection<UUID> messageIds) {
        requireNonNull(recipient, "recipient");

        var ids = new LinkedHashSet<>(requireNonNull(messageIds, "messageIds"));
        return mutate(() -> {
            var messages = inboxMutable(recipient);
            IntStream.range(0, messages.size())
                    .forEach(index -> {
                        var persisted = messages.get(index);
                        var message = MailMapper.toDomain(persisted);
                        if (ids.contains(message.id()) && !message.read()) {
                            messages.set(index, MailMapper.fromDomain(message.withRead(true)));
                        }
                    });
            return Boolean.TRUE;
        }).thenAccept(_ -> {
        });
    }

    @Override
    public CompletableFuture<Boolean> delete(UUID recipient, UUID messageId) {
        requireNonNull(recipient, "recipient");
        requireNonNull(messageId, "messageId");

        return mutate(() ->
                inboxMutable(recipient).removeIf(message ->
                        MailMapper.toDomain(message).id().equals(messageId)
                )
        );
    }

    @Override
    public CompletableFuture<Integer> clear(UUID recipient) {
        requireNonNull(recipient, "recipient");

        return mutate(() -> {
            var removed = document.inboxes.remove(recipient.toString());
            return removed == null
                    ? 0
                    : removed.size();
        });
    }

    @Override
    public CompletableFuture<Integer> purgeExpired(long now) {
        return mutate(() -> purgeExpiredInternal(now));
    }

    private int purgeExpiredInternal(long now) {
        var removed = 0;
        var iterator = document.inboxes.entrySet().iterator();

        while (iterator.hasNext()) {
            var entry = iterator.next();
            var before = entry.getValue().size();

            entry.getValue().removeIf(message -> MailMapper.toDomain(message).expired(now));
            removed += before - entry.getValue().size();

            if (entry.getValue().isEmpty()) {
                iterator.remove();
            }
        }

        return removed;
    }

    private <T> CompletableFuture<T> mutate(Supplier<T> operation) {
        return mutate(operation, true);
    }

    private void add(MailMessage message) {
        var messages = inboxMutable(message.toUuid());
        messages.add(MailMapper.fromDomain(message));

        var maximum = config.maxMailPerPlayer;
        while (messages.size() > maximum) {
            messages.removeFirst();
        }
    }

    private <T> CompletableFuture<T> mutate(Supplier<T> operation, boolean alwaysSave) {
        synchronized (queueLock) {
            var next = tail
                    .handle((_, _) -> Boolean.TRUE)
                    .thenCompose(_ -> {
                        var snapshot = document.copy();
                        final T result;

                        try {
                            result = operation.get();
                        } catch (RuntimeException exception) {
                            document = snapshot;
                            return CompletableFuture.failedFuture(exception);
                        }

                        if (!alwaysSave && same(snapshot, document)) {
                            return CompletableFuture.completedFuture(result);
                        }

                        return storage
                                .save(path, document)
                                .handle((_, failure) -> {
                                    if (failure != null) {
                                        document = snapshot;
                                        throw new IllegalStateException(
                                                "Unable to persist mail data",
                                                failure
                                        );
                                    }
                                    return result;
                                });
                    });

            tail = next.handle((_, _) -> Boolean.TRUE);
            return next;
        }
    }

    private ArrayList<MailMessageDocument> inboxMutable(UUID recipient) {
        var existing = document.inboxes.computeIfAbsent(
                recipient.toString(),
                _ -> new ArrayList<>()
        );
        var replacement = new ArrayList<>(existing);

        document.inboxes.put(recipient.toString(), replacement);
        return replacement;
    }

    private static boolean same(MailDocument first, MailDocument second) {
        return first.inboxes.equals(second.inboxes);
    }

}
