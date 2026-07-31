package top.likoslupus.cellulosesz.modules.admin.service;

import top.likoslupus.cellulosesz.api.admin.*;
import top.likoslupus.cellulosesz.api.lifecycle.AsyncInitializable;
import top.likoslupus.cellulosesz.api.storage.StorageService;
import top.likoslupus.cellulosesz.modules.admin.data.MuteDocument;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

public final class JsonMuteService implements MuteService, AsyncInitializable {

    private final StorageService storage;
    private final Path path;
    private final Clock clock;

    private MuteDocument document = new MuteDocument();
    private CompletableFuture<Void> mutationTail = CompletableFuture.completedFuture(null);

    public JsonMuteService(
            StorageService storage,
            Path path,
            Clock clock
    ) {
        this.storage = requireNonNull(storage, "storage");
        this.path = requireNonNull(path, "path");
        this.clock = requireNonNull(clock, "clock");
    }

    @Override
    public CompletableFuture<Void> initialize() {
        return storage.createIfMissing(
                        path,
                        MuteDocument.class,
                        MuteDocument::new
                )
                .thenApply(loaded -> {
                    snapshot(loaded);
                    return loaded;
                })
                .thenAccept(loaded -> {
                    synchronized (this) {
                        document = copy(loaded);
                    }
                });
    }

    private static List<BanRecord> snapshot(MuteDocument source) {
        var values = new ArrayList<BanRecord>();

        source.records.forEach(
                value -> values.add(fromDocument(value))
        );

        return List.copyOf(values);
    }

    private static MuteDocument copy(MuteDocument source) {
        var target = new MuteDocument();

        source.records.forEach(value -> {
            var next = new MuteDocument.Record();
            next.uuid = value.uuid;
            next.name = value.name;
            next.reason = value.reason;
            next.actorUuid = value.actorUuid;
            next.actorName = value.actorName;
            next.createdAt = value.createdAt;
            next.permanent = value.permanent;
            next.expiresAt = value.expiresAt;
            target.records.add(next);
        });

        return target;
    }

    private static BanRecord fromDocument(MuteDocument.Record value) {
        var created = Instant.ofEpochMilli(value.createdAt);

        var expiration = value.permanent
                ? Expiration.permanent()
                : Expiration.at(
                        Instant.ofEpochMilli(value.expiresAt)
                );

        var actor = new AdminActor(
                value.actorUuid.isBlank()
                        ? Optional.empty()
                        : Optional.of(
                                UUID.fromString(value.actorUuid)
                        ),
                value.actorName
        );

        return BanRecord.player(
                UUID.fromString(value.uuid),
                value.name,
                value.reason,
                actor,
                created,
                expiration
        );
    }

    @Override
    public CompletableFuture<AdminResult> mute(
            UUID uuid,
            String name,
            AdminActor actor,
            Expiration expiration,
            String reason
    ) {
        var record = BanRecord.player(
                uuid,
                name,
                reason,
                actor,
                clock.instant(),
                expiration
        );

        return mutate(current -> {
            current.records.removeIf(value ->
                    value.uuid.equals(uuid.toString())
            );

            current.records.add(toDocument(record));

            return AdminResult.success(
                    "service.admin.mute-success",
                    Map.of("player", name)
            );
        });
    }

    @Override
    public CompletableFuture<AdminResult> unmute(
            UUID uuid,
            String name,
            AdminActor actor
    ) {
        return mutate(current ->
                current.records.removeIf(value ->
                        value.uuid.equals(uuid.toString())
                )
                        ? AdminResult.success(
                        "service.admin.unmute-success",
                        Map.of("player", name)
                )
                        : AdminResult.failure(
                                AdminStatus.NOT_FOUND,
                                "service.admin.not-muted",
                                Map.of("player", name)
                        )
        );
    }

    @Override
    public synchronized boolean muted(UUID uuid) {
        return record(uuid).isPresent();
    }

    @Override
    public synchronized Optional<BanRecord> record(UUID uuid) {
        var now = clock.instant();

        return snapshot(document)
                .stream()
                .filter(value ->
                        value.uuid().orElseThrow().equals(uuid)
                )
                .filter(value -> !value.expired(now))
                .findFirst();
    }

    @Override
    public CompletableFuture<Integer> purgeExpired() {
        var result = new CompletableFuture<Integer>();

        enqueue(
                current -> {
                    var before = current.records.size();
                    var now = clock.instant();

                    current.records.removeIf(value ->
                            fromDocument(value).expired(now)
                    );

                    return new Mutation<>(
                            current,
                            before - current.records.size()
                    );
                },
                result
        );

        return result;
    }

    private CompletableFuture<AdminResult> mutate(
            Function<MuteDocument, AdminResult> operation
    ) {
        var result = new CompletableFuture<AdminResult>();

        enqueue(
                current -> new Mutation<>(
                        current,
                        operation.apply(current)
                ),
                result
        );

        return result;
    }

    private static MuteDocument.Record toDocument(BanRecord value) {
        var target = new MuteDocument.Record();

        target.uuid = value.uuid()
                .orElseThrow()
                .toString();
        target.name = value.name();
        target.reason = value.reason();
        target.actorUuid = value.actor().uuid()
                .map(UUID::toString)
                .orElse("");
        target.actorName = value.actor().name();
        target.createdAt = value.createdAt().toEpochMilli();
        target.permanent = value.expiration() instanceof Expiration.Permanent;
        target.expiresAt = value.expiration().expiresAt()
                .map(Instant::toEpochMilli)
                .orElse(0L);

        return target;
    }

    private synchronized <T> void enqueue(
            Function<MuteDocument, Mutation<T>> operation,
            CompletableFuture<T> result
    ) {
        mutationTail = mutationTail
                .handle((_, _) -> null)
                .thenCompose(_ -> {
                    MuteDocument current;

                    synchronized (this) {
                        current = copy(document);
                    }

                    final Mutation<T> mutation;

                    try {
                        mutation = operation.apply(current);
                    } catch (RuntimeException failure) {
                        result.completeExceptionally(failure);
                        return CompletableFuture.completedFuture(null);
                    }

                    return storage.save(
                                    path,
                                    mutation.document()
                            )
                            .handle((_, failure) -> {
                                if (failure == null) {
                                    synchronized (this) {
                                        document = mutation.document();
                                    }

                                    result.complete(mutation.result());
                                } else if (mutation.result() instanceof AdminResult) {
                                    @SuppressWarnings("unchecked")
                                    var value = (T) AdminResult.failure(
                                            AdminStatus.PERSISTENCE_FAILURE,
                                            "service.admin.persistence-failed"
                                    );

                                    result.complete(value);
                                } else {
                                    result.completeExceptionally(
                                            failure
                                    );
                                }

                                return (Void) null;
                            });
                });

        mutationTail.whenComplete((_, failure) -> {
            if (failure != null) {
                result.completeExceptionally(failure);
            }
        });
    }

    private record Mutation<T>(
            MuteDocument document,
            T result
    ) {

    }

}
