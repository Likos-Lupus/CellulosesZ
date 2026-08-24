package top.likoslupus.cellulosesz.modules.playerstate.application;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.player.DisplayNameService;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.api.player.PlayerLocationPlatformService;
import top.likoslupus.cellulosesz.api.player.PlayerResolver;
import top.likoslupus.cellulosesz.api.playerstate.VanishService;
import top.likoslupus.cellulosesz.api.teleport.CellLocation;
import top.likoslupus.cellulosesz.api.text.LocalizedMessage;
import top.likoslupus.cellulosesz.api.text.MessageArguments;
import top.likoslupus.cellulosesz.api.user.UserService;
import top.likoslupus.cellulosesz.common.playerstate.PlayerStatePlatformService;
import top.likoslupus.cellulosesz.core.command.execution.ServerThreadExecutor;

import java.text.Normalizer;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

import static java.util.Objects.requireNonNull;

/**
 * Read-only player information queries with privacy filtering and stable snapshots.
 */
public final class PlayerInformationCommandService {

    private static final String[] DIRECTION_KEYS = {
            "south",
            "south-west",
            "west",
            "north-west",
            "north",
            "north-east",
            "east",
            "south-east"
    };
    private static final Pattern LEGACY = Pattern.compile("(?i)[§&][0-9A-FK-ORX]");
    private static final Pattern MINI_TAG = Pattern.compile("<[^>]{1,64}>");

    private final PlayerDirectory players;
    private final PlayerLocationPlatformService locations;
    private final PlayerStatePlatformService statePlatform;
    private final PlayerResolver resolver;
    private final UserService users;
    private final VanishService vanish;
    private final DisplayNameService displayNames;
    private final ServerThreadExecutor serverThread;
    private volatile PlayerStateCommandSettings settings;

    public PlayerInformationCommandService(
            PlayerDirectory players,
            PlayerLocationPlatformService locations,
            PlayerStatePlatformService statePlatform,
            PlayerResolver resolver,
            UserService users,
            VanishService vanish,
            DisplayNameService displayNames,
            ServerThreadExecutor serverThread,
            PlayerStateCommandSettings settings
    ) {
        this.players = requireNonNull(players, "players");
        this.locations = requireNonNull(locations, "locations");
        this.statePlatform = requireNonNull(statePlatform, "statePlatform");
        this.resolver = requireNonNull(resolver, "resolver");
        this.users = requireNonNull(users, "users");
        this.vanish = requireNonNull(vanish, "vanish");
        this.displayNames = requireNonNull(displayNames, "displayNames");
        this.serverThread = requireNonNull(serverThread, "serverThread");
        this.settings = requireNonNull(settings, "settings");
    }

    public void configure(PlayerStateCommandSettings replacement) {
        settings = requireNonNull(replacement, "replacement");
    }

    public CompletableFuture<PlayerStateCommandResult> compass(CellPlayer player) {
        return serverThread.submit(() -> {
            var degrees = normalizeDegrees(locations.currentLocation(player).yaw());
            var direction = DIRECTION_KEYS[(int) Math.floor((degrees + 22.5D) / 45.0D) & 7];

            return PlayerStateCommandResult.success(
                    "commands.playerstate.compass." + direction,
                    MessageArguments.builder()
                            .add(Math.round(degrees * 10.0D) / 10.0D)
                            .build()
            );
        });
    }

    public static double normalizeDegrees(double yaw) {
        var value = yaw % 360.0D;
        return value < 0.0D
                ? value + 360.0D
                : value;
    }

    public CompletableFuture<PlayerStateCommandResult> depth(CellPlayer player) {
        return serverThread
                .submit(() -> {
                    var sea = statePlatform.seaLevel(player);
                    if (!sea.successful() || sea.value() == null) {
                        return PlayerStateCommandResult.failure(
                                "commands.playerstate.depth.platform-failed"
                        );
                    }

                    var seaLevel = sea.value();
                    var y = (int) Math.floor(locations.currentLocation(player).y());
                    var difference = y - seaLevel;
                    var key = difference > 0
                            ? "commands.playerstate.depth.above"
                            : difference < 0
                                    ? "commands.playerstate.depth.below"
                                    : "commands.playerstate.depth.equal";

                    var arguments = difference == 0
                            ?
                            MessageArguments.builder()
                                    .add(y)
                                    .add(seaLevel)
                                    .build()
                            : MessageArguments.builder()
                                    .add(Math.abs(difference))
                                    .add(y)
                                    .add(seaLevel)
                                    .build();
                    return PlayerStateCommandResult.success(key, arguments);
                });
    }

