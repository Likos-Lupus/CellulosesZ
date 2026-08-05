package top.likoslupus.cellulosesz.modules.admin.application;

import top.likoslupus.cellulosesz.api.admin.*;
import top.likoslupus.cellulosesz.api.command.execution.ServerThreadExecutor;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.api.player.PlayerLocationPlatformService;
import top.likoslupus.cellulosesz.api.player.PlayerResolver;
import top.likoslupus.cellulosesz.api.text.MessageArguments;
import top.likoslupus.cellulosesz.modules.admin.config.AdminRuntimeSettings;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.requireNonNull;

public final class DefaultJailCommandService implements JailCommandService {

    private final JailService jails;
    private final PlayerDirectory players;
    private final PlayerResolver resolver;
    private final PlayerLocationPlatformService locations;
    private final ServerThreadExecutor serverThread;
    private final Clock clock;
    private final AdminRuntimeSettings config;

    public DefaultJailCommandService(
            JailService jails,
            PlayerDirectory players,
            PlayerResolver resolver,
            PlayerLocationPlatformService locations,
            ServerThreadExecutor serverThread,
            Clock clock,
            AdminRuntimeSettings config
    ) {
        this.jails = requireNonNull(jails, "jails");
        this.players = requireNonNull(players, "players");
        this.resolver = requireNonNull(resolver, "resolver");
        this.locations = requireNonNull(locations, "locations");
        this.serverThread = requireNonNull(serverThread, "serverThread");
        this.clock = requireNonNull(clock, "clock");
        this.config = requireNonNull(config, "config");
    }

    @Override
    public CompletableFuture<AdminResult> set(CellPlayer actor, String name) {
        return serverThread
                .submit(() -> locations.currentLocation(actor))
                .thenCompose(location -> jails.setJail(
                        name,
                        location,
                        AdminActor.player(actor)
                ));
    }

    @Override
    public CompletableFuture<AdminResult> delete(String name) {
        return jails.deleteJail(name);
    }

    @Override
    public List<Jail> jails() {
        return jails.jails();
    }

    @Override
    public List<JailedPlayer> jailedPlayers() {
        return jails.jailedPlayers();
    }

    @Override
    public CompletableFuture<AdminResult> jail(
            String player,
            String jail,
            AdminActor actor,
            Optional<Duration> duration,
            String reason
    ) {
        var target = players.onlinePlayer(player);
        if (target.isEmpty()) {
            return completed(AdminResult.failure(
                    AdminStatus.NOT_FOUND,
                    "commands.common.unknown-player",
                    MessageArguments.builder().add(player).build()
            ));
        }

        final Expiration expiration;
        if (duration.isPresent()) {
            expiration = Expiration.after(
                    clock.instant(),
                    duration.orElseThrow()
            );
        } else if (config.defaultJailSeconds() <= 0) {
            expiration = Expiration.permanent();
        } else {
            expiration = Expiration.after(
                    clock.instant(),
                    Duration.ofSeconds(config.defaultJailSeconds())
            );
        }

        return jails.jailPlayer(
                target.orElseThrow(),
                jail,
                actor,
                expiration,
                reason(reason)
        );
    }

    @Override
    public CompletableFuture<AdminResult> unjail(String player, AdminActor actor) {
        return resolver.resolve(
                        player,
                        actor.uuid()
                                .flatMap(players::onlinePlayer)
                                .orElse(null)
                )
                .thenCompose(value -> value.optionalUuid().isEmpty()
                        ?
                        completed(AdminResult.failure(
                                AdminStatus.NOT_FOUND,
                                "commands.common.player-not-found",
                                MessageArguments.builder().add(player).build()
                        ))
                        : jails.unjail(
                                value.optionalUuid().orElseThrow(),
                                value.name(),
                                actor
                        ));
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
