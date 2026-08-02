package top.likoslupus.cellulosesz.modules.admin.service;

import top.likoslupus.cellulosesz.api.admin.*;
import top.likoslupus.cellulosesz.api.command.execution.ServerThreadExecutor;
import top.likoslupus.cellulosesz.api.lifecycle.AsyncCloseable;
import top.likoslupus.cellulosesz.api.lifecycle.AsyncInitializable;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.player.PlayerConnectionService;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.api.player.PlayerNetworkService;
import top.likoslupus.cellulosesz.api.storage.StorageService;
import top.likoslupus.cellulosesz.api.text.MessageRenderer;
import top.likoslupus.cellulosesz.api.text.PlayerAudienceService;
import top.likoslupus.cellulosesz.api.text.RichText;
import top.likoslupus.cellulosesz.core.concurrent.SerialAsyncQueue;
import top.likoslupus.cellulosesz.modules.admin.config.AdminRuntimeSettings;
import top.likoslupus.cellulosesz.modules.admin.data.TempBanDocument;

import java.net.InetAddress;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

public final class JsonTempBanService
        implements TempBanService, AsyncInitializable, AsyncCloseable {

    private static final int MAXIMUM_PENDING_MUTATIONS = 4_096;

    private final StorageService storage;
    private final Path path;
    private final PlayerDirectory players;
    private final PlayerConnectionService connections;
    private final PlayerAudienceService audience;
    private final PlayerNetworkService networks;
    private final MessageRenderer renderer;
    private final ServerThreadExecutor serverThread;
    private final Clock clock;
    private final AdminRuntimeSettings settings;

    private final SerialAsyncQueue mutations = new SerialAsyncQueue(
            Runnable::run,
            MAXIMUM_PENDING_MUTATIONS
    );
    private TempBanDocument document = new TempBanDocument();

    public JsonTempBanService(
            StorageService storage,
            Path path,
            PlayerDirectory players,
            PlayerConnectionService connections,
            PlayerAudienceService audience,
            PlayerNetworkService networks,
            MessageRenderer renderer,
            ServerThreadExecutor serverThread,
            Clock clock,
            AdminRuntimeSettings settings
    ) {
        this.storage = requireNonNull(storage, "storage");
        this.path = requireNonNull(path, "path");
        this.players = requireNonNull(players, "players");
        this.connections = requireNonNull(connections, "connections");
        this.audience = requireNonNull(audience, "audience");
        this.networks = requireNonNull(networks, "networks");
        this.renderer = requireNonNull(renderer, "renderer");
        this.serverThread = requireNonNull(serverThread, "serverThread");
        this.clock = requireNonNull(clock, "clock");
        this.settings = requireNonNull(settings, "settings");
    }

    @Override
    public CompletableFuture<Void> initialize() {
        return storage
                .createIfMissing(
                        path,
                        TempBanDocument.class,
                        TempBanDocument::new
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

    private static List<BanRecord> snapshot(TempBanDocument source) {
        var result = new ArrayList<BanRecord>();
        source.records.forEach(value -> result.add(fromDocument(value)));
        return List.copyOf(result);
    }

    private static TempBanDocument copy(TempBanDocument source) {
        var target = new TempBanDocument();

        source.records.forEach(value -> {
            var next = new TempBanDocument.Record();
            next.ip = value.ip;
            next.uuid = value.uuid;
            next.name = value.name;
            next.address = value.address;
            next.reason = value.reason;
            next.actorUuid = value.actorUuid;
            next.actorName = value.actorName;
            next.createdAt = value.createdAt;
            next.expiresAt = value.expiresAt;
            target.records.add(next);
        });

        return target;
    }

    private static BanRecord fromDocument(TempBanDocument.Record value) {
        var actor = new AdminActor(
                value.actorUuid.isBlank()
                        ? Optional.empty()
                        : Optional.of(UUID.fromString(value.actorUuid)),
                value.actorName
        );

        var created = Instant.ofEpochMilli(value.createdAt);
        var expiration = Expiration.at(Instant.ofEpochMilli(value.expiresAt));

        if (value.ip) {
            var address = IpAddresses
                    .parseLiteral(value.address)
                    .orElseThrow(() -> new IllegalStateException("Invalid stored IP address"));

            return BanRecord.address(
                    address,
                    value.reason,
                    actor,
                    created,
                    expiration
            );
        }

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
    public CompletableFuture<AdminResult> tempBan(
            UUID uuid,
            String name,
            AdminActor actor,
            Duration duration,
            String reason
    ) {
        return mutations.submit(() -> {
            final Expiration expiration;
            try {
                expiration = Expiration.after(clock.instant(), duration);
            } catch (IllegalArgumentException failure) {
                return invalidDuration();
            }

            var record = BanRecord.player(
                    uuid,
                    name,
                    reason,
                    actor,
                    clock.instant(),
                    expiration
            );

            return mutateAccepted(current -> {
                current.records.removeIf(value ->
                        !value.ip
                                && (
                                value.uuid.equals(uuid.toString())
                                        || value.name.equalsIgnoreCase(name)
                        )
                );
                current.records.add(toDocument(record));

                return AdminResult.success(
                        "service.admin.temp-ban-success",
                        Map.of("player", name)
                );
            }).thenCompose(result -> (!result.success() || !settings.tempBanKickOnlinePlayers())
                    ? completed(result)
                    : disconnectUser(
                            uuid,
                            reason,
                            result
                    )
            );
        });
    }

    @Override
    public CompletableFuture<AdminResult> tempBanIp(
            InetAddress address,
            AdminActor actor,
            Duration duration,
            String reason
    ) {
        return mutations.submit(() -> {
            final Expiration expiration;
            try {
                expiration = Expiration.after(clock.instant(), duration);
            } catch (IllegalArgumentException _) {
                return invalidDuration();
            }

            var record = BanRecord.address(
                    address,
                    reason,
                    actor,
                    clock.instant(),
                    expiration
            );

            var canonical = IpAddresses.canonical(address);
            return mutateAccepted(current -> {
                current.records.removeIf(value -> value.ip
                        && value.address.equals(canonical)
                );
                current.records.add(toDocument(record));

                return AdminResult.success(
                        "service.admin.temp-ban-ip-success",
                        Map.of("address", canonical)
                );
            }).thenCompose(result -> (!result.success() || !settings.tempBanKickOnlinePlayers())
                    ? completed(result)
                    : disconnectAddress(
                            address,
                            reason,
                            result
                    )
            );
        });
    }

    @Override
    public CompletableFuture<AdminResult> unban(
            UUID uuid,
            String name,
            AdminActor actor
    ) {
        return mutations.submit(() -> mutateAccepted(current ->
                current.records.removeIf(value ->
                        !value.ip && (
                                value.uuid.equals(uuid.toString())
                                        || value.name.equalsIgnoreCase(name)
                        )
                )
                        ?
                        AdminResult.success(
                                "service.admin.temp-unban-success",
                                Map.of("player", name)
                        )
                        : AdminResult.failure(
                                AdminStatus.NOT_FOUND,
                                "service.admin.temp-ban-not-found",
                                Map.of("player", name)
                        )
        ));
    }

    @Override
    public CompletableFuture<AdminResult> unbanIp(InetAddress address, AdminActor actor) {
        return mutations.submit(() -> {
            var canonical = IpAddresses.canonical(address);
            return mutateAccepted(current ->
                    current.records.removeIf(value ->
                            value.ip && value.address.equals(canonical)
                    )
                            ?
                            AdminResult.success(
                                    "service.admin.temp-unban-ip-success",
                                    Map.of("address", canonical)
                            )
                            : AdminResult.failure(
                                    AdminStatus.NOT_FOUND,
                                    "service.admin.temp-ban-ip-not-found",
                                    Map.of("address", canonical)
                            )
            );
        });
    }

    @Override
    public synchronized Optional<BanRecord> active(UUID uuid, String name) {
        var now = clock.instant();

        return snapshot(document)
                .stream()
                .filter(value ->
                        !value.ip() && !value.expired(now)
                )
                .filter(value ->
                        value.uuid().orElseThrow().equals(uuid)
                                || value.name().equalsIgnoreCase(name)
                )
                .findFirst();
    }

    @Override
    public synchronized Optional<BanRecord> activeIp(InetAddress address) {
        var canonical = IpAddresses.canonical(address);
        var now = clock.instant();

        return snapshot(document)
                .stream()
                .filter(BanRecord::ip)
                .filter(value -> !value.expired(now))
                .filter(value ->
                        IpAddresses.canonical(value.address().orElseThrow()).equals(canonical)
                )
                .findFirst();
    }

    @Override
    public CompletableFuture<Integer> purgeExpired() {
        return mutations.submit(() -> persistAccepted(current -> {
            var before = current.records.size();
            var now = clock.instant();

            current.records.removeIf(value -> fromDocument(value).expired(now));
            return new Mutation<>(
                    current,
                    before - current.records.size()
            );
        }));
    }

    private CompletableFuture<AdminResult> disconnectAddress(
            InetAddress address,
            String reason,
            AdminResult success
    ) {
        return serverThread
                .submit(() -> {
                    var failed = false;
                    var canonical = IpAddresses.canonical(address);

                    for (var player : players.onlinePlayers()) {
                        if (networks.address(player)
                                .map(IpAddresses::canonical)
                                .filter(canonical::equals)
                                .isEmpty()
                        ) {
                            continue;
                        }

                        if (!connections.disconnect(
                                player,
                                kickMessage(player, reason)
                        ).successful()) {
                            failed = true;
                        }
                    }

                    return !failed;
                })
                .thenApply(all ->
                        all
                                ? success
                                : AdminResult.partial(
                                        "service.admin.temp-ban-ip-success",
                                        success.message().placeholders()
                                )
                )
                .exceptionally(_ ->
                        AdminResult.partial(
                                "service.admin.temp-ban-ip-success",
                                success.message().placeholders()
                        )
                );
    }

    private static CompletableFuture<AdminResult> invalidDuration() {
        return completed(
                AdminResult.failure(
                        AdminStatus.INVALID_INPUT,
                        "service.admin.invalid-duration"
                )
        );
    }

    private CompletableFuture<AdminResult> mutateAccepted(
            Function<TempBanDocument, AdminResult> operation
    ) {
        return persistAccepted(current -> new Mutation<>(
                current,
                operation.apply(current)
        )).exceptionally(_ -> AdminResult.failure(
                AdminStatus.PERSISTENCE_FAILURE,
                "service.admin.persistence-failed"
        ));
    }

    private static TempBanDocument.Record toDocument(BanRecord value) {
        var target = new TempBanDocument.Record();

        target.ip = value.ip();
        target.uuid = value.uuid()
                .map(UUID::toString)
                .orElse("");
        target.name = value.name();
        target.address = value.address()
                .map(IpAddresses::canonical)
                .orElse("");
        target.reason = value.reason();
        target.actorUuid = value.actor()
                .uuid()
                .map(UUID::toString)
                .orElse("");
        target.actorName = value.actor().name();
        target.createdAt = value.createdAt().toEpochMilli();
        target.expiresAt = value.expiration()
                .expiresAt()
                .orElseThrow()
                .toEpochMilli();

        return target;
    }

    private static CompletableFuture<AdminResult> completed(AdminResult result) {
        return CompletableFuture.completedFuture(result);
    }

    private CompletableFuture<AdminResult> disconnectUser(
            UUID uuid,
            String reason,
            AdminResult success
    ) {
        return serverThread
                .submit(() -> players.onlinePlayer(uuid)
                        .map(player -> connections.disconnect(
                                player,
                                kickMessage(player, reason)
                        ).successful())
                        .orElse(true)
                )
                .thenApply(disconnected ->
                        disconnected
                                ? success
                                : AdminResult.partial(
                                        "service.admin.temp-ban-success",
                                        success.message().placeholders()
                                )
                )
                .exceptionally(_ ->
                        AdminResult.partial(
                                "service.admin.temp-ban-success",
                                success.message().placeholders()
                        )
                );
    }

    private <T> CompletableFuture<T> persistAccepted(
            Function<TempBanDocument, Mutation<T>> operation
    ) {
        TempBanDocument current;
        synchronized (this) {
            current = copy(document);
        }

        var mutation = operation.apply(current);
        return storage
                .save(path, mutation.document())
                .thenApply(_ -> {
                    synchronized (this) {
                        document = mutation.document();
                    }

                    return mutation.result();
                });
    }

    private RichText kickMessage(
            CellPlayer player,
            String reason
    ) {
        return renderer.render(
                audience.locale(player),
                "service.admin.temp-ban-kick",
                Map.of("reason", reason)
        );
    }

    @Override
    public void stopAccepting() {
        mutations.stopAccepting();
    }

    @Override
    public CompletableFuture<Void> drain() {
        return mutations.drain();
    }

    private record Mutation<T>(
            TempBanDocument document,
            T result
    ) {

    }

}