    public CompletableFuture<PlayerStateCommandResult> getPos(
            Optional<CellPlayer> viewer,
            CellPlayer target
    ) {
        return serverThread.submit(() -> {
            if (viewer.isPresent()
                    && !vanish.canSee(viewer.orElseThrow(), target.uuid())
            ) {
                return PlayerStateCommandResult.failure(
                        "commands.common.unknown-player",
                        MessageArguments.builder().add(target.name()).build()
                );
            }

            var location = locations.currentLocation(target);
            var distance = viewer
                    .filter(value -> !value.uuid().equals(target.uuid()))
                    .map(locations::currentLocation)
                    .filter(value -> value.world().equals(location.world()))
                    .map(value -> Math.sqrt(
                            square(value.x() - location.x())
                                    + square(value.y() - location.y())
                                    + square(value.z() - location.z())
                    ));

            return PlayerStateCommandResult.success(
                    "commands.playerstate.getpos.result",
                    MessageArguments.builder()
                            .add(target.name())
                            .add(location.world())
                            .add((int) Math.floor(location.x()))
                            .add((int) Math.floor(location.y()))
                            .add((int) Math.floor(location.z()))
                            .add(round(location.x()))
                            .add(round(location.y()))
                            .add(round(location.z()))
                            .add(round(location.yaw()))
                            .add(round(location.pitch()))
                            .add(
                                    distance
                                            .map(PlayerInformationCommandService::round)
                                            .map(String::valueOf)
                                            .orElse("-")
                            )
                            .build()
            );
        });
    }

    private static double square(double value) {
        return value * value;
    }

    private static double round(double value) {
        return Math.round(value * 100.0D) / 100.0D;
    }

    public CompletableFuture<PlayerStateCommandResult> near(CellPlayer viewer, int radius) {
        return serverThread
                .submit(() -> {
                    var origin = locations.currentLocation(viewer);
                    var maximumSquared = (double) radius * radius;
                    var rows = players.onlinePlayers().stream()
                            .filter(target -> !target.uuid().equals(viewer.uuid()))
                            .filter(target -> vanish.canSee(viewer, target.uuid()))
                            .map(target -> new LocatedPlayer(
                                    target,
                                    locations.currentLocation(target)
                            ))
                            .filter(entry -> entry.location().world().equals(origin.world()))
                            .map(entry -> new NearbyPlayer(
                                    entry.player(),
                                    distanceSquared(origin, entry.location())
                            ))
                            .filter(entry -> entry.distanceSquared() <= maximumSquared)
                            .sorted(Comparator.comparingDouble(NearbyPlayer::distanceSquared)
                                    .thenComparing(
                                            entry -> entry.player().name(),
                                            String.CASE_INSENSITIVE_ORDER
                                    )
                            )
                            .limit(settings.maximumNearResults())
                            .map(entry -> LocalizedMessage.of(
                                    "commands.playerstate.near-entry",
                                    MessageArguments.builder()
                                            .add(displayNames.displayName(entry.player()))
                                            .add(
                                                    Math.round(Math.sqrt(entry.distanceSquared()))
                                            )
                                            .build()
                            ))
                            .toList();

                    if (rows.isEmpty()) {
                        return PlayerStateCommandResult.success(
                                "commands.playerstate.near-empty",
                                MessageArguments.empty()
                        );
                    }

                    var messages = new ArrayList<LocalizedMessage>();
                    messages.add(LocalizedMessage.of(
                            "commands.playerstate.near-header",
                            MessageArguments.builder()
                                    .add(rows.size())
                                    .add(radius)
                                    .build()
                    ));
                    messages.addAll(rows);
                    return PlayerStateCommandResult.success(messages);
                });
    }

    private static double distanceSquared(CellLocation first, CellLocation second) {
        return square(first.x() - second.x())
                + square(first.y() - second.y())
                + square(first.z() - second.z());
    }

