package top.likoslupus.cellulosesz.modules.home.application;

import top.likoslupus.cellulosesz.api.command.execution.CommandOutcome;
import top.likoslupus.cellulosesz.api.player.PlayerLocationPlatformService;
import top.likoslupus.cellulosesz.api.player.PlayerResolver;
import top.likoslupus.cellulosesz.api.teleport.CellLocation;
import top.likoslupus.cellulosesz.api.teleport.TeleportOptions;
import top.likoslupus.cellulosesz.api.teleport.TeleportService;
import top.likoslupus.cellulosesz.api.text.LocalizedMessage;
import top.likoslupus.cellulosesz.api.text.MessageArguments;
import top.likoslupus.cellulosesz.core.command.execution.ServerThreadExecutor;
import top.likoslupus.cellulosesz.core.command.service.CooldownService;
import top.likoslupus.cellulosesz.modules.home.HomeConfig;
import top.likoslupus.cellulosesz.modules.home.service.HomeService;

import java.time.Duration;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

import static top.likoslupus.cellulosesz.api.validation.Checks.requireNonNegative;
import static top.likoslupus.cellulosesz.api.validation.Checks.requirePositive;

import static java.util.Objects.requireNonNull;

public final class DefaultHomeCommandService implements HomeCommandService {

    private static final String COOLDOWN_KEY = "home.teleport";

    private final HomeService homes;
    private final TeleportService teleports;
    private final CooldownService cooldowns;
    private final PlayerResolver players;
    private final PlayerLocationPlatformService locations;
    private final ServerThreadExecutor serverThread;
    private volatile Snapshot config;

    public DefaultHomeCommandService(
            HomeService homes,
            TeleportService teleports,
            CooldownService cooldowns,
            PlayerResolver players,
            PlayerLocationPlatformService locations,
            ServerThreadExecutor serverThread,
            HomeConfig config
    ) {
        this.homes = requireNonNull(homes, "homes");
        this.teleports = requireNonNull(teleports, "teleports");
        this.cooldowns = requireNonNull(cooldowns, "cooldowns");
        this.players = requireNonNull(players, "players");
        this.locations = requireNonNull(locations, "locations");
        this.serverThread = requireNonNull(serverThread, "serverThread");
        configure(config);
    }

    @Override
    public CompletableFuture<Result> list(UUID playerUuid) {
        return homes.homes(playerUuid).handle((known, failure) -> {
            if (failure != null) {
                return failed("common.persistence-failed");
            }

            if (known.isEmpty()) {
                return success("commands.home.list-empty");
            }

            return success(LocalizedMessage.of(
                    "commands.home.list",
                    MessageArguments.builder()
                            .add(String.join(", ", known.keySet()))
                            .build()
            ));
        });
    }

    @Override
    public CompletableFuture<Result> teleport(Request request, String rawName) {
        var name = normalizeDefault(rawName);
        var current = config;

        if (!request.bypassCooldown()) {
            var remaining = cooldowns.remaining(request.playerUuid(), COOLDOWN_KEY);

            if (!remaining.isZero()) {
                var seconds = Math.max(
                        1L,
                        remaining.toSeconds() + (
                                remaining.toMillisPart() > 0
                                        ? 1
                                        : 0
                        )
                );

                return CompletableFuture.completedFuture(failure(LocalizedMessage.of(
                        "commands.home.cooldown",
                        MessageArguments.builder().add(seconds).build()
                )));
            }
        }

        return homes.home(request.playerUuid(), name)
                .handle((location, loadFailure) -> {
                    if (loadFailure != null) {
                        return CompletableFuture.completedFuture(failed(
                                "common.persistence-failed"
                        ));
                    }

                    if (location.isEmpty()) {
                        return CompletableFuture.completedFuture(failure(LocalizedMessage.of(
                                "commands.home.home-command.error.home-does-not-exist",
                                MessageArguments.builder().add(name).build()
                        )));
                    }

                    return teleportLoaded(request, name, location.orElseThrow(), current);
                })
                .thenCompose(stage -> stage);
    }

