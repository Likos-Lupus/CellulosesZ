package top.likoslupus.cellulosesz.modules.playerstate.application;

import top.likoslupus.cellulosesz.api.command.execution.ServerThreadExecutor;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.MovementSpeedType;
import top.likoslupus.cellulosesz.api.playerstate.*;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

/**
 * Coordinates player-state mutations, persistence, server-thread hops, and rollback.
 */
public final class PlayerAbilityCommandService {

    private final PlayerStateService states;
    private final VanishService vanish;
    private final PlayerStatePlatformService platform;
    private final ServerThreadExecutor serverThread;

    public PlayerAbilityCommandService(
            PlayerStateService states,
            VanishService vanish,
            PlayerStatePlatformService platform,
            ServerThreadExecutor serverThread
    ) {
        this.states = requireNonNull(states, "states");
        this.vanish = requireNonNull(vanish, "vanish");
        this.platform = requireNonNull(platform, "platform");
        this.serverThread = requireNonNull(serverThread, "serverThread");
    }

    public CompletableFuture<PlayerStateCommandResult> afk(CellPlayer player) {
        return onServer(() -> !states.afk(player.uuid()))
                .thenCompose(enabled -> states.setAfk(
                        player.uuid(),
                        player.name(),
                        enabled
                ))
                .thenCompose(result ->
                        finish(PlayerStateCommandResult.from(result))
                )
                .exceptionally(_ ->
                        PlayerStateCommandResult.failed("service.user.persistence-failed")
                );
    }

    private <T> CompletableFuture<T> onServer(Supplier<T> operation) {
        return serverThread.submit(operation);
    }

    private CompletableFuture<PlayerStateCommandResult> finish(PlayerStateCommandResult result) {
        return onServer(() -> result);
    }

    public CompletableFuture<PlayerStateCommandResult> fly(
            CellPlayer player,
            Optional<Boolean> requested
    ) {
        return onServer(() -> platform.flying(player))
                .thenCompose(current -> {
                    if (!current.successful() || current.value().isEmpty()) {
                        return finish(PlayerStateCommandResult.failure(
                                "service.playerstate.fly-failed"
                        ));
                    }

                    var enabled = requested.orElse(!current.value().orElseThrow());
                    return states
                            .setFlying(player, enabled)
                            .thenCompose(result ->
                                    finish(PlayerStateCommandResult.from(result))
                            );
                })
                .exceptionally(_ ->
                        PlayerStateCommandResult.failed("service.playerstate.fly-failed")
                );
    }

    public CompletableFuture<PlayerStateCommandResult> god(
            CellPlayer player,
            Optional<Boolean> requested
    ) {
        return onServer(() -> platform.invulnerable(player))
                .thenCompose(current -> {
                    if (!current.successful() || current.value().isEmpty()) {
                        return finish(PlayerStateCommandResult.failure(
                                "service.playerstate.god-failed"
                        ));
                    }

                    var enabled = requested
                            .orElse(!current.value().orElseThrow());
                    return states
                            .setGod(player, enabled)
                            .thenCompose(result -> finish(PlayerStateCommandResult.from(result)));
                })
                .exceptionally(_ -> PlayerStateCommandResult.failed(
                        "service.playerstate.god-failed"
                ));
    }

    public CompletableFuture<PlayerStateCommandResult> vanish(
            CellPlayer player,
            Optional<Boolean> requested
    ) {
        return onServer(() -> requested.orElse(!vanish.vanished(player.uuid())))
                .thenCompose(enabled -> vanish.setVanished(player, enabled))
                .thenCompose(result -> finish(PlayerStateCommandResult.from(result)))
                .exceptionally(_ -> PlayerStateCommandResult.failed(
                        "service.playerstate.vanish-failed"
                ));
    }

    public CompletableFuture<PlayerStateCommandResult> heal(CellPlayer player) {
        return onServer(() -> PlayerStateCommandResult.from(states.heal(player)));
    }

    public CompletableFuture<PlayerStateCommandResult> feed(CellPlayer player) {
        return onServer(() -> PlayerStateCommandResult.from(states.feed(player)));
    }

    public CompletableFuture<PlayerStateCommandResult> experience(CellPlayer player) {
        return onServer(() -> {
            var result = platform.experience(player);
            if (!result.successful() || result.value().isEmpty()) {
                return PlayerStateCommandResult.failure(
                        "commands.playerstate.exp.platform-failed"
                );
            }

            return experienceResult(player, result.value().orElseThrow());
        });
    }

    private static PlayerStateCommandResult experienceResult(
            CellPlayer player,
            ExperienceSnapshot snapshot
    ) {
        return PlayerStateCommandResult.success(
                "commands.playerstate.exp.result",
                Map.of(
                        "player", player.name(),
                        "total", snapshot.totalPoints(),
                        "level", snapshot.level(),
                        "progress", Math.round(snapshot.progress() * 1000.0D) / 10.0D,
                        "next", snapshot.pointsToNextLevel()
                )
        );
    }

