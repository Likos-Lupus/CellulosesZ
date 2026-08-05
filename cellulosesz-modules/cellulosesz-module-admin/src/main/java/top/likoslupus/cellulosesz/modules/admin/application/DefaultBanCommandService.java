package top.likoslupus.cellulosesz.modules.admin.application;

import top.likoslupus.cellulosesz.api.admin.*;
import top.likoslupus.cellulosesz.api.command.execution.ServerThreadExecutor;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.api.player.PlayerNetworkService;
import top.likoslupus.cellulosesz.api.player.PlayerResolver;
import top.likoslupus.cellulosesz.api.text.MessageArguments;
import top.likoslupus.cellulosesz.modules.admin.command.argument.NetworkTargetInput;
import top.likoslupus.cellulosesz.modules.admin.config.AdminRuntimeSettings;
import top.likoslupus.cellulosesz.modules.admin.service.IpAddresses;

import java.net.InetAddress;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.requireNonNull;

public final class DefaultBanCommandService implements BanCommandService {

    private final BanService bans;
    private final TempBanService temporary;
    private final PlayerResolver resolver;
    private final PlayerDirectory players;
    private final PlayerNetworkService networks;
    private final AddressBookService addresses;
    private final ServerThreadExecutor serverThread;
    private final AdminRuntimeSettings config;

    public DefaultBanCommandService(
            BanService bans,
            TempBanService temporary,
            PlayerResolver resolver,
            PlayerDirectory players,
            PlayerNetworkService networks,
            AddressBookService addresses,
            ServerThreadExecutor serverThread,
            AdminRuntimeSettings config
    ) {
        this.bans = requireNonNull(bans, "bans");
        this.temporary = requireNonNull(temporary, "temporary");
        this.resolver = requireNonNull(resolver, "resolver");
        this.players = requireNonNull(players, "players");
        this.networks = requireNonNull(networks, "networks");
        this.addresses = requireNonNull(addresses, "addresses");
        this.serverThread = requireNonNull(serverThread, "serverThread");
        this.config = requireNonNull(config, "config");
    }

    @Override
    public CompletableFuture<AdminResult> ban(
            String player,
            AdminActor actor,
            String reason
    ) {
        return resolve(player, actor)
                .thenCompose(target -> target.isEmpty()
                        ? notFound(player)
                        : serverThread
                                .submit(() -> bans.ban(
                                        target.orElseThrow().uuid(),
                                        target.orElseThrow().name(),
                                        actor,
                                        reason(reason)
                                ))
                );
    }

    @Override
    public CompletableFuture<AdminResult> unban(String player, AdminActor actor) {
        return resolve(player, actor)
                .thenCompose(target -> {
                    if (target.isEmpty()) {
                        return notFound(player);
                    }

                    var value = target.orElseThrow();
                    return serverThread
                            .submit(() -> bans.unban(
                                    value.uuid(),
                                    value.name(),
                                    actor
                            ))
                            .thenCompose(permanent ->
                                    temporary
                                            .unban(
                                                    value.uuid(),
                                                    value.name(),
                                                    actor
                                            )
                                            .thenApply(temp -> combineUnban(
                                                    permanent,
                                                    temp,
                                                    value.name(),
                                                    "service.admin.unban-success",
                                                    "service.admin.unban-partial",
                                                    "service.admin.unban-not-found",
                                                    "service.admin.unban-failed"
                                            ))
                            );
                });
    }

    @Override
    public CompletableFuture<AdminResult> banIp(
            NetworkTargetInput target,
            AdminActor actor,
            String reason
    ) {
        return address(target, actor)
                .thenCompose(value -> value.isEmpty()
                        ? missingAddress(target)
                        : serverThread
                                .submit(() -> bans.banIp(
                                        value.orElseThrow(),
                                        actor,
                                        reason(reason)
                                ))
                );
    }

    @Override
    public CompletableFuture<AdminResult> unbanIp(InetAddress address, AdminActor actor) {
        var canonical = IpAddresses.canonical(address);
        return serverThread
                .submit(() -> bans.unbanIp(address, actor))
                .thenCompose(permanent -> temporary.unbanIp(address, actor)
                        .thenApply(temp -> combineUnban(
                                permanent,
                                temp,
                                canonical,
                                "service.admin.unban-ip-success",
                                "service.admin.unban-ip-partial",
                                "service.admin.unban-ip-not-found",
                                "service.admin.unban-ip-failed"
                        ))
                );
    }

    @Override
    public CompletableFuture<AdminResult> tempBan(
            String player,
            AdminActor actor,
            Duration duration,
            String reason
    ) {
        return resolve(player, actor)
                .thenCompose(target -> target.isEmpty()
                        ? notFound(player)
                        : temporary.tempBan(
                                target.orElseThrow().uuid(),
                                target.orElseThrow().name(),
                                actor,
                                duration,
                                reason(reason)
                        )
                );
    }

