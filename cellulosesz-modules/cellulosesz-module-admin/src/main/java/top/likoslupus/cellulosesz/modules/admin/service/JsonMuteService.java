package top.likoslupus.cellulosesz.modules.admin.service;

import org.jspecify.annotations.Nullable;
import top.likoslupus.cellulosesz.api.admin.AdminResult;
import top.likoslupus.cellulosesz.api.admin.AdminStatus;
import top.likoslupus.cellulosesz.api.admin.BanRecord;
import top.likoslupus.cellulosesz.api.admin.MuteService;
import top.likoslupus.cellulosesz.api.lifecycle.AsyncInitializable;
import top.likoslupus.cellulosesz.api.storage.StorageService;
import top.likoslupus.cellulosesz.modules.admin.data.MuteDocument;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public final class JsonMuteService implements MuteService, AsyncInitializable {

    private final StorageService storage;
    private final Path path;
    private MuteDocument document;
    private CompletableFuture<Void> mutationTail = CompletableFuture.completedFuture(null);

    public JsonMuteService(StorageService storage, Path path) {
        this.storage = storage;
        this.path = path;
        this.document = new MuteDocument();
    }

    @Override
    public CompletableFuture<Void> initialize() {
        return storage.createIfMissing(path, MuteDocument.class, MuteDocument::new)
                .thenApply(loaded -> {
                    validate(loaded);
                    return loaded;
                })
                .thenAccept(loaded -> {
                    synchronized (this) {
                        document = loaded;
                    }
                });
    }

    private void validate(MuteDocument candidate) {
        if (candidate.records.stream().anyMatch(BanRecord::ip)) {
            throw new IllegalStateException("Mute storage contains an IP punishment record");
        }
    }

    @Override
    public CompletableFuture<AdminResult> mute(
            UUID uuid,
            String name,
            String actor,
            @Nullable Long durationMillis,
            String reason
    ) {
        final long createdAt = System.currentTimeMillis();
        final Long expiresAt;
        try {
            expiresAt = durationMillis == null || durationMillis <= 0L
                    ? null
                    : Math.addExact(createdAt, durationMillis);
        } catch (ArithmeticException exception) {
            return CompletableFuture.completedFuture(AdminResult.failure(
                    AdminStatus.INVALID_INPUT, "service.admin.invalid-duration"));
        }
        var record = new BanRecord(uuid, name, reason, actor, createdAt, expiresAt, false, null);
        return mutate(current -> {
            current.records.removeIf(existing -> uuid.equals(existing.uuid()));
            current.records.add(record);
            return AdminResult.success("service.admin.mute-success", Map.of("player", name));
        });
    }

    @Override
    public CompletableFuture<AdminResult> unmute(UUID uuid, String name, String actor) {
        return mutate(current -> current.records.removeIf(record -> uuid.equals(record.uuid()))
                ? AdminResult.success("service.admin.unmute-success", Map.of("player", name))
                : AdminResult.failure(AdminStatus.NOT_FOUND, "service.admin.not-muted", Map.of("player", name)));
    }

    @Override
    public synchronized boolean muted(UUID uuid) {
        return record(uuid).isPresent();
    }

    @Override
    public synchronized Optional<BanRecord> record(UUID uuid) {
        var now = System.currentTimeMillis();
        return document.records.stream()
                .filter(record -> uuid.equals(record.uuid()))
                .filter(record -> !record.expired(now))
                .findFirst();
    }

    @Override
    public CompletableFuture<Integer> purgeExpired() {
        var result = new CompletableFuture<Integer>();
        enqueue(current -> {
            var before = current.records.size();
            current.records.removeIf(record -> record.expired(System.currentTimeMillis()));
            return new Mutation<>(current, before - current.records.size());
        }, result);
        return result;
    }

    private CompletableFuture<AdminResult> mutate(Function<MuteDocument, AdminResult> operation) {
        var result = new CompletableFuture<AdminResult>();
        enqueue(current -> new Mutation<>(current, operation.apply(current)), result);
        return result;
    }

    private synchronized <T> void enqueue(
            Function<MuteDocument, Mutation<T>> operation,
            CompletableFuture<T> result
    ) {
        mutationTail = mutationTail.handle((_, _) -> null)
                .thenCompose(ignored -> {
                    MuteDocument current;
                    synchronized (this) {
                        current = copy(document);
                    }
                    final Mutation<T> mutation;
                    try {
                        mutation = operation.apply(current);
                    } catch (RuntimeException exception) {
                        result.completeExceptionally(exception);
                        return CompletableFuture.completedFuture(null);
                    }
                    return storage.save(path, mutation.document()).handle((_, failure) -> {
                        if (failure == null) {
                            synchronized (this) {
                                document = mutation.document();
                            }
                            result.complete(mutation.result());
                        } else if (mutation.result() instanceof AdminResult) {
                            @SuppressWarnings("unchecked")
                            var failureResult = (T) AdminResult.failure(
                                    AdminStatus.PERSISTENCE_FAILURE, "service.admin.persistence-failed");
                            result.complete(failureResult);
                        } else {
                            result.completeExceptionally(failure);
                        }
                        return (Void) null;
                    });
                });
        mutationTail.whenComplete((ignored, failure) -> {
            if (failure != null) result.completeExceptionally(failure);
        });
    }

    private MuteDocument copy(MuteDocument source) {
        var target = new MuteDocument();
        target.records = new java.util.ArrayList<>(source.records);
        return target;
    }

    private record Mutation<T>(
            MuteDocument document,
            T result
    ) {

    }

}