    public CompletableFuture<PlayerStateCommandResult> mutateExperience(
            CellPlayer player,
            ExperienceRequest request
    ) {
        return onServer(() -> {
            var result = platform.mutateExperience(player, request);
            if (!result.successful() || result.value().isEmpty()) {
                return PlayerStateCommandResult.failure(
                        "commands.playerstate.exp.platform-failed"
                );
            }

            return experienceResult(player, result.value().orElseThrow());
        });
    }

    public CompletableFuture<PlayerStateCommandResult> gameMode(
            CellPlayer player,
            GameModeKind mode
    ) {
        return onServer(() -> {
            var result = platform.setGameMode(player, mode);
            if (!result.successful() || result.value().isEmpty()) {
                return PlayerStateCommandResult.failure(
                        "commands.playerstate.gamemode-invalid",
                        Map.of("mode", mode.name().toLowerCase(Locale.ROOT))
                );
            }

            var change = result.value().orElseThrow();
            return PlayerStateCommandResult.success(
                    "commands.playerstate.gamemode-set",
                    Map.of(
                            "player", player.name(),
                            "previous", change.previous().name().toLowerCase(Locale.ROOT),
                            "mode", change.current().name().toLowerCase(Locale.ROOT)
                    )
            );
        });
    }

    public CompletableFuture<PlayerStateCommandResult> speedForCurrentMovement(
            CellPlayer player,
            double speed
    ) {
        return onServer(() -> platform.flying(player))
                .thenCompose(flying -> {
                    if (!flying.successful() || flying.value().isEmpty()) {
                        return finish(PlayerStateCommandResult.failure(
                                "commands.playerstate.speed-failed"
                        ));
                    }

                    return speed(
                            player,
                            flying.value().orElseThrow()
                                    ? MovementSpeedType.FLY
                                    : MovementSpeedType.WALK,
                            speed
                    );
                });
    }

    public CompletableFuture<PlayerStateCommandResult> speed(
            CellPlayer player,
            MovementSpeedType type,
            double speed
    ) {
        if (!Double.isFinite(speed)) {
            return CompletableFuture.completedFuture(PlayerStateCommandResult.failure(
                    "commands.playerstate.speed-invalid"
            ));
        }

        return onServer(() -> {
            var result = platform.setMovementSpeed(player, type, speed);
            if (!result.successful() || result.value().isEmpty()) {
                return PlayerStateCommandResult.failure(
                        "commands.playerstate.speed-failed"
                );
            }

            var change = result.value().orElseThrow();
            return PlayerStateCommandResult.success(
                    "commands.playerstate.speed-set",
                    Map.of(
                            "player", player.name(),
                            "speed", change.current(),
                            "type", type.name().toLowerCase(Locale.ROOT)
                    )
            );
        });
    }

    public CompletableFuture<PlayerStateCommandResult> rest(CellPlayer player) {
        return onServer(() ->
                platform.resetRest(player).successful()
                        ?
                        PlayerStateCommandResult.success(
                                "commands.playerstate.rest.success",
                                Map.of("player", player.name())
                        )
                        : PlayerStateCommandResult.failure(
                                "commands.playerstate.rest.failed",
                                Map.of("player", player.name())
                        )
        );
    }

    public CompletableFuture<PlayerStateCommandResult> nick(
            CellPlayer player,
            Optional<String> nickname
    ) {
        return states
                .setNick(
                        player.uuid(),
                        player.name(),
                        nickname
                )
                .thenCompose(result ->
                        finish(PlayerStateCommandResult.from(result))
                )
                .exceptionally(_ -> PlayerStateCommandResult.failed(
                        "service.user.persistence-failed"
                ));
    }

    public CompletableFuture<PlayerStateCommandResult> personalTime(
            CellPlayer player,
            PersonalTimeSetting setting
    ) {
        return states
                .setPersonalTime(player, setting)
                .thenCompose(result ->
                        finish(PlayerStateCommandResult.from(result))
                )
                .exceptionally(_ -> PlayerStateCommandResult.failed(
                        "service.user.persistence-failed"
                ));
    }

    public CompletableFuture<PlayerStateCommandResult> personalWeather(
            CellPlayer player,
            PersonalWeatherSetting setting
    ) {
        return states
                .setPersonalWeather(player, setting)
                .thenCompose(result ->
                        finish(PlayerStateCommandResult.from(result))
                )
                .exceptionally(_ -> PlayerStateCommandResult.failed(
                        "service.user.persistence-failed"
                ));
    }

}
