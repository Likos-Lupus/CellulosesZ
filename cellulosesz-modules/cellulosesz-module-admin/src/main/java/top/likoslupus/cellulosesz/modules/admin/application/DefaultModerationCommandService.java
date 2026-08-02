package top.likoslupus.cellulosesz.modules.admin.application;

import top.likoslupus.cellulosesz.api.admin.*;
import top.likoslupus.cellulosesz.api.command.execution.ServerThreadExecutor;
import top.likoslupus.cellulosesz.api.permission.PermissionService;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.api.player.PlayerResolver;
import top.likoslupus.cellulosesz.modules.admin.config.AdminRuntimeSettings;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.requireNonNull;

public final class DefaultModerationCommandService implements ModerationCommandService {

    private final BanService bans;
    private final MuteService mutes;
    private final PlayerDirectory players;
    private final PlayerResolver resolver;
    private final PermissionService permissions;
    private final ServerThreadExecutor serverThread;
    private final Clock clock;
    private final AdminRuntimeSettings config;

    public DefaultModerationCommandService(
            BanService bans,
            MuteService mutes,
            PlayerDirectory players,
            PlayerResolver resolver,
            PermissionService permissions,
            ServerThreadExecutor serverThread,
            Clock clock,
            AdminRuntimeSettings config
    ) {
        this.bans = requireNonNull(bans, "bans");
        this.mutes = requireNonNull(mutes, "mutes");
        this.players = requireNonNull(players, "players");
        this.resolver = requireNonNull(resolver, "resolver");
        this.permissions = requireNonNull(permissions, "permissions");
        this.serverThread = requireNonNull(serverThread, "serverThread");
        this.clock = requireNonNull(clock, "clock");
        this.config = requireNonNull(config, "config");
    }

    @Override
    public CompletableFuture<AdminResult> kick(
            String player,
            AdminActor actor,
            String reason
    ) {
        return serverThread
                .submit(() -> players.onlinePlayer(player)
                        .map(value -> bans.kick(value, reason(reason)))
                        .orElseGet(() -> AdminResult.failure(
                                AdminStatus.NOT_FOUND,
                                "service.admin.kick-failed",
                                Map.of("player", player)
                        ))
                );
    }

    @Override
    public CompletableFuture<AdminResult> kickAll(AdminActor actor, String reason) {
        return serverThread.submit(() -> {
            var snapshot = players.onlinePlayers();

            var kicked = 0;
            var exempt = 0;
            var offline = 0;
            var failed = 0;

            for (var player : snapshot) {
                if (actor.uuid().filter(player.uuid()::equals).isPresent()
                        || permissions.has(player, "cellulosesz.admin.kickall.exempt")
                ) {
                    exempt++;
                    continue;
                }

                var result = bans.kick(player, reason(reason));
                if (result.success()) {
                    kicked++;
                } else {
                    failed++;
                }
            }

            var values = Map.<String, Object>of(
                    "kicked", kicked,
                    "exempt", exempt,
                    "alreadyOffline", offline,
                    "failed", failed
            );

            return failed == 0
                    ? AdminResult.success("service.admin.kick-all-success", values)
                    : kicked > 0
                            ? AdminResult.partial("service.admin.kick-all-partial", values)
                            : AdminResult.failure(
                                    AdminStatus.PLATFORM_FAILURE,
                                    "service.admin.kick-all-failed",
                                    values
                            );
        });
    }

    @Override
    public CompletableFuture<AdminResult> mute(
            String player,
            AdminActor actor,
            Optional<Duration> duration,
            String reason
    ) {
        return resolver.resolve(
                player,
                actor.uuid()
                        .flatMap(players::onlinePlayer)
                        .orElse(null)
        ).thenCompose(value -> {
            if (value.optionalUuid().isEmpty()) {
                return completed(AdminResult.failure(
                        AdminStatus.NOT_FOUND,
                        "commands.common.player-not-found",
                        Map.of("player", player)
                ));
            }

            final Expiration expiration;
            if (duration.isPresent()) {
                expiration = Expiration.after(
                        clock.instant(),
                        duration.orElseThrow()
                );
            } else if (config.defaultMuteSeconds() <= 0) {
                expiration = Expiration.permanent();
            } else {
                expiration = Expiration.after(
                        clock.instant(),
                        Duration.ofSeconds(config.defaultMuteSeconds())
                );
            }

            return mutes.mute(
                    value.optionalUuid().orElseThrow(),
                    value.name(),
                    actor,
                    expiration,
                    reason(reason)
            );
        });
    }

    @Override
    public CompletableFuture<AdminResult> unmute(String player, AdminActor actor) {
        return resolver.resolve(
                        player,
                        actor.uuid().flatMap(players::onlinePlayer).orElse(null)
                )
                .thenCompose(value -> value.optionalUuid().isEmpty()
                        ?
                        completed(AdminResult.failure(
                                AdminStatus.NOT_FOUND,
                                "commands.common.player-not-found",
                                Map.of("player", player)
                        ))
                        : mutes.unmute(
                                value.optionalUuid().orElseThrow(),
                                value.name(),
                                actor
                        )
                );
    }

    private static CompletableFuture<AdminResult> completed(AdminResult value) {
        return CompletableFuture.completedFuture(value);
    }

    private String reason(String input) {
        var value = input.trim();

        if (value.isBlank()) {
            value = config.defaultReason();
        }

        if (value.length() > config.maximumReasonLength()
                || value.chars().anyMatch(Character::isISOControl)
        ) {
            throw new IllegalArgumentException("invalid reason");
        }

        return value;
    }

}
