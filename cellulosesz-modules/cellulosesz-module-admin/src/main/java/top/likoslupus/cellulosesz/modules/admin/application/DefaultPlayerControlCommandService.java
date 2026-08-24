package top.likoslupus.cellulosesz.modules.admin.application;

import top.likoslupus.cellulosesz.api.permission.PermissionService;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.api.text.MessageArguments;
import top.likoslupus.cellulosesz.common.command.service.CommandDispatchOrigin;
import top.likoslupus.cellulosesz.common.command.service.PlayerCommandDispatchRequest;
import top.likoslupus.cellulosesz.common.command.service.PlayerCommandDispatchService;
import top.likoslupus.cellulosesz.common.playerstate.KillKind;
import top.likoslupus.cellulosesz.common.playerstate.PlayerStatePlatformService;
import top.likoslupus.cellulosesz.modules.admin.config.AdminRuntimeSettings;
import top.likoslupus.cellulosesz.modules.admin.domain.AdminActor;
import top.likoslupus.cellulosesz.modules.admin.domain.AdminResult;
import top.likoslupus.cellulosesz.modules.admin.domain.AdminStatus;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.requireNonNull;

public final class DefaultPlayerControlCommandService implements PlayerControlCommandService {

    private final PlayerDirectory players;
    private final PlayerStatePlatformService states;
    private final PermissionService permissions;
    private final PlayerCommandDispatchService dispatch;
    private final AdminRuntimeSettings config;

    public DefaultPlayerControlCommandService(
            PlayerDirectory players,
            PlayerStatePlatformService states,
            PermissionService permissions,
            PlayerCommandDispatchService dispatch,
            AdminRuntimeSettings config
    ) {
        this.players = requireNonNull(players, "players");
        this.states = requireNonNull(states, "states");
        this.permissions = requireNonNull(permissions, "permissions");
        this.dispatch = requireNonNull(dispatch, "dispatch");
        this.config = requireNonNull(config, "config");
    }

    @Override
    public CompletableFuture<AdminResult> burn(String player, int seconds) {
        var target = Optional.ofNullable(players.onlinePlayer(player));
        if (target.isEmpty()) {
            return completed(notFound(player));
        }

        final int ticks;
        try {
            ticks = Math.multiplyExact(seconds, 20);
        } catch (ArithmeticException failure) {
            return completed(AdminResult.failure(
                    AdminStatus.INVALID_INPUT,
                    "commands.admin.burn.invalid-seconds",
                    MessageArguments.builder().add(config.maximumBurnSeconds()).build()
            ));
        }

        var result = seconds == 0
                ? states.extinguish(target.orElseThrow())
                : states.setFireTicks(target.orElseThrow(), ticks);
        return completed(result.successful()
                ?
                AdminResult.success(
                        seconds == 0
                                ? "commands.admin.burn.extinguished"
                                : "commands.admin.burn.success",
                        MessageArguments
                                .builder()
                                .add(target.orElseThrow().name())
                                .add(seconds)
                                .build()
                )
                : AdminResult.failure(
                        AdminStatus.PLATFORM_FAILURE,
                        "service.admin.platform-failed"
                )
        );
    }

    @Override
    public CompletableFuture<AdminResult> extinguish(
            Optional<CellPlayer> actor,
            Optional<String> target
    ) {
        var value = target
                .flatMap(name -> Optional.ofNullable(players.onlinePlayer(name)))
                .or(() -> actor);
        if (value.isEmpty()) {
            return completed(AdminResult.failure(
                    AdminStatus.INVALID_INPUT,
                    "common.player-only"
            ));
        }

        var result = states.extinguish(value.orElseThrow());
        return completed(result.successful()
                ?
                AdminResult.success(
                        "commands.admin.ext.success",
                        MessageArguments.builder()
                                .add(value.orElseThrow().name())
                                .build()
                )
                : AdminResult.failure(
                        AdminStatus.PLATFORM_FAILURE,
                        "service.admin.platform-failed"
                )
        );
    }

    @Override
    public CompletableFuture<AdminResult> ice(Optional<CellPlayer> actor, Optional<String> target) {
        var value = target
                .flatMap(name -> Optional.ofNullable(players.onlinePlayer(name)))
                .or(() -> actor);

        if (value.isEmpty()) {
            return completed(AdminResult.failure(
                    AdminStatus.INVALID_INPUT,
                    "common.player-only"
            ));
        }

        var result = states.freeze(value.orElseThrow());
        var frozenTicks = result.value();
        return completed(result.successful()
                ?
                AdminResult.success(
                        "commands.admin.ice.success",
                        MessageArguments.builder()
                                .add(value.orElseThrow().name())
                                .add(requireNonNull(frozenTicks))
                                .build()
                )
                : AdminResult.failure(
                        AdminStatus.PLATFORM_FAILURE,
                        "service.admin.platform-failed"
                ));
    }

