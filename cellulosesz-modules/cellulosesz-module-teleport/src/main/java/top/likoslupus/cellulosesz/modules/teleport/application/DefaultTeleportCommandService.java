package top.likoslupus.cellulosesz.modules.teleport.application;

import top.likoslupus.cellulosesz.api.command.execution.ServerThreadExecutor;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.api.player.PlayerLocationPlatformService;
import top.likoslupus.cellulosesz.api.player.PlayerResolver;
import top.likoslupus.cellulosesz.api.teleport.*;
import top.likoslupus.cellulosesz.api.user.UserService;
import top.likoslupus.cellulosesz.api.world.WorldDirectory;
import top.likoslupus.cellulosesz.api.world.WorldResolution;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;
import static top.likoslupus.cellulosesz.api.validation.Checks.requirePositive;
import static top.likoslupus.cellulosesz.modules.teleport.application.TeleportCommandStatus.*;

public final class DefaultTeleportCommandService implements TeleportCommandService {

    private final PlayerDirectory players;
    private final PlayerResolver resolver;
    private final PlayerLocationPlatformService locations;
    private final TeleportOperations operations;
    private final TeleportService teleports;
    private final OfflineLocationService offlineLocations;
    private final WorldDirectory worlds;
    private final UserService users;
    private final ServerThreadExecutor serverThread;
    private final int maximumBulkTargets;

    public DefaultTeleportCommandService(
            PlayerDirectory players,
            PlayerResolver resolver,
            PlayerLocationPlatformService locations,
            TeleportOperations operations,
            TeleportService teleports,
            OfflineLocationService offlineLocations,
            WorldDirectory worlds,
            UserService users,
            ServerThreadExecutor serverThread,
            int maximumBulkTargets
    ) {
        this.players = requireNonNull(players, "players");
        this.resolver = requireNonNull(resolver, "resolver");
        this.locations = requireNonNull(locations, "locations");
        this.operations = requireNonNull(operations, "operations");
        this.teleports = requireNonNull(teleports, "teleports");
        this.offlineLocations = requireNonNull(offlineLocations, "offlineLocations");
        this.worlds = requireNonNull(worlds, "worlds");
        this.users = requireNonNull(users, "users");
        this.serverThread = requireNonNull(serverThread, "serverThread");
        this.maximumBulkTargets = requirePositive(maximumBulkTargets, "maximumBulkTargets");
    }

    @Override
    public CompletableFuture<TeleportCommandResult> tp(
            Optional<CellPlayer> actor,
            String first,
            Optional<String> second,
            boolean override,
            boolean bypassPreference
    ) {
        if (second.isEmpty()) {
            if (actor.isEmpty()) {
                return completed(failure(INVALID_INPUT, "common.player-only"));
            }

            var destination = players.onlinePlayer(first);
            if (destination.isEmpty()) {
                return offline(first);
            }

            return move(
                    actor.orElseThrow(),
                    destination.orElseThrow(),
                    override,
                    bypassPreference,
                    true
            );
        }

        var mover = players.onlinePlayer(first);
        var destination = players.onlinePlayer(second.orElseThrow());

        if (mover.isEmpty()) {
            return offline(first);
        }
        if (destination.isEmpty()) {
            return offline(second.orElseThrow());
        }

        var selfMove = actor
                .map(source ->
                        source.uuid().equals(mover.orElseThrow().uuid())
                ).orElse(false);
        return move(
                mover.orElseThrow(),
                destination.orElseThrow(),
                override,
                bypassPreference,
                selfMove
        );
    }

    @Override
    public CompletableFuture<TeleportCommandResult> here(
            CellPlayer actor,
            String target,
            boolean override,
            boolean bypassPreference
    ) {
        var mover = players.onlinePlayer(target);
        if (mover.isEmpty()) {
            return offline(target);
        }

        return move(
                mover.orElseThrow(),
                actor,
                override,
                bypassPreference,
                mover.orElseThrow().uuid().equals(actor.uuid())
        );
    }

    @Override
    public CompletableFuture<TeleportCommandResult> position(
            CellPlayer actor,
            double x, double y, double z,
            Optional<String> world
    ) {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
            return completed(failure(INVALID_INPUT, "commands.teleport.invalid-coordinate"));
        }

