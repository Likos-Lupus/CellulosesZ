package top.likoslupus.cellulosesz.modules.warp.application;

import top.likoslupus.cellulosesz.api.command.execution.CommandOutcome;
import top.likoslupus.cellulosesz.api.player.PlayerLocationPlatformService;
import top.likoslupus.cellulosesz.api.player.PlayerResolver;
import top.likoslupus.cellulosesz.api.teleport.TeleportOptions;
import top.likoslupus.cellulosesz.api.teleport.TeleportService;
import top.likoslupus.cellulosesz.api.text.LocalizedMessage;
import top.likoslupus.cellulosesz.api.text.MessageArguments;
import top.likoslupus.cellulosesz.api.warp.Warp;
import top.likoslupus.cellulosesz.api.warp.WarpService;
import top.likoslupus.cellulosesz.core.command.execution.ServerThreadExecutor;
import top.likoslupus.cellulosesz.core.command.service.CooldownService;
import top.likoslupus.cellulosesz.modules.warp.WarpConfig;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

import static top.likoslupus.cellulosesz.api.validation.Checks.requireNonNegative;
import static top.likoslupus.cellulosesz.api.validation.Checks.requirePositive;

import static java.util.Objects.requireNonNull;

public final class DefaultWarpCommandService implements WarpCommandService {

    private static final String COOLDOWN_KEY = "warp.teleport";

    private final WarpService warps;
    private final TeleportService teleports;
    private final CooldownService cooldowns;
    private final PlayerResolver players;
    private final PlayerLocationPlatformService locations;
    private final ServerThreadExecutor serverThread;
    private volatile Snapshot config;

    public DefaultWarpCommandService(
            WarpService warps,
            TeleportService teleports,
            CooldownService cooldowns,
            PlayerResolver players,
            PlayerLocationPlatformService locations,
            ServerThreadExecutor serverThread,
            WarpConfig config
    ) {
        this.warps = requireNonNull(warps, "warps");
        this.teleports = requireNonNull(teleports, "teleports");
        this.cooldowns = requireNonNull(cooldowns, "cooldowns");
        this.players = requireNonNull(players, "players");
        this.locations = requireNonNull(locations, "locations");
        this.serverThread = requireNonNull(serverThread, "serverThread");
        configure(config);
    }

    @Override
    public CompletableFuture<Result> list(
            int requestedPage,
            Predicate<String> hasPermission
    ) {
        var current = config;
        return warps.warps()
                .handle((available, failure) -> {
                    if (failure != null) {
                        return failed("service.warp.persistence-failed");
                    }

                    var visible = available.stream()
                            .filter(warp -> !current.hideNoPermission()
                                    || allowed(warp, hasPermission)
                            )
                            .sorted(Comparator.comparing(Warp::name))
                            .toList();
                    if (visible.isEmpty()) {
                        return success("commands.warp.list-empty");
                    }

                    var pageSize = current.pageSize();
                    var pages = Math.max(1, (visible.size() + pageSize - 1) / pageSize);
                    if (requestedPage < 1 || requestedPage > pages) {
                        return failure(LocalizedMessage.of(
                                "commands.common.page-out-of-range",
                                MessageArguments.builder().add(pages).build()
                        ));
                    }

                    final int from;
                    try {
                        from = Math.multiplyExact(requestedPage - 1, pageSize);
                    } catch (ArithmeticException _) {
                        return failure(LocalizedMessage.of(
                                "commands.common.page-out-of-range",
                                MessageArguments.builder().add(pages).build()
                        ));
                    }

                    var to = (int) Math.min((long) from + pageSize, visible.size());
                    var names = visible.subList(from, to).stream()
                            .map(Warp::displayName)
                            .toList();

                    return success(LocalizedMessage.of(
                            "commands.warp.list-page",
                            MessageArguments.builder()
                                    .add(requestedPage)
                                    .add(pages)
                                    .add(String.join(", ", names))
                                    .build()
                    ));
                });
    }

