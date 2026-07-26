package top.likoslupus.cellulosesz.modules.admin.service;

import org.jspecify.annotations.Nullable;
import top.likoslupus.cellulosesz.api.admin.*;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.storage.StorageService;
import top.likoslupus.cellulosesz.api.teleport.CellLocation;
import top.likoslupus.cellulosesz.modules.admin.config.AdminConfig;
import top.likoslupus.cellulosesz.modules.admin.data.JailDocument;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public final class JsonJailService implements JailService {

    private final StorageService storage;
    private final Path path;
    private final PlatformService platform;
    private final AdminConfig config;
    private JailDocument document;
    private CompletableFuture<Void> mutationTail = CompletableFuture.completedFuture(null);

    public JsonJailService(
            StorageService storage,
            Path path,
            PlatformService platform,
            AdminConfig config
    ) {
        this.storage = storage;
        this.path = path;
        this.platform = platform;
        this.config = config;
        this.document = storage.load(path, JailDocument.class, JailDocument::new).join();
        validate(document);
    }

    private void validate(JailDocument candidate) {
        candidate.jails.forEach(jail -> {
            if (normalize(jail.name).isEmpty()) throw new IllegalStateException("Jail name must not be blank");
            if (jail.location == null) throw new IllegalStateException("Jail location is required");
        });
        candidate.jailed.forEach(record -> {
            if (record.uuid == null) throw new IllegalStateException("Jailed player UUID is required");
            if (record.jail.isBlank()) throw new IllegalStateException("Jailed player jail is required");
            if (record.expiresAt != null && record.expiresAt > 0L && record.expiresAt <= record.createdAt) {
                throw new IllegalStateException("Jail expiry must be after creation");
            }
        });
    }

    private String normalize(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }

    @Override
    public CompletableFuture<AdminResult> setJail(
            String name,
            CellLocation location,
            String actor
    ) {
        var normalized = normalize(name);
        if (normalized.isEmpty()) {
            return CompletableFuture.completedFuture(AdminResult.failure(
                    AdminStatus.INVALID_INPUT, "service.admin.jail-invalid-name"));
        }
        var jail = new Jail();
        jail.name = normalized;
        jail.location = copy(location);
        jail.createdBy = actor;
        jail.createdAt = System.currentTimeMillis();
        return mutate(current -> {
            current.jails.removeIf(existing -> existing.name.equalsIgnoreCase(normalized));
            current.jails.add(copy(jail));
            return AdminResult.success("service.admin.jail-set", Map.of("jail", normalized));
        });
    }

    @Override
    public CompletableFuture<AdminResult> deleteJail(String name) {
        var normalized = normalize(name);
        final List<JailedPlayer> affected;
        synchronized (this) {
            if (document.jails.stream().noneMatch(jail -> jail.name.equalsIgnoreCase(normalized))) {
                return CompletableFuture.completedFuture(AdminResult.failure(
                        AdminStatus.NOT_FOUND,
                        "service.admin.jail-not-found",
                        Map.of("jail", normalized)
                ));
            }
            affected = document.jailed.stream()
                    .filter(record -> record.jail.equalsIgnoreCase(normalized))
                    .map(this::copy)
                    .toList();
        }
        return mutate(current -> {
            current.jails.removeIf(jail -> jail.name.equalsIgnoreCase(normalized));
            current.jailed.stream()
                    .filter(record -> record.jail.equalsIgnoreCase(normalized))
                    .forEach(record -> record.expiresAt = 0L);
            return AdminResult.success("service.admin.jail-deleted", Map.of("jail", normalized));
        }).thenCompose(saved -> {
            if (!saved.success() || affected.isEmpty()) return CompletableFuture.completedFuture(saved);
            return releaseAll(affected).thenCompose(outcomes -> {
                var released = outcomes.stream()
                        .filter(outcome -> outcome.result() == ReleaseResult.RELEASED)
                        .map(ReleaseAttempt::record)
                        .map(record -> record.uuid)
                        .toList();
                var pending = outcomes.size() - released.size();
                if (released.isEmpty()) {
                    return CompletableFuture.completedFuture(pending == 0
                            ? saved
                            : AdminResult.partial(
                                    "service.admin.jail-delete-release-pending",
                                    Map.of("jail", normalized, "count", pending)
                            ));
                }
                return mutate(current -> {
                    current.jailed.removeIf(record -> released.contains(record.uuid));
                    return pending == 0
                            ? saved
                            : AdminResult.partial(
                                    "service.admin.jail-delete-release-pending",
                                    Map.of("jail", normalized, "count", pending)
                            );
                });
            });
        });
    }

    @Override
    public synchronized Optional<Jail> jail(String name) {
        var normalized = normalize(name);
        return document.jails.stream()
                .filter(jail -> jail.name.equalsIgnoreCase(normalized))
                .findFirst()
                .map(this::copy);
    }

    @Override
    public synchronized Collection<Jail> jails() {
        return document.jails.stream().map(this::copy).toList();
    }

    @Override
    public CompletableFuture<AdminResult> jailPlayer(
            CellPlayer player,
            String jailName,
            String actor,
            @Nullable Long durationMillis,
            String reason
    ) {
        final Jail jail;
        final @Nullable JailedPlayer previous;
        synchronized (this) {
            var found = document.jails.stream()
                    .filter(value -> value.name.equalsIgnoreCase(normalize(jailName)))
                    .findFirst();
            if (found.isEmpty()) {
                return CompletableFuture.completedFuture(AdminResult.failure(
                        AdminStatus.NOT_FOUND,
                        "service.admin.jail-not-found",
                        Map.of("jail", jailName)
                ));
            }
            jail = copy(found.orElseThrow());
            previous = document.jailed.stream()
                    .filter(existing -> player.uuid().equals(existing.uuid))
                    .findFirst()
                    .map(this::copy)
                    .orElse(null);
        }

        var beforeTeleport = copy(platform.location(player));
        var createdAt = System.currentTimeMillis();
        final @Nullable Long expiresAt;
        try {
            expiresAt = durationMillis == null || durationMillis <= 0L
                    ? null
                    : Math.addExact(createdAt, durationMillis);
        } catch (ArithmeticException exception) {
            return CompletableFuture.completedFuture(AdminResult.failure(
                    AdminStatus.INVALID_INPUT, "service.admin.invalid-duration"));
        }

        var record = new JailedPlayer();
        record.uuid = player.uuid();
        record.name = player.name();
        record.jail = jail.name;
        record.actor = actor;
        record.reason = reason;
        record.createdAt = createdAt;
        record.expiresAt = expiresAt;
        record.returnLocation = previous != null && previous.returnLocation != null
                ? copy(previous.returnLocation)
                : beforeTeleport;

        return mutate(current -> {
            current.jailed.removeIf(existing -> player.uuid().equals(existing.uuid));
            current.jailed.add(copy(record));
            return AdminResult.success(
                    "service.admin.player-jailed",
                    Map.of("player", player.name(), "jail", jail.name)
            );
        }).thenCompose(saved -> {
            if (!saved.success()) return CompletableFuture.completedFuture(saved);
            return teleport(player, jail.location).thenCompose(moved -> {
                if (moved) return CompletableFuture.completedFuture(saved);
                return mutate(current -> {
                    current.jailed.removeIf(existing -> player.uuid().equals(existing.uuid));
                    if (previous != null) current.jailed.add(copy(previous));
                    return AdminResult.failure(
                            AdminStatus.PLATFORM_FAILURE,
                            "service.admin.jail-teleport-failed",
                            Map.of("player", player.name())
                    );
                }).thenApply(rollback -> rollback.status() == AdminStatus.PERSISTENCE_FAILURE
                        ? AdminResult.failure(
                        AdminStatus.ROLLBACK_FAILURE,
                        "service.admin.jail-rollback-failed",
                        Map.of("player", player.name())
                )
                        : rollback);
            });
        });
    }

    @Override
    public CompletableFuture<AdminResult> unjail(UUID uuid, String name, String actor) {
        final JailedPlayer record;
        synchronized (this) {
            var found = document.jailed.stream()
                    .filter(jailed -> uuid.equals(jailed.uuid))
                    .findFirst();
            if (found.isEmpty()) {
                return CompletableFuture.completedFuture(AdminResult.failure(
                        AdminStatus.NOT_FOUND,
                        "service.admin.player-not-jailed",
                        Map.of("player", name)
                ));
            }
            record = copy(found.orElseThrow());
        }
        return mutate(current -> {
            current.jailed.stream()
                    .filter(jailed -> uuid.equals(jailed.uuid))
                    .forEach(jailed -> jailed.expiresAt = 0L);
            return AdminResult.success("service.admin.player-unjailed", Map.of("player", name));
        }).thenCompose(saved -> {
            if (!saved.success()) return CompletableFuture.completedFuture(saved);
            return releasePlayer(record).thenCompose(result -> {
                if (result == ReleaseResult.FAILED) {
                    return CompletableFuture.completedFuture(AdminResult.failure(
                            AdminStatus.PLATFORM_FAILURE,
                            "service.admin.jail-release-failed",
                            Map.of("player", name)
                    ));
                }
                if (result == ReleaseResult.PENDING) return CompletableFuture.completedFuture(saved);
                return mutate(current -> {
                    current.jailed.removeIf(jailed -> uuid.equals(jailed.uuid));
                    return saved;
                });
            });
        });
    }

    @Override
    public synchronized Optional<JailedPlayer> jailed(UUID uuid) {
        var now = System.currentTimeMillis();
        return document.jailed.stream()
                .filter(record -> uuid.equals(record.uuid))
                .filter(record -> !record.expired(now))
                .findFirst()
                .map(this::copy);
    }

    @Override
    public synchronized Collection<JailedPlayer> jailedPlayers() {
        var now = System.currentTimeMillis();
        return document.jailed.stream()
                .filter(record -> !record.expired(now))
                .map(this::copy)
                .toList();
    }

    @Override
    public CompletableFuture<Integer> purgeExpired() {
        final List<JailedPlayer> expired;
        synchronized (this) {
            var now = System.currentTimeMillis();
            expired = document.jailed.stream()
                    .filter(record -> record.expired(now))
                    .map(this::copy)
                    .toList();
        }
        if (expired.isEmpty()) return CompletableFuture.completedFuture(0);
        return releaseAll(expired).thenCompose(outcomes -> {
            var released = outcomes.stream()
                    .filter(outcome -> outcome.result() == ReleaseResult.RELEASED)
                    .map(outcome -> outcome.record().uuid)
                    .toList();
            if (released.isEmpty()) return CompletableFuture.completedFuture(0);
            var result = new CompletableFuture<Integer>();
            enqueue(current -> {
                current.jailed.removeIf(record -> released.contains(record.uuid));
                return new Mutation<>(current, released.size());
            }, result);
            return result;
        });
    }

    private JailedPlayer copy(JailedPlayer source) {
        var target = new JailedPlayer();
        target.uuid = source.uuid;
        target.name = source.name;
        target.jail = source.jail;
        target.reason = source.reason;
        target.actor = source.actor;
        target.createdAt = source.createdAt;
        target.expiresAt = source.expiresAt;
        target.returnLocation = source.returnLocation == null ? null : copy(source.returnLocation);
        return target;
    }

    private CompletableFuture<AdminResult> mutate(Function<JailDocument, AdminResult> operation) {
        var result = new CompletableFuture<AdminResult>();
        enqueue(current -> new Mutation<>(current, operation.apply(current)), result);
        return result;
    }

    private CompletableFuture<List<ReleaseAttempt>> releaseAll(List<JailedPlayer> records) {
        var futures = records.stream()
                .map(record -> releasePlayer(record)
                        .thenApply(result -> new ReleaseAttempt(record, result)))
                .toList();
        CompletableFuture<List<ReleaseAttempt>> combined =
                CompletableFuture.completedFuture(List.of());
        for (var future : futures) {
            combined = combined.thenCombine(future, (results, attempt) -> {
                var next = new ArrayList<ReleaseAttempt>(results.size() + 1);
                next.addAll(results);
                next.add(attempt);
                return List.copyOf(next);
            });
        }
        return combined;
    }

    private CellLocation copy(CellLocation source) {
        return new CellLocation(source.world, source.x, source.y, source.z, source.yaw, source.pitch);
    }

    private synchronized <T> void enqueue(
            Function<JailDocument, Mutation<T>> operation,
            CompletableFuture<T> result
    ) {
        mutationTail = mutationTail.handle((ignored, failure) -> null)
                .thenCompose(ignored -> {
                    JailDocument current;
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
                    return storage.save(path, mutation.document()).handle((saved, failure) -> {
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

    private CompletableFuture<ReleaseResult> releasePlayer(JailedPlayer record) {
        if (!config.teleportOnJailRelease || record.returnLocation == null) {
            return CompletableFuture.completedFuture(ReleaseResult.RELEASED);
        }
        var player = platform.onlinePlayers().stream()
                .filter(online -> online.uuid().equals(record.uuid))
                .findFirst();
        if (player.isEmpty()) return CompletableFuture.completedFuture(ReleaseResult.PENDING);
        return teleport(player.orElseThrow(), record.returnLocation)
                .handle((success, failure) -> failure == null && success
                        ? ReleaseResult.RELEASED
                        : ReleaseResult.FAILED);
    }

    private JailDocument copy(JailDocument source) {
        var target = new JailDocument();
        target.jails = source.jails.stream()
                .map(this::copy)
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        target.jailed = source.jailed.stream()
                .map(this::copy)
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        return target;
    }

    private CompletableFuture<Boolean> teleport(CellPlayer player, CellLocation location) {
        return platform.callOnServerThread(() -> platform.teleport(player, copy(location)))
                .thenCompose(Function.identity());
    }

    private Jail copy(Jail source) {
        var target = new Jail();
        target.name = source.name;
        target.location = copy(source.location);
        target.createdBy = source.createdBy;
        target.createdAt = source.createdAt;
        return target;
    }

    private enum ReleaseResult {
        RELEASED,
        PENDING,
        FAILED
    }

    private record ReleaseAttempt(
            JailedPlayer record,
            ReleaseResult result
    ) {

    }

    private record Mutation<T>(
            JailDocument document,
            T result
    ) {

    }

}