    public CompletableFuture<PlayerStateCommandResult> realName(
            Optional<CellPlayer> viewer,
            String rawQuery
    ) {
        return serverThread
                .submit(() -> {
                    var query = normalize(rawQuery);
                    if (query.isEmpty()) {
                        return PlayerStateCommandResult.failure(
                                "commands.playerstate.realname.invalid-query"
                        );
                    }

                    var matches = players.onlinePlayers().stream()
                            .filter(target -> viewer.isEmpty()
                                    || vanish.canSee(viewer.orElseThrow(), target.uuid())
                            )
                            .filter(target ->
                                    normalize(displayNames.plainDisplayName(target))
                                            .contains(query)
                            )
                            .sorted(Comparator.comparing(target ->
                                    target.name().toLowerCase(Locale.ROOT)
                            ))
                            .limit(settings.maximumRealNameResults())
                            .toList();

                    if (matches.isEmpty()) {
                        return PlayerStateCommandResult.failure(
                                "commands.playerstate.realname.none",
                                MessageArguments.builder().add(rawQuery).build()
                        );
                    }

                    var messages = new ArrayList<LocalizedMessage>();
                    messages.add(LocalizedMessage.of(
                            "commands.playerstate.realname.header",
                            MessageArguments.builder().add(matches.size()).build()
                    ));
                    matches.forEach(target -> messages.add(LocalizedMessage.of(
                            "commands.playerstate.realname.entry",
                            MessageArguments.builder()
                                    .add(displayNames.displayName(target))
                                    .add(target.name())
                                    .build()
                    )));
                    return PlayerStateCommandResult.success(messages);
                });
    }

    public static String normalize(String value) {
        var plain = MINI_TAG
                .matcher(LEGACY.matcher(value).replaceAll(""))
                .replaceAll("");

        return Normalizer
                .normalize(plain, Normalizer.Form.NFKC)
                .strip()
                .toLowerCase(Locale.ROOT);
    }

    public CompletableFuture<PlayerStateCommandResult> playtime(
            Optional<CellPlayer> viewer,
            String input
    ) {
        return resolver
                .resolve(input, viewer.orElse(null))
                .thenCompose(resolved -> {
                    if (resolved.uuid() == null
                            || resolved.vanished()
                            && viewer.isPresent()
                            &&
                            !vanish.canSee(
                                    viewer.orElseThrow(),
                                    resolved.uuid()
                            )
                    ) {
                        return finish(PlayerStateCommandResult.failure(
                                "commands.common.unknown-player",
                                MessageArguments.builder().add(input).build()
                        ));
                    }

                    return users
                            .load(resolved.uuid())
                            .thenCompose(user -> finish(playtimeResult(
                                    resolved.name(),
                                    user.timestamps().playTimeMillis(),
                                    user.timestamps().activeSessionStartedAt(),
                                    System.currentTimeMillis()
                            )))
                            .exceptionally(_ -> PlayerStateCommandResult.failed(
                                    "service.user.load-failed"
                            ));

                });
    }

    private CompletableFuture<PlayerStateCommandResult> finish(PlayerStateCommandResult result) {
        return serverThread
                .submit(() -> result);
    }

    private static PlayerStateCommandResult playtimeResult(
            String name,
            long stored,
            @Nullable Long activeStarted,
            long now
    ) {
        var total = saturatedPlaytime(stored, activeStarted, now);
        return PlayerStateCommandResult.success(
                "commands.playerstate.playtime",
                MessageArguments.builder()
                        .add(name)
                        .add(duration(total))
                        .build()
        );
    }

    private static long saturatedPlaytime(
            long stored,
            @Nullable Long activeStarted,
            long now
    ) {
        if (activeStarted == null) {
            return Math.max(0L, stored);
        }

        var increment = Math.max(0L, now - activeStarted);
        try {
            return Math.addExact(Math.max(0L, stored), increment);
        } catch (ArithmeticException _) {
            return Long.MAX_VALUE;
        }
    }

    public static String duration(long millis) {
        var seconds = Math.max(0L, millis / 1000L);
        var days = seconds / 86_400L;
        var hours = seconds % 86_400L / 3_600L;
        var minutes = seconds % 3_600L / 60L;
        var remaining = seconds % 60L;
        return "%dd %02dh %02dm %02ds".formatted(days, hours, minutes, remaining);
    }