    @Override
    public CompletableFuture<AdminResult> tempBanIp(
            NetworkTargetInput target,
            AdminActor actor,
            Duration duration,
            String reason
    ) {
        return address(target, actor)
                .thenCompose(value -> value.isEmpty()
                        ? missingAddress(target)
                        : temporary.tempBanIp(
                                value.orElseThrow(),
                                actor,
                                duration,
                                reason(reason)
                        )
                );
    }

    private CompletableFuture<Optional<InetAddress>> address(
            NetworkTargetInput input,
            AdminActor actor
    ) {
        if (input instanceof NetworkTargetInput.Address(InetAddress address)) {
            return CompletableFuture.completedFuture(Optional.of(address));
        }

        var name = ((NetworkTargetInput.PlayerName) input).name();
        return resolver
                .resolve(
                        name,
                        actor.uuid()
                                .flatMap(players::onlinePlayer)
                                .orElse(null)
                ).thenApply(resolved -> {
                    if (resolved.optionalUuid().isEmpty()) {
                        return Optional.empty();
                    }

                    var online = resolved.online()
                            .flatMap(networks::address);
                    return online
                            .or(() -> addresses.address(resolved.optionalUuid().orElseThrow()))
                            .or(() -> addresses.address(resolved.name()));
                });
    }

    private static CompletableFuture<AdminResult> missingAddress(NetworkTargetInput target) {
        var label = target instanceof NetworkTargetInput.PlayerName(String name)
                ? name
                : "address";
        return CompletableFuture.completedFuture(AdminResult.failure(
                AdminStatus.NOT_FOUND,
                "service.admin.address-not-found",
                MessageArguments.builder().add(label).build()
        ));
    }

    private static AdminResult combineUnban(
            AdminResult permanent,
            AdminResult temporary,
            String targetValue,
            String successKey,
            String partialKey,
            String notFoundKey,
            String failureKey
    ) {
        var components = List.of(permanent, temporary);
        var targetArguments = MessageArguments.builder()
                .add(targetValue)
                .build();

        if (permanent.status() == AdminStatus.SUCCESS
                && temporary.status() == AdminStatus.SUCCESS
        ) {
            return AdminResult.success(successKey, targetArguments, components);
        }

        if (permanent.success() || temporary.success()) {
            return AdminResult.partial(
                    partialKey,
                    MessageArguments.builder()
                            .add(targetValue)
                            .add(permanent.status().name())
                            .add(temporary.status().name())
                            .build(),
                    components
            );
        }

        if (permanent.status() == AdminStatus.NOT_FOUND
                && temporary.status() == AdminStatus.NOT_FOUND
        ) {
            return AdminResult.failure(
                    AdminStatus.NOT_FOUND,
                    notFoundKey,
                    targetArguments,
                    components
            );
        }

        return AdminResult.failure(
                combinedFailureStatus(permanent.status(), temporary.status()),
                failureKey,
                targetArguments,
                components
        );
    }

    private static AdminStatus combinedFailureStatus(
            AdminStatus permanent,
            AdminStatus temporary
    ) {
        if (permanent == AdminStatus.PERSISTENCE_FAILURE
                || temporary == AdminStatus.PERSISTENCE_FAILURE
        ) {
            return AdminStatus.PERSISTENCE_FAILURE;
        }

        if (permanent == AdminStatus.PLATFORM_FAILURE
                || temporary == AdminStatus.PLATFORM_FAILURE
                || permanent == AdminStatus.NATIVE_COMMAND_FAILURE
                || temporary == AdminStatus.NATIVE_COMMAND_FAILURE
        ) {
            return AdminStatus.PLATFORM_FAILURE;
        }

        if (permanent == AdminStatus.INVALID_INPUT
                || temporary == AdminStatus.INVALID_INPUT
        ) {
            return AdminStatus.INVALID_INPUT;
        }

        return AdminStatus.FAILURE;
    }

    private CompletableFuture<Optional<Target>> resolve(
            String input,
            AdminActor actor
    ) {
        return resolver
                .resolve(
                        input,
                        actor.uuid()
                                .flatMap(players::onlinePlayer)
                                .orElse(null)
                )
                .thenApply(value -> value.optionalUuid().map(
                        uuid -> new Target(uuid, value.name())
                ));
    }

    private static CompletableFuture<AdminResult> notFound(String player) {
        return CompletableFuture.completedFuture(AdminResult.failure(
                AdminStatus.NOT_FOUND,
                "commands.common.player-not-found",
                MessageArguments.builder().add(player).build()
        ));
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

    private record Target(
            UUID uuid,
            String name
    ) {

    }

}