    @Override
    public CompletableFuture<AdminResult> kill(String player, boolean force) {
        var target = Optional.ofNullable(players.onlinePlayer(player));
        if (target.isEmpty()) {
            return completed(notFound(player));
        }

        if (!force && permissions.has(target.orElseThrow(), "cellulosesz.command.kill.exempt")) {
            return completed(AdminResult.failure(
                    AdminStatus.FAILURE,
                    "commands.admin.kill.exempt",
                    MessageArguments.builder().add(target.orElseThrow().name()).build()
            ));
        }

        var result = states.kill(
                target.orElseThrow(),
                KillKind.ADMIN,
                force
        );
        return completed(result.successful()
                ?
                AdminResult.success(
                        "commands.admin.kill.success",
                        MessageArguments
                                .builder()
                                .add(target.orElseThrow().name())
                                .build()
                )
                : AdminResult.failure(
                        AdminStatus.PLATFORM_FAILURE,
                        "commands.admin.kill.failed",
                        MessageArguments
                                .builder()
                                .add(target.orElseThrow().name())
                                .build()
                )
        );
    }

    @Override
    public CompletableFuture<AdminResult> suicide(CellPlayer player) {
        var result = states.kill(
                player,
                KillKind.SUICIDE,
                false
        );

        return completed(result.successful()
                ?
                AdminResult.success(
                        "commands.admin.suicide.success",
                        MessageArguments.builder().add(player.name()).build()
                )
                : AdminResult.failure(
                        AdminStatus.PLATFORM_FAILURE,
                        "commands.admin.suicide.failed",
                        MessageArguments.builder()
                                .add(result.detail().isBlank()
                                        ? result.status().name().toLowerCase()
                                        : result.detail()
                                )
                                .build()
                )
        );
    }

    @Override
    public CompletableFuture<AdminResult> sudo(
            AdminActor actor,
            String player,
            String command
    ) {
        var target = Optional.ofNullable(players.onlinePlayer(player));
        if (target.isEmpty()) {
            return completed(notFound(player));
        }

        if (actor.uuid()
                .filter(target.orElseThrow().uuid()::equals)
                .isPresent()
        ) {
            return completed(AdminResult.failure(
                    AdminStatus.INVALID_INPUT,
                    "commands.admin.sudo.self"
            ));
        }

        if (permissions.has(target.orElseThrow(), "cellulosesz.command.sudo.exempt")) {
            return completed(AdminResult.failure(
                    AdminStatus.FAILURE,
                    "commands.admin.sudo.exempt",
                    MessageArguments.builder().add(target.orElseThrow().name()).build()
            ));
        }

        var value = requireNonNull(command, "command");
        if (value.startsWith("/")) {
            value = value.substring(1);
        }

        if (value.isBlank()
                || value.length() > config.sudoMaximumCommandLength()
                || value.chars().anyMatch(Character::isISOControl)
        ) {
            return completed(AdminResult.failure(
                    AdminStatus.INVALID_INPUT,
                    "commands.admin.sudo.invalid-command",
                    MessageArguments.builder().add(config.sudoMaximumCommandLength()).build()
            ));
        }

        if (value.regionMatches(
                true,
                0,
                "c:",
                0,
                2
        )) {
            return completed(AdminResult.failure(
                    AdminStatus.INVALID_INPUT,
                    "commands.admin.sudo.chat-unsupported"
            ));
        }

        var result = dispatch.dispatch(PlayerCommandDispatchRequest.start(
                target.orElseThrow(),
                actor.uuid().orElse(PlayerCommandDispatchRequest.CONSOLE_ACTOR_ID),
                CommandDispatchOrigin.SUDO,
                value
        ));
        return completed(result.successful()
                ?
                AdminResult.success(
                        "commands.admin.sudo.success",
                        MessageArguments
                                .builder()
                                .add(target.orElseThrow().name())
                                .add(result.commandResult())
                                .build()
                )
                : AdminResult.failure(
                        AdminStatus.PLATFORM_FAILURE,
                        "commands.admin.sudo.failed",
                        MessageArguments
                                .builder()
                                .add(target.orElseThrow().name())
                                .build()
                )
        );
    }

    private static CompletableFuture<AdminResult> completed(AdminResult value) {
        return CompletableFuture.completedFuture(value);
    }

    private static AdminResult notFound(String player) {
        return AdminResult.failure(
                AdminStatus.NOT_FOUND,
                "commands.common.unknown-player",
                MessageArguments.builder().add(player).build()
        );
    }

}