    @Override
    public CompletableFuture<Result> teleport(
            Request request,
            String rawName,
            Predicate<String> hasPermission
    ) {
        var name = normalize(rawName);
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
                        "commands.warp.cooldown",
                        MessageArguments.builder().add(seconds).build()
                )));
            }
        }

        final CompletableFuture<Warp> loaded;
        try {
            loaded = warps.warp(name);
        } catch (IllegalArgumentException _) {
            return CompletableFuture.completedFuture(missing(name));
        }

        return loaded
                .handle((found, loadFailure) -> {
                    if (loadFailure != null) {
                        return CompletableFuture.completedFuture(failed(
                                "service.warp.persistence-failed"
                        ));
                    }

                    if (found == null) {
                        return CompletableFuture.completedFuture(missing(name));
                    }

                    var warp = found;
                    if (!allowed(warp, hasPermission)) {
                        return CompletableFuture.completedFuture(failure(
                                "commands.warp.warp-command.error.do-not-permission-use-warp"
                        ));
                    }

                    return teleportLoaded(request, warp, current);
                })
                .thenCompose(stage -> stage);
    }

    @Override
    public CompletableFuture<Result> set(
            Request request,
            String rawName,
            Predicate<String> hasPermission
    ) {
        var name = rawName.trim();
        var invalid = validateName(name);

        if (invalid != null) {
            return CompletableFuture.completedFuture(failure(invalid));
        }

        var key = normalize(name);
        return warps
                .warp(key)
                .thenCompose(existing -> {
                    if (existing != null
                            && !hasPermission.test("cellulosesz.warp.overwrite")
                            && !hasPermission.test("cellulosesz.warp.overwrite." + key)
                    ) {
                        return CompletableFuture.completedFuture(failure(LocalizedMessage.of(
                                "commands.warp.set-warp-command.error.exists",
                                MessageArguments.builder().add(name).build()
                        )));
                    }

                    return serverThread
                            .submit(() -> players.resolveKnown(
                                    request.playerUuid(),
                                    null
                            ).onlinePlayer())
                            .thenCompose(online -> {
                                if (online == null) {
                                    return CompletableFuture.completedFuture(failure(
                                            LocalizedMessage.of(
                                                    "commands.common.player-offline",
                                                    MessageArguments.builder()
                                                            .add(request.playerName())
                                                            .build()
                                            )
                                    ));
                                }

                                return serverThread
                                        .submit(() ->
                                                locations.currentLocation(online)
                                        )
                                        .thenCompose(location ->
                                                warps.setWarp(key, location, request.playerUuid())
                                        )
                                        .thenApply(_ -> success(LocalizedMessage.of(
                                                "commands.warp.set-warp-command.reply.set-warp",
                                                MessageArguments.builder().add(name).build()
                                        )));
                            });
                })
                .exceptionally(_ -> failed("service.warp.persistence-failed"));
    }

    @Override
    public CompletableFuture<Result> delete(String rawName) {
        var name = rawName.trim();
        var invalid = validateName(name);

        if (invalid != null) {
            return CompletableFuture.completedFuture(failure(invalid));
        }

        try {
            return warps
                    .deleteWarp(name)
                    .handle((deleted, failure) -> {
                        if (failure != null) {
                            return failed("service.warp.persistence-failed");
                        }

                        if (!deleted) {
                            return failure(LocalizedMessage.of(
                                    "commands.warp.del-warp-command.error.warp-does-not-exist",
                                    MessageArguments.builder().add(name).build()
                            ));
                        }

                        return success(LocalizedMessage.of(
                                "commands.warp.del-warp-command.reply.deleted-warp",
                                MessageArguments.builder().add(name).build()
                        ));
                    });
        } catch (IllegalArgumentException _) {
            return CompletableFuture.completedFuture(missing(name));
        }
    }

    @Override
    public CompletableFuture<Result> info(String rawName) {
        var name = rawName.trim();
        var invalid = validateName(name);

        if (invalid != null) {
            return CompletableFuture.completedFuture(failure(invalid));
        }

        try {
            return warps
                    .warp(name)
                    .handle((warp, failure) -> {
                        if (failure != null) {
                            return failed("service.warp.persistence-failed");
                        }

                        if (warp == null) {
                            return failure(LocalizedMessage.of(
                                    "commands.warp.warp-info-command.error.warp-does-not-exist",
                                    MessageArguments.builder().add(name).build()
                            ));
                        }

                        var value = warp;
                        return success(LocalizedMessage.of(
                                "commands.warp.warp-info-command.reply.warp-at",
                                MessageArguments.builder()
                                        .add(value.name())
                                        .add(value.location().compact())
                                        .build()
                        ));
                    });
        } catch (IllegalArgumentException _) {
            return CompletableFuture.completedFuture(failure(LocalizedMessage.of(
                    "commands.warp.warp-info-command.error.warp-does-not-exist",
                    MessageArguments.builder().add(name).build()
            )));
        }
    }

    @Override
    public List<String> cachedNames() {
        return warps.cachedWarps().stream()
                .map(Warp::name)
                .sorted()
                .toList();
    }

    @Override
    public List<String> usableNames(Predicate<String> hasPermission) {
        requireNonNull(hasPermission, "hasPermission");
        return warps.cachedWarps().stream()
                .filter(warp -> allowed(warp, hasPermission))
                .map(Warp::name)
                .sorted()
                .toList();
    }

    @Override
    public void configure(WarpConfig candidate) {
        config = Snapshot.from(candidate);
    }

    private @Nullable LocalizedMessage validateName(String name) {
        var current = config;

        if (name.isBlank() || name.length() > current.maxLength()) {
            return LocalizedMessage.of(
                    "commands.warp.abstract-warp-command.error.warp-names-cannot-empty-longer-than-characters",
                    MessageArguments.builder().add(current.maxLength()).build()
            );
        }

        if (!current.pattern().matcher(name).matches()) {
            return LocalizedMessage.of(
                    "commands.warp.abstract-warp-command.error.warp-names-may-only-contain-configured-characters"
            );
        }

        return null;
    }

    private String normalize(String value) {
        return requireNonNull(value, "name").trim().toLowerCase(Locale.ROOT);
    }

    private Result missing(String name) {
        return failure(LocalizedMessage.of(
                "commands.warp.warp-command.error.warp-does-not-exist",
                MessageArguments.builder().add(name).build()
        ));
    }

    private Result failure(String key) {
        return failure(LocalizedMessage.of(key));
    }

    private CompletableFuture<Result> teleportLoaded(
            Request request,
            Warp warp,
            Snapshot current
    ) {
        return serverThread
                .submit(() -> players.resolveKnown(request.playerUuid(), null).onlinePlayer())
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
                            .withWarmup(
                                    request.bypassWarmup()
                                            ? 0
                                            : current.warmupSeconds()
                            );

                    return serverThread
                            .submit(() -> teleports.teleport(
                                    online,
                                    warp.location(),
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
                                        "commands.warp.warp-command.reply.teleported-warp",
                                        MessageArguments.builder()
                                                .add(warp.displayName())
                                                .build()
                                ));
                            });
                })
                .exceptionally(_ -> failed("commands.teleport.request.failed"));
    }

    private Result failed(String key) {
        return new Result(CommandOutcome.Status.FAILED, LocalizedMessage.of(key));
    }

    private boolean allowed(Warp warp, Predicate<String> hasPermission) {
        var permission = warps.requiredPermission(warp);
        return permission == null || hasPermission.test(permission);
    }

    private Result success(String key) {
        return success(LocalizedMessage.of(key));
    }

    private Result failure(LocalizedMessage message) {
        return new Result(false, message);
    }

    private Result success(LocalizedMessage message) {
        return new Result(true, message);
    }

    public void validateConfiguration(WarpConfig candidate) {
        Snapshot.from(candidate);
    }

    private record Snapshot(
            int warmupSeconds,
            int cooldownSeconds,
            boolean safe,
            int pageSize,
            boolean hideNoPermission,
            int maxLength,
            Pattern pattern
    ) {

        static Snapshot from(WarpConfig source) {
            requireNonNull(source, "config");
            requireNonNegative(source.teleport.warmupSeconds, "warmupSeconds");
            requireNonNegative(source.teleport.cooldownSeconds, "cooldownSeconds");
            requirePositive(source.list.pageSize, "pageSize");
            requirePositive(source.naming.maxLength, "maxLength");

            return new Snapshot(
                    source.teleport.warmupSeconds,
                    source.teleport.cooldownSeconds,
                    source.teleport.safe,
                    source.list.pageSize,
                    source.list.hideNoPermission,
                    source.naming.maxLength,
                    Pattern.compile(source.naming.pattern)
            );
        }

    }

}
