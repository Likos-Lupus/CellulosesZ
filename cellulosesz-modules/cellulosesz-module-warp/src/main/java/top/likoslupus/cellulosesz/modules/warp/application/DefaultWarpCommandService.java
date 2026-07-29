package top.likoslupus.cellulosesz.modules.warp.application;

import org.jspecify.annotations.Nullable;
import top.likoslupus.cellulosesz.api.command.execution.ServerThreadExecutor;
import top.likoslupus.cellulosesz.api.command.service.CooldownService;
import top.likoslupus.cellulosesz.api.player.PlayerLocationPlatformService;
import top.likoslupus.cellulosesz.api.player.PlayerResolver;
import top.likoslupus.cellulosesz.api.teleport.TeleportOptions;
import top.likoslupus.cellulosesz.api.teleport.TeleportService;
import top.likoslupus.cellulosesz.api.text.LocalizedMessage;
import top.likoslupus.cellulosesz.api.warp.Warp;
import top.likoslupus.cellulosesz.api.warp.WarpService;
import top.likoslupus.cellulosesz.core.i18n.GeneratedMessageKeys;
import top.likoslupus.cellulosesz.modules.warp.WarpConfig;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;
import static top.likoslupus.cellulosesz.api.validation.Checks.requireNonNegative;
import static top.likoslupus.cellulosesz.api.validation.Checks.requirePositive;

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
    public CompletableFuture<Result> list(int requestedPage, Predicate<String> hasPermission) {
        var current = config;
        return warps.warps()
                .handle((available, failure) -> {
                    if (failure != null) {
                        return failure(GeneratedMessageKeys.SERVICE_WARP_PERSISTENCE_FAILED);
                    }

                    var visible = available.stream()
                            .filter(warp -> !current.hideNoPermission() || allowed(warp, hasPermission))
                            .sorted(Comparator.comparing(warp -> warp.name))
                            .toList();
                    if (visible.isEmpty()) {
                        return success(GeneratedMessageKeys.COMMANDS_WARP_LIST_EMPTY);
                    }

                    var pageSize = current.pageSize();
                    var pages = Math.max(1, (visible.size() + pageSize - 1) / pageSize);
                    if (requestedPage < 1 || requestedPage > pages) {
                        return failure(LocalizedMessage.of(
                                GeneratedMessageKeys.COMMANDS_COMMON_PAGE_OUT_OF_RANGE,
                                Map.of("pages", pages)
                        ));
                    }

                    final int from;
                    try {
                        from = Math.multiplyExact(requestedPage - 1, pageSize);
                    } catch (ArithmeticException _) {
                        return failure(LocalizedMessage.of(
                                GeneratedMessageKeys.COMMANDS_COMMON_PAGE_OUT_OF_RANGE,
                                Map.of("pages", pages)
                        ));
                    }

                    var to = (int) Math.min((long) from + pageSize, visible.size());
                    var names = visible.subList(from, to).stream()
                            .map(warp -> warp.displayName)
                            .toList();

                    return success(LocalizedMessage.of(
                            GeneratedMessageKeys.COMMANDS_WARP_LIST_PAGE,
                            Map.of(
                                    "warps", String.join(", ", names),
                                    "page", requestedPage,
                                    "pages", pages
                            )
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
                        remaining.toSeconds() + (remaining.toMillisPart() > 0 ? 1 : 0)
                );
                return CompletableFuture.completedFuture(failure(LocalizedMessage.of(
                        GeneratedMessageKeys.COMMANDS_WARP_COOLDOWN,
                        Map.of("seconds", seconds)
                )));
            }
        }

        final CompletableFuture<java.util.Optional<Warp>> loaded;
        try {
            loaded = warps.warp(name);
        } catch (IllegalArgumentException _) {
            return CompletableFuture.completedFuture(missing(name));
        }

        return loaded
                .handle((found, loadFailure) -> {
                    if (loadFailure != null) {
                        return CompletableFuture.completedFuture(failure(
                                GeneratedMessageKeys.SERVICE_WARP_PERSISTENCE_FAILED
                        ));
                    }

                    if (found.isEmpty()) {
                        return CompletableFuture.completedFuture(missing(name));
                    }

                    var warp = found.orElseThrow();
                    if (!allowed(warp, hasPermission)) {
                        return CompletableFuture.completedFuture(failure(
                                GeneratedMessageKeys.COMMANDS_WARP_WARP_COMMAND_ERROR_DO_NOT_PERMISSION_USE_WARP
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
        return warps.warp(key)
                .thenCompose(existing -> {
                    if (existing.isPresent()
                            && !hasPermission.test("cellulosesz.warp.overwrite")
                            && !hasPermission.test("cellulosesz.warp.overwrite." + key)
                    ) {
                        return CompletableFuture.completedFuture(failure(LocalizedMessage.of(
                                GeneratedMessageKeys.COMMANDS_WARP_SET_WARP_COMMAND_ERROR_EXISTS,
                                Map.of("warp", name)
                        )));
                    }

                    return serverThread
                            .submit(() -> players.resolveKnown(
                                    request.playerUuid(),
                                    null
                            ).online())
                            .thenCompose(online -> {
                                if (online.isEmpty()) {
                                    return CompletableFuture.completedFuture(failure(LocalizedMessage.of(
                                            GeneratedMessageKeys.COMMANDS_COMMON_PLAYER_OFFLINE,
                                            Map.of("player", request.playerName())
                                    )));
                                }

                                return serverThread
                                        .submit(() -> locations.currentLocation(online.orElseThrow()))
                                        .thenCompose(location ->
                                                warps.setWarp(key, location, request.playerUuid())
                                        )
                                        .thenApply(_ -> success(LocalizedMessage.of(
                                                GeneratedMessageKeys.COMMANDS_WARP_SET_WARP_COMMAND_REPLY_SET_WARP,
                                                Map.of("warp", name)
                                        )));
                            });
                })
                .exceptionally(_ -> failure(GeneratedMessageKeys.SERVICE_WARP_PERSISTENCE_FAILED));
    }

    @Override
    public CompletableFuture<Result> delete(String rawName) {
        var name = rawName.trim();
        var invalid = validateName(name);
        if (invalid != null) {
            return CompletableFuture.completedFuture(failure(invalid));
        }

        try {
            return warps.deleteWarp(name)
                    .handle((deleted, failure) -> {
                        if (failure != null) {
                            return failure(GeneratedMessageKeys.SERVICE_WARP_PERSISTENCE_FAILED);
                        }

                        if (!deleted) {
                            return failure(LocalizedMessage.of(
                                    GeneratedMessageKeys.COMMANDS_WARP_DEL_WARP_COMMAND_ERROR_WARP_DOES_NOT_EXIST,
                                    Map.of("warp", name)
                            ));
                        }

                        return success(LocalizedMessage.of(
                                GeneratedMessageKeys.COMMANDS_WARP_DEL_WARP_COMMAND_REPLY_DELETED_WARP,
                                Map.of("warp", name)
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
        if (invalid != null) return CompletableFuture.completedFuture(failure(invalid));

        try {
            return warps.warp(name)
                    .handle((warp, failure) -> {
                        if (failure != null) {
                            return failure(GeneratedMessageKeys.SERVICE_WARP_PERSISTENCE_FAILED);
                        }

                        if (warp.isEmpty()) {
                            return failure(LocalizedMessage.of(
                                    GeneratedMessageKeys.COMMANDS_WARP_WARP_INFO_COMMAND_ERROR_WARP_DOES_NOT_EXIST,
                                    Map.of("warp", name)
                            ));
                        }

                        var value = warp.orElseThrow();
                        return success(LocalizedMessage.of(
                                GeneratedMessageKeys.COMMANDS_WARP_WARP_INFO_COMMAND_REPLY_WARP_AT,
                                Map.of(
                                        "warp", value.name,
                                        "location", value.location.compact()
                                )
                        ));
                    });
        } catch (IllegalArgumentException _) {
            return CompletableFuture.completedFuture(failure(LocalizedMessage.of(
                    GeneratedMessageKeys.COMMANDS_WARP_WARP_INFO_COMMAND_ERROR_WARP_DOES_NOT_EXIST,
                    Map.of("warp", name)
            )));
        }
    }

    @Override
    public List<String> cachedNames() {
        return warps.cachedWarps().stream()
                .map(warp -> warp.name)
                .sorted()
                .toList();
    }

    @Override
    public List<String> usableNames(Predicate<String> hasPermission) {
        requireNonNull(hasPermission, "hasPermission");
        return warps.cachedWarps().stream()
                .filter(warp -> allowed(warp, hasPermission))
                .map(warp -> warp.name)
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
                    GeneratedMessageKeys.COMMANDS_WARP_ABSTRACT_WARP_COMMAND_ERROR_WARP_NAMES_CANNOT_EMPTY_LONGER_THAN_CHARACTERS,
                    Map.of("maximum_length", current.maxLength())
            );
        }

        if (!current.pattern().matcher(name).matches()) {
            return LocalizedMessage.of(
                    GeneratedMessageKeys.COMMANDS_WARP_ABSTRACT_WARP_COMMAND_ERROR_WARP_NAMES_MAY_ONLY_CONTAIN_CONFIGURED_CHARACTERS
            );
        }

        return null;
    }

    private String normalize(String value) {
        return requireNonNull(value, "name").trim().toLowerCase(Locale.ROOT);
    }

    private Result missing(String name) {
        return failure(LocalizedMessage.of(
                GeneratedMessageKeys.COMMANDS_WARP_WARP_COMMAND_ERROR_WARP_DOES_NOT_EXIST,
                Map.of("warp", name)
        ));
    }

    private CompletableFuture<Result> teleportLoaded(
            Request request,
            Warp warp,
            Snapshot current
    ) {
        return serverThread
                .submit(() -> players.resolveKnown(request.playerUuid(), null).online())
                .thenCompose(online -> {
                    if (online.isEmpty()) {
                        return CompletableFuture.completedFuture(failure(LocalizedMessage.of(
                                GeneratedMessageKeys.COMMANDS_COMMON_PLAYER_OFFLINE,
                                Map.of("player", request.playerName())
                        )));
                    }

                    var options = new TeleportOptions()
                            .safe(current.safe())
                            .warmupSeconds(
                                    request.bypassWarmup()
                                            ? 0
                                            : current.warmupSeconds()
                            );

                    return serverThread
                            .submit(() -> teleports.teleport(
                                    online.orElseThrow(),
                                    warp.location,
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
                                        GeneratedMessageKeys.COMMANDS_WARP_WARP_COMMAND_REPLY_TELEPORTED_WARP,
                                        Map.of("target", warp.displayName)
                                ));
                            });
                })
                .exceptionally(_ -> failure(GeneratedMessageKeys.COMMANDS_TELEPORT_REQUEST_FAILED));
    }

    private Result failure(String key) {
        return failure(LocalizedMessage.of(key));
    }

    private boolean allowed(Warp warp, Predicate<String> hasPermission) {
        return warps.requiredPermission(warp)
                .map(hasPermission::test)
                .orElse(true);
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