        return serverThread
                .submit(() -> {
                    var current = locations.currentLocation(actor);
                    var worldId = world.isEmpty()
                            ? Optional.of(current.world)
                            : worlds.resolveLoadedWorld(world.orElseThrow());

                    return worldId.map(value -> new CellLocation(
                            value,
                            x, y, z,
                            current.yaw, current.pitch
                    ));
                })
                .thenCompose(destination -> destination.isEmpty()
                        ? completed(failure(NOT_FOUND, "commands.teleport.world-not-found"))
                        : teleports
                                .teleport(
                                        actor,
                                        destination.orElseThrow(),
                                        TeleportOptions.defaults()
                                )
                                .thenApply(DefaultTeleportCommandService::mapTeleport)
                );
    }

    @Override
    public CompletableFuture<TeleportCommandResult> all(
            Optional<CellPlayer> actor,
            Optional<String> destinationName,
            boolean bypassPreference
    ) {
        final Optional<CellPlayer> destination;

        if (destinationName.isPresent()) {
            destination = players.onlinePlayer(destinationName.orElseThrow());
            if (destination.isEmpty()) {
                return offline(destinationName.orElseThrow());
            }
        } else {
            destination = actor;
            if (destination.isEmpty()) {
                return completed(failure(INVALID_INPUT, "common.player-only"));
            }
        }

        var target = destination.orElseThrow();
        var candidates = players.onlinePlayers().stream()
                .filter(player -> !player.uuid().equals(target.uuid()))
                .limit(maximumBulkTargets).toList();

        return serverThread
                .submit(() -> locations.currentLocation(target))
                .thenCompose(location -> {
                    var counts = new int[3];
                    CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);

                    for (var candidate : candidates) {
                        chain = chain
                                .thenCompose(_ -> preference(candidate, bypassPreference)
                                        .thenCompose(allowed -> {
                                            if (!allowed) {
                                                counts[1]++;
                                                return CompletableFuture.completedFuture(null);
                                            }

                                            return teleports
                                                    .teleport(
                                                            candidate,
                                                            location,
                                                            TeleportOptions.defaults()
                                                    )
                                                    .handle((result, failure) -> {
                                                        if (failure == null && result.success()) {
                                                            counts[0]++;
                                                        } else {
                                                            counts[2]++;
                                                        }
                                                        return (Void) null;
                                                    });
                                        })
                                );
                    }

                    return chain
                            .thenApply(_ -> counts[2] == 0 ?
                                    TeleportCommandResult.success(
                                            "commands.teleport.tp-all-command.reply.teleported-all-players",
                                            Map.of(
                                                    "success", counts[0],
                                                    "blocked", counts[1],
                                                    "failed", counts[2]
                                            )
                                    ) :
                                    TeleportCommandResult.partial(
                                            "commands.teleport.tp-all-command.reply.teleported-all-players",
                                            Map.of(
                                                    "success", counts[0],
                                                    "blocked", counts[1],
                                                    "failed", counts[2]
                                            )
                                    )
                            );
                });
    }

    private CompletableFuture<Boolean> preference(
            CellPlayer player,
            boolean bypass
    ) {
        return bypass
                ? CompletableFuture.completedFuture(true)
                : users
                        .load(player.uuid())
                        .thenApply(user -> user.preferences.teleportRequests)
                        .exceptionally(_ -> false);
    }

    @Override
    public CompletableFuture<TeleportCommandResult> offline(CellPlayer actor, String target) {
        return resolver
                .resolve(target, actor)
                .thenCompose(resolved -> {
                    if (resolved.optionalUuid().isEmpty()) {
                        return completed(failure(
                                NOT_FOUND,
                                "commands.common.player-not-found",
                                Map.of("player", target)
                        ));
                    }

                    var location = offlineLocations.location(
                            resolved.optionalUuid().orElseThrow()
                    );

                    if (location.isEmpty()) {
                        return completed(failure(
                                NOT_FOUND,
                                "commands.teleport.offline-location-missing",
                                Map.of("player", resolved.name())
                        ));
                    }

                    return teleports
                            .teleport(
                                    actor,
                                    location.orElseThrow(),
                                    TeleportOptions.defaults()
                            )
                            .thenApply(DefaultTeleportCommandService::mapTeleport);
                });
    }

    @Override
    public CompletableFuture<TeleportCommandResult> back(CellPlayer actor) {
        var location = teleports.backLocation(actor.uuid());

        if (location.isEmpty()) {
            return completed(failure(
                    NOT_FOUND,
                    "commands.teleport.back-command.error.no-location"
            ));
        }
        return teleports
                .teleport(
                        actor,
                        location.orElseThrow(),
                        TeleportOptions.defaults().withoutBackMemory()
                )
                .thenApply(DefaultTeleportCommandService::mapTeleport);
    }

    @Override
    public CompletableFuture<TeleportCommandResult> jump(CellPlayer actor, int maximumDistance) {
        return operationDestination(
                actor,
                () -> operations.targetLocation(actor, maximumDistance)
        );
    }

    @Override
    public CompletableFuture<TeleportCommandResult> top(CellPlayer actor) {
        return operationDestination(
                actor,
                () -> operations.highestSafeLocation(locations.currentLocation(actor))
        );
    }

    @Override
    public CompletableFuture<TeleportCommandResult> bottom(CellPlayer actor) {
        return operationDestination(
                actor,
                () -> operations.lowestSafeLocation(locations.currentLocation(actor))
        );
    }

    @Override
    public CompletableFuture<TeleportCommandResult> world(CellPlayer actor, String world) {
        var resolution = worlds.resolve(world);
        if (resolution.worldId().isEmpty()) {
            return completed(failure(
                    resolution.status() == WorldResolution.Status.AMBIGUOUS
                            ? AMBIGUOUS
                            : NOT_FOUND,
                    resolution.status() == WorldResolution.Status.AMBIGUOUS
                            ? "commands.teleport.world-ambiguous"
                            : "commands.teleport.world-not-found"
            ));
        }

        return serverThread
                .submit(() ->
                        locations.currentLocation(actor).withWorld(resolution.worldId().orElseThrow())
                )
                .thenCompose(destination ->
                        teleports.teleport(
                                actor,
                                destination,
                                TeleportOptions.defaults()
                        )
                )
                .thenApply(DefaultTeleportCommandService::mapTeleport);
    }

    private CompletableFuture<TeleportCommandResult> operationDestination(
            CellPlayer actor,
            Supplier<PlatformResult<CellLocation>> operation
    ) {
        return serverThread
                .submit(operation)
                .thenCompose(result -> {
                    if (!result.successful() || result.value().isEmpty()) {
                        return completed(failure(
                                PLATFORM_FAILURE,
                                "commands.teleport.location-search-failed"
                        ));
                    }

                    return teleports
                            .teleport(
                                    actor,
                                    result.value().orElseThrow(),
                                    TeleportOptions.defaults().withSafe(false)
                            )
                            .thenApply(DefaultTeleportCommandService::mapTeleport);
                });
    }

    private static CompletableFuture<TeleportCommandResult> completed(TeleportCommandResult value) {
        return CompletableFuture.completedFuture(value);
    }

    private static TeleportCommandResult failure(
            TeleportCommandStatus status,
            String key
    ) {
        return TeleportCommandResult.failure(status, key);
    }

    private static CompletableFuture<TeleportCommandResult> offline(String name) {
        return completed(failure(
                NOT_FOUND,
                "commands.common.player-offline",
                Map.of("player", name)
        ));
    }

    private CompletableFuture<TeleportCommandResult> move(
            CellPlayer mover,
            CellPlayer destination,
            boolean override,
            boolean bypassPreference,
            boolean selfMove
    ) {
        var policy = override || bypassPreference || selfMove
                ? CompletableFuture.completedFuture(true)
                : users
                        .load(mover.uuid())
                        .thenApply(user -> user.preferences.teleportRequests);

        return policy
                .thenCompose(allowed -> {
                    if (!allowed) {
                        return completed(failure(
                                BLOCKED,
                                "commands.teleport.request.blocked",
                                Map.of("player", mover.name())
                        ));
                    }

                    return serverThread
                            .submit(() -> locations.currentLocation(destination))
                            .thenCompose(location -> teleports.teleport(
                                    mover,
                                    location,
                                    TeleportOptions.defaults()
                            ))
                            .thenApply(DefaultTeleportCommandService::mapTeleport);
                })
                .exceptionally(_ -> failure(
                        PLATFORM_FAILURE,
                        "commands.teleport.request.failed"
                ));
    }

    private static TeleportCommandResult failure(
            TeleportCommandStatus status,
            String key,
            Map<String, ?> values
    ) {
        return TeleportCommandResult.failure(status, key, values);
    }

    private static TeleportCommandResult mapTeleport(TeleportResult result) {
        if (result.success()) {
            return TeleportCommandResult.success(
                    result.message().key(),
                    result.message().placeholders()
            );
        }

        var status = switch (result.status()) {
            case UNSAFE_DESTINATION -> UNSAFE_DESTINATION;
            case CROSS_WORLD_DISABLED -> CROSS_WORLD_DISABLED;
            case BACK_PERSISTENCE_FAILURE -> BACK_PERSISTENCE_FAILURE;
            case ROLLBACK_FAILURE -> ROLLBACK_FAILURE;
            case CANCELLED_MOVE -> CANCELLED_MOVE;
            case CANCELLED_DAMAGE -> CANCELLED_DAMAGE;
            case CANCELLED_DEATH -> CANCELLED_DEATH;
            case CANCELLED_DISCONNECT -> CANCELLED_DISCONNECT;
            case CANCELLED_REPLACED -> CANCELLED_REPLACED;
            case PLATFORM_FAILURE -> PLATFORM_FAILURE;
            case SUCCESS -> throw new IllegalStateException("Successful teleport result reported as failure");
        };
        return TeleportCommandResult.failure(
                status,
                result.message().key(),
                result.message().placeholders()
        );
    }

}