    @Override
    public CompletableFuture<Result> set(Request request, String rawName, boolean bypassLimit) {
        var name = normalizeDefault(rawName);
        var invalid = validateName(name);
        if (invalid != null) {
            return CompletableFuture.completedFuture(failure(invalid));
        }

        var current = config;
        return homes.homes(request.playerUuid())
                .thenCompose(existing -> {
                    var key = name.toLowerCase(Locale.ROOT);
                    if (!existing.containsKey(key)
                            && existing.size() >= current.maxHomes()
                            && !bypassLimit
                    ) {
                        return CompletableFuture.completedFuture(failure(LocalizedMessage.of(
                                "commands.home.set-home-command.error.reached-home-limit",
                                MessageArguments.builder().add(current.maxHomes()).build()
                        )));
                    }

                    return serverThread.submit(() ->
                                    players.resolveKnown(request.playerUuid(), null).onlinePlayer()
                            )
                            .thenCompose(online -> {
                                if (online == null) {
                                    return CompletableFuture.completedFuture(failure(
                                            LocalizedMessage.of(
                                                    "commands.common.player-offline",
                                                    MessageArguments.builder()
                                                            .add(request.playerName())
                                                            .build()
                                            )));
                                }

                                return serverThread
                                        .submit(() ->
                                                locations.currentLocation(online)
                                        )
                                        .thenCompose(location ->
                                                homes.setHome(request.playerUuid(), name, location)
                                        )
                                        .thenApply(_ -> success(LocalizedMessage.of(
                                                "commands.home.set-home-command.reply.set-home",
                                                MessageArguments.builder().add(name).build()
                                        )));
                            });
                })
                .exceptionally(_ -> failed("common.persistence-failed"));
    }

    @Override
    public CompletableFuture<Result> delete(UUID playerUuid, String rawName) {
        var name = rawName.trim();
        var invalid = validateName(name);

        if (invalid != null) {
            return CompletableFuture.completedFuture(failure(invalid));
        }

        return homes.deleteHome(playerUuid, name)
                .handle((deleted, failure) -> {
                    if (failure != null) {
                        return failed("common.persistence-failed");
                    }

                    if (!deleted) {
                        return failure(LocalizedMessage.of(
                                "commands.home.del-home-command.error.home-does-not-exist",
                                MessageArguments.builder().add(name).build()
                        ));
                    }

                    return success(LocalizedMessage.of(
                            "commands.home.del-home-command.reply.deleted-home",
                            MessageArguments.builder().add(name).build()
                    ));
                });
    }

    @Override
    public CompletableFuture<Result> rename(
            UUID playerUuid,
            String rawOldName,
            String rawNewName
    ) {
        var oldName = rawOldName.trim();
        var newName = rawNewName.trim();

        var invalidOld = validateName(oldName);
        if (invalidOld != null) {
            return CompletableFuture.completedFuture(failure(invalidOld));
        }

        var invalidNew = validateName(newName);
        if (invalidNew != null) {
            return CompletableFuture.completedFuture(failure(invalidNew));
        }

        return homes.renameHomeDetailed(playerUuid, oldName, newName)
                .handle((status, failure) -> {
                    if (failure != null) {
                        return failed("common.persistence-failed");
                    }

                    return switch (status) {
                        case RENAMED -> success(LocalizedMessage.of(
                                "commands.home.rename-home-command.reply.renamed-home",
                                MessageArguments.builder()
                                        .add(oldName)
                                        .add(newName)
                                        .build()
                        ));
                        case SOURCE_MISSING -> failure(LocalizedMessage.of(
                                "commands.home.rename-home-command.error.source-missing",
                                MessageArguments.builder().add(oldName).build()
                        ));
                        case TARGET_EXISTS -> failure(LocalizedMessage.of(
                                "commands.home.rename-home-command.error.target-exists",
                                MessageArguments.builder().add(newName).build()
                        ));
                    };
                });
    }

    @Override
    public Set<String> cachedNames(UUID playerUuid) {
        return Set.copyOf(homes.cachedHomes(playerUuid).keySet());
    }

