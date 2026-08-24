package top.likoslupus.cellulosesz.modules.teleport.application;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.player.PlayerLocationPlatformService;
import top.likoslupus.cellulosesz.api.teleport.*;
import top.likoslupus.cellulosesz.api.text.MessageArguments;
import top.likoslupus.cellulosesz.api.world.WorldDirectory;
import top.likoslupus.cellulosesz.core.command.execution.ServerThreadExecutor;
import top.likoslupus.cellulosesz.modules.teleport.TeleportRuntimeSettings;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static top.likoslupus.cellulosesz.modules.teleport.application.TeleportCommandStatus.*;

import static java.util.Objects.requireNonNull;

public final class DefaultRandomTeleportCommandService implements RandomTeleportCommandService {

    private final RandomTeleportSettingsService settings;
    private final RandomTeleportService random;
    private final TeleportService teleports;
    private final PlayerLocationPlatformService locations;
    private final WorldDirectory worlds;
    private final ServerThreadExecutor serverThread;
    private final TeleportRuntimeSettings runtimeSettings;

    public DefaultRandomTeleportCommandService(
            RandomTeleportSettingsService settings,
            RandomTeleportService random,
            TeleportService teleports,
            PlayerLocationPlatformService locations,
            WorldDirectory worlds,
            ServerThreadExecutor serverThread,
            TeleportRuntimeSettings runtimeSettings
    ) {
        this.settings = requireNonNull(settings, "settings");
        this.random = requireNonNull(random, "random");
        this.teleports = requireNonNull(teleports, "teleports");
        this.locations = requireNonNull(locations, "locations");
        this.worlds = requireNonNull(worlds, "worlds");
        this.serverThread = requireNonNull(serverThread, "serverThread");
        this.runtimeSettings = requireNonNull(runtimeSettings, "runtimeSettings");
    }

    @Override
    public CompletableFuture<TeleportCommandResult> center(
            Optional<CellPlayer> actor, String world,
            Optional<Coordinates> coordinates
    ) {
        var resolved = worlds.resolveLoadedWorld(world);
        if (resolved == null) {
            return completed(TeleportCommandResult.failure(
                    NOT_FOUND,
                    "commands.teleport.world-not-found"
            ));
        }

        if (coordinates.isPresent()) {
            return saveCenter(resolved, coordinates.orElseThrow());
        }

        if (actor.isEmpty()) {
            return completed(TeleportCommandResult.failure(
                    INVALID_INPUT,
                    "common.player-only"
            ));
        }

        return serverThread
                .submit(() -> locations.currentLocation(actor.orElseThrow()))
                .thenCompose(location -> saveCenter(
                        resolved,
                        new Coordinates(location.x(), location.z())
                ));
    }

    @Override
    public CompletableFuture<TeleportCommandResult> minimum(
            String world,
            Optional<Integer> radius
    ) {
        return radius(world, radius, true);
    }

    @Override
    public CompletableFuture<TeleportCommandResult> maximum(
            String world,
            Optional<Integer> radius
    ) {
        return radius(world, radius, false);
    }

    @Override
    public CompletableFuture<TeleportCommandResult> random(CellPlayer player) {
        return serverThread
                .submit(() -> {
                    var location = locations.currentLocation(player);
                    return new WorldAndResult(
                            location.world(),
                            random.randomLocation(
                                    location.world(),
                                    settings.settings(location.world())
                            )
                    );
                })
                .thenCompose(value -> {
                    if (!value.result().success() || value.result().location() == null) {
                        return completed(TeleportCommandResult.failure(
                                PLATFORM_FAILURE,
                                value.result().status() == RandomTeleportStatus.WORLD_NOT_FOUND
                                        ? "commands.teleport.world-not-found"
                                        : "commands.teleport.random-no-safe-location"
                        ));
                    }

                    return teleports
                            .teleport(
                                    player,
                                    value.result().location(),
                                    TeleportOptions
                                            .defaults()
                                            .withWarmup(runtimeSettings.warmupSeconds())
                            )
                            .thenApply(DefaultRandomTeleportCommandService::mapTeleport);
                });
    }

    private static TeleportCommandResult mapTeleport(TeleportResult result) {
        if (result.success()) {
            return TeleportCommandResult.success(
                    result.message().key(),
                    result.message().arguments()
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
            case SUCCESS -> throw new IllegalStateException(
                    "Successful teleport result reported as failure"
            );
        };

        return TeleportCommandResult.failure(
                status,
                result.message().key(),
                result.message().arguments()
        );
    }

    private CompletableFuture<TeleportCommandResult> radius(
            String world,
            Optional<Integer> radius,
            boolean minimum
    ) {
        var resolved = worlds.resolveLoadedWorld(world);
        if (resolved == null) {
            return completed(TeleportCommandResult.failure(
                    NOT_FOUND,
                    "commands.teleport.world-not-found"
            ));
        }

        var current = settings.settings(resolved);
        if (radius.isEmpty()) {
            return completed(TeleportCommandResult.success(
                    minimum
                            ? "commands.teleport.set-tpr-command.reply.minrange"
                            : "commands.teleport.set-tpr-command.reply.maxrange",
                    MessageArguments.builder()
                            .add(resolved)
                            .add(
                                    minimum
                                            ? current.minRadius()
                                            : current.maxRadius()
                            ).build()
            ));
        }

        var value = (int) radius.orElseThrow();
        if (value < 0
                || minimum && value >= current.maxRadius()
                || !minimum && value <= current.minRadius()
        ) {
            return completed(TeleportCommandResult.failure(
                    INVALID_INPUT,
                    "commands.teleport.set-tpr-command.error.invalid-range"
            ));
        }

        var mutation = minimum
                ? settings.setMinimumRadius(resolved, value)
                : settings.setMaximumRadius(resolved, value);
        return mutation
                .thenApply(_ -> TeleportCommandResult.success(
                        minimum
                                ? "commands.teleport.set-tpr-command.reply.minrange"
                                : "commands.teleport.set-tpr-command.reply.maxrange",
                        MessageArguments.builder()
                                .add(resolved)
                                .add(value)
                                .build()
                ))
                .exceptionally(_ -> TeleportCommandResult.failure(
                        PERSISTENCE_FAILURE,
                        "commands.teleport.random-settings-persistence-failed"
                ));
    }

    private static CompletableFuture<TeleportCommandResult> completed(TeleportCommandResult value) {
        return CompletableFuture.completedFuture(value);
    }

    private CompletableFuture<TeleportCommandResult> saveCenter(
            String world,
            Coordinates coordinates
    ) {
        return settings
                .setCenter(
                        world,
                        coordinates.x(),
                        coordinates.z()
                )
                .thenApply(_ -> TeleportCommandResult.success(
                        "commands.teleport.set-tpr-command.reply.center",
                        MessageArguments.builder()
                                .add(world)
                                .add(coordinates.x())
                                .add(coordinates.z())
                                .build()
                ))
                .exceptionally(_ -> TeleportCommandResult.failure(
                        PERSISTENCE_FAILURE,
                        "commands.teleport.random-settings-persistence-failed"
                ));
    }

    private record WorldAndResult(
            String world,
            RandomTeleportResult result
    ) {

    }

}