    public CompletableFuture<PlayerStateCommandResult> seen(
            Optional<CellPlayer> viewer,
            String input
    ) {
        return resolver
                .resolve(input, viewer.orElse(null))
                .thenCompose(resolved -> {
                    if (resolved.uuid() == null) {
                        return finish(PlayerStateCommandResult.failure(
                                "commands.common.unknown-player",
                                MessageArguments.builder().add(input).build()
                        ));
                    }

                    var uuid = resolved.uuid();
                    var visibleOnline = resolved.onlinePlayer() != null
                            && (viewer.isEmpty() || vanish.canSee(viewer.orElseThrow(), uuid));
                    if (visibleOnline) {
                        return finish(PlayerStateCommandResult.success(
                                "commands.playerstate.seen-online",
                                MessageArguments.empty()
                        ));
                    }

                    return users.load(uuid).thenCompose(user -> {
                                if (user.timestamps().lastQuit() <= 0L) {
                                    return finish(PlayerStateCommandResult.success(
                                            "commands.playerstate.seen-never",
                                            MessageArguments.empty()
                                    ));
                                }

                                return finish(PlayerStateCommandResult.success(
                                        "commands.playerstate.seen-offline",
                                        MessageArguments.empty()
                                ));
                            })
                            .exceptionally(_ -> PlayerStateCommandResult.failed(
                                    "service.user.load-failed"
                            ));
                });
    }

    public CompletableFuture<PlayerStateCommandResult> whois(
            Optional<CellPlayer> viewer,
            String input,
            boolean showUuid
    ) {
        return resolver
                .resolve(input, viewer.orElse(null))
                .thenCompose(resolved -> {
                    if (resolved.uuid() == null) {
                        return finish(PlayerStateCommandResult.failure(
                                "commands.common.unknown-player",
                                MessageArguments.builder().add(input).build()
                        ));
                    }

                    var uuid = resolved.uuid();
                    if (resolved.vanished()
                            && viewer.isPresent()
                            && !vanish.canSee(viewer.orElseThrow(), uuid)
                    ) {
                        return finish(PlayerStateCommandResult.failure(
                                "commands.common.unknown-player",
                                MessageArguments.builder().add(input).build()
                        ));
                    }

                    return users.load(uuid).thenCompose(user -> {
                                var total = saturatedPlaytime(
                                        user.timestamps().playTimeMillis(),
                                        user.timestamps().activeSessionStartedAt(),
                                        System.currentTimeMillis()
                                );

                                return finish(PlayerStateCommandResult.success(
                                        "commands.playerstate.whois",
                                        MessageArguments.builder()
                                                .add(resolved.name())
                                                .add(
                                                        showUuid
                                                                ? uuid.toString()
                                                                : "-"
                                                )
                                                .add(resolved.onlinePlayer() != null)
                                                .add(user.state().afk())
                                                .add(
                                                        instantOrUnknown(user.timestamps().firstJoin())
                                                )
                                                .add(
                                                        instantOrUnknown(user.timestamps().lastJoin())
                                                )
                                                .add(
                                                        instantOrUnknown(user.timestamps().lastQuit())
                                                )
                                                .add(duration(total))
                                                .add(
                                                        user.state().nickname() == null
                                                                ? "-"
                                                                : user.state().nickname()
                                                )
                                                .build()
                                ));
                            })
                            .exceptionally(_ -> PlayerStateCommandResult.failed(
                                    "service.user.load-failed"
                            ));
                });
    }

    private static String instantOrUnknown(long value) {
        return value <= 0L
                ? "unknown"
                : Instant.ofEpochMilli(value).toString();
    }

    public List<String> visibleOnlineNames(Optional<CellPlayer> viewer) {
        return players.onlinePlayers().stream()
                .filter(target -> viewer.isEmpty()
                        || vanish.canSee(viewer.orElseThrow(), target.uuid())
                )
                .map(CellPlayer::name)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    private record LocatedPlayer(
            CellPlayer player,
            CellLocation location
    ) {

    }

    private record NearbyPlayer(
            CellPlayer player,
            double distanceSquared
    ) {

    }

}