    @Override
    public void configure(HomeConfig candidate) {
        config = Snapshot.from(candidate);
    }

    private @Nullable LocalizedMessage validateName(String name) {
        var current = config;
        if (name.length() < current.minLength()
                || name.length() > current.maxLength()
        ) {
            return LocalizedMessage.of(
                    "commands.home.abstract-home-command.error.home-names-must-between-characters-long",
                    MessageArguments.builder()
                            .add(current.minLength())
                            .add(current.maxLength())
                            .build()
            );
        }

        if (!current.pattern().matcher(name).matches()) {
            return LocalizedMessage.of(
                    "commands.home.abstract-home-command.error.home-names-may-only-contain-configured-characters"
            );
        }

        return null;
    }

    private String normalizeDefault(String value) {
        var trimmed = requireNonNull(value, "name").trim();
        return trimmed.isEmpty()
                ? "home"
                : trimmed;
    }

    private Result failure(LocalizedMessage message) {
        return new Result(false, message);
    }

    private CompletableFuture<Result> teleportLoaded(
            Request request,
            String name,
            CellLocation location,
            Snapshot current
    ) {
        return serverThread
                .submit(() ->
                        players.resolveKnown(
                                request.playerUuid(),
                                null
                        ).onlinePlayer()
                )
                .thenCompose(online -> {
                    if (online == null) {
                        return CompletableFuture.completedFuture(failure(LocalizedMessage.of(
                                "commands.common.player-offline",
                                MessageArguments.builder()
                                        .add(request.playerName())
                                        .build()
                        )));
                    }

                    var options = TeleportOptions.defaults()
                            .withSafe(current.safe())
                            .withWarmup(request.bypassWarmup()
                                    ? 0
                                    : current.warmupSeconds());

                    return serverThread
                            .submit(() -> teleports.teleport(
                                    online,
                                    location,
                                    options
                            ))
                            .thenCompose(stage -> stage)
                            .thenApply(result -> {
                                if (!result.success()) {
                                    return failure(result.message());
                                }

                                if (!request.bypassCooldown() && current.cooldownSeconds() > 0) {
                                    cooldowns.start(
                                            request.playerUuid(),
                                            COOLDOWN_KEY,
                                            Duration.ofSeconds(current.cooldownSeconds())
                                    );
                                }

                                return success(LocalizedMessage.of(
                                        "commands.home.home-command.reply.teleported-home",
                                        MessageArguments.builder().add(name).build()
                                ));
                            });
                })
                .exceptionally(_ -> failed("commands.teleport.request.failed"));
    }

    private Result failed(String key) {
        return new Result(CommandOutcome.Status.FAILED, LocalizedMessage.of(key));
    }

    private Result success(String key) {
        return success(LocalizedMessage.of(key));
    }

    private Result success(LocalizedMessage message) {
        return new Result(true, message);
    }

    private Result failure(String key) {
        return failure(LocalizedMessage.of(key));
    }

    private record Snapshot(
            int maxHomes,
            int warmupSeconds,
            int cooldownSeconds,
            boolean safe,
            int minLength,
            int maxLength,
            Pattern pattern
    ) {

        static Snapshot from(HomeConfig source) {
            requireNonNull(source, "config");
            requirePositive(source.limits.defaultMaxHomes, "defaultMaxHomes");
            requireNonNegative(source.teleport.warmupSeconds, "warmupSeconds");
            requireNonNegative(source.teleport.cooldownSeconds, "cooldownSeconds");
            if (source.naming.minLength < 1 || source.naming.maxLength < source.naming.minLength) {
                throw new IllegalArgumentException("invalid home naming bounds");
            }

            return new Snapshot(
                    source.limits.defaultMaxHomes,
                    source.teleport.warmupSeconds,
                    source.teleport.cooldownSeconds,
                    source.teleport.safe,
                    source.naming.minLength,
                    source.naming.maxLength,
                    Pattern.compile(source.naming.pattern)
            );
        }

    }

}
