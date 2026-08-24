package top.likoslupus.cellulosesz.modules.playerstate.application;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.playerstate.*;
import top.likoslupus.cellulosesz.api.text.MessageArguments;
import top.likoslupus.cellulosesz.common.platform.MovementSpeedType;
import top.likoslupus.cellulosesz.common.playerstate.ExperienceRequest;
import top.likoslupus.cellulosesz.common.playerstate.ExperienceSnapshot;
import top.likoslupus.cellulosesz.common.playerstate.PlayerStatePlatformService;
import top.likoslupus.cellulosesz.core.command.execution.ServerThreadExecutor;

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
                    var currentValue = current.value();
                    if (!current.successful() || currentValue == null) {
                        return finish(PlayerStateCommandResult.failure(
                                "service.playerstate.fly-failed"
                        ));
                    }

                    var enabled = requested.orElse(!currentValue);
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
                    var currentValue = current.value();
                    if (!current.successful() || currentValue == null) {
                        return finish(PlayerStateCommandResult.failure(
                                "service.playerstate.god-failed"
                        ));
                    }

                    var enabled = requested
                            .orElse(!currentValue);
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
            if (!result.successful() || result.value() == null) {
                return PlayerStateCommandResult.failure(
                        "commands.playerstate.exp.platform-failed"
                );
            }

            return experienceResult(player, result.value());
        });
    }

    private static PlayerStateCommandResult experienceResult(
            CellPlayer player,
            ExperienceSnapshot snapshot
    ) {
        return PlayerStateCommandResult.success(
                "commands.playerstate.exp.result",
                MessageArguments.builder()
                        .add(player.name())
                        .add(snapshot.totalPoints())
                        .add(snapshot.level())
                        .add(Math.round(snapshot.progress() * 1000.0D) / 10.0D)
                        .add(snapshot.pointsToNextLevel())
                        .build()
        );
    }

    public CompletableFuture<PlayerStateCommandResult> mutateExperience(
            CellPlayer player,
            ExperienceRequest request
    ) {
        return onServer(() -> {
            var result = platform.mutateExperience(player, request);
            if (!result.successful() || result.value() == null) {
                return PlayerStateCommandResult.failure(
                        "commands.playerstate.exp.platform-failed"
                );
            }

            return experienceResult(player, result.value());
        });
    }

    public CompletableFuture<PlayerStateCommandResult> gameMode(
            CellPlayer player,
            GameModeKind mode
    ) {
        return onServer(() -> {
            var result = platform.setGameMode(player, mode);
            if (!result.successful() || result.value() == null) {
                return PlayerStateCommandResult.failure(
                        "commands.playerstate.gamemode-invalid",
                        MessageArguments.empty()
                );
            }

            var change = result.value();
            return PlayerStateCommandResult.success(
                    "commands.playerstate.gamemode-set",
                    MessageArguments.empty()
            );
        });
    }

    public CompletableFuture<PlayerStateCommandResult> speedForCurrentMovement(
            CellPlayer player,
            double speed
    ) {
        return onServer(() -> platform.flying(player))
                .thenCompose(flying -> {
                    var flyingValue = flying.value();
                    if (!flying.successful() || flyingValue == null) {
                        return finish(PlayerStateCommandResult.failure(
                                "commands.playerstate.speed-failed"
                        ));
                    }

                    return speed(
                            player,
                            flyingValue
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
            if (!result.successful() || result.value() == null) {
                return PlayerStateCommandResult.failure(
                        "commands.playerstate.speed-failed"
                );
            }

            return PlayerStateCommandResult.success(
                    "commands.playerstate.speed-set",
                    MessageArguments.empty()
            );
        });
    }

    public CompletableFuture<PlayerStateCommandResult> rest(CellPlayer player) {
        return onServer(() ->
                platform.resetRest(player).successful()
                        ?
                        PlayerStateCommandResult.success(
                                "commands.playerstate.rest.success",
                                MessageArguments.builder().add(player.name()).build()
                        )
                        : PlayerStateCommandResult.failure(
                                "commands.playerstate.rest.failed",
                                MessageArguments.builder().add(player.name()).build()
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
                        nickname.orElse(null)
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
