package top.likoslupus.cellulosesz.modules.teleport.application;

import top.likoslupus.cellulosesz.api.command.execution.ServerThreadExecutor;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.api.player.PlayerLocationPlatformService;
import top.likoslupus.cellulosesz.api.player.PlayerResolver;
import top.likoslupus.cellulosesz.api.playerstate.VanishService;
import top.likoslupus.cellulosesz.api.teleport.*;
import top.likoslupus.cellulosesz.api.text.MessageRenderer;
import top.likoslupus.cellulosesz.api.text.PlayerAudienceService;
import top.likoslupus.cellulosesz.api.user.UserService;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.requireNonNull;
import static top.likoslupus.cellulosesz.api.validation.Checks.requireNonNegative;
import static top.likoslupus.cellulosesz.api.validation.Checks.requirePositive;
import static top.likoslupus.cellulosesz.modules.teleport.application.TeleportCommandStatus.*;

public final class DefaultTeleportRequestCommandService implements TeleportRequestCommandService {

    private final TeleportService teleports;
    private final TeleportRequestService requests;
    private final UserService users;
    private final PlayerDirectory players;
    private final PlayerResolver resolver;
    private final PlayerLocationPlatformService locations;
    private final PlayerAudienceService audience;
    private final MessageRenderer renderer;
    private final ServerThreadExecutor serverThread;
    private final Optional<VanishService> vanish;
    private final int timeoutSeconds;
    private final int warmupSeconds;
    private final int maximumBulkTargets;

    public DefaultTeleportRequestCommandService(
            TeleportService teleports,
            TeleportRequestService requests,
            UserService users,
            PlayerDirectory players,
            PlayerResolver resolver,
            PlayerLocationPlatformService locations,
            PlayerAudienceService audience,
            MessageRenderer renderer,
            ServerThreadExecutor serverThread,
            Optional<VanishService> vanish,
            int timeoutSeconds,
            int warmupSeconds,
            int maximumBulkTargets
    ) {
        this.teleports = requireNonNull(teleports, "teleports");
        this.requests = requireNonNull(requests, "requests");
        this.users = requireNonNull(users, "users");
        this.players = requireNonNull(players, "players");
        this.resolver = requireNonNull(resolver, "resolver");
        this.locations = requireNonNull(locations, "locations");
        this.audience = requireNonNull(audience, "audience");
        this.renderer = requireNonNull(renderer, "renderer");
        this.serverThread = requireNonNull(serverThread, "serverThread");
        this.vanish = requireNonNull(vanish, "vanish");
        this.timeoutSeconds = requirePositive(timeoutSeconds, "timeoutSeconds");
        this.warmupSeconds = requireNonNegative(warmupSeconds, "warmupSeconds");
        this.maximumBulkTargets = requirePositive(maximumBulkTargets, "maximumBulkTargets");
    }

    @Override
    public CompletableFuture<TeleportCommandResult> create(
            CellPlayer requester,
            String targetName,
            TeleportRequestType type,
            boolean bypassPreference
    ) {
        var target = players.onlinePlayer(targetName);

        if (target.isEmpty()) {
            return completed(failure(
                    NOT_FOUND,
                    "commands.common.player-offline",
                    Map.of("player", targetName)
            ));
        }

        if (target.orElseThrow().uuid().equals(requester.uuid())) {
            return completed(failure(
                    INVALID_INPUT,
                    "commands.teleport.request.self"
            ));
        }

        return allowed(target.orElseThrow(), bypassPreference)
                .thenCompose(allowed -> {
                    if (!allowed) {
                        return completed(failure(
                                BLOCKED,
                                "commands.teleport.request.blocked",
                                Map.of("player", target.orElseThrow().name())
                        ));
                    }

                    var creation = requests.create(
                            requester,
                            target.orElseThrow(),
                            type,
                            timeoutSeconds
                    );
                    if (!creation.created()) {
                        return completed(failure(
                                REQUEST_CHANGED,
                                "commands.teleport.request.already-pending",
                                Map.of("request", creation.request().id())
                        ));
                    }

                    notify(
                            target.orElseThrow(),
                            type == TeleportRequestType.REQUESTER_TO_TARGET
                                    ? "commands.teleport.request.received-tpa"
                                    : "commands.teleport.request.received-tpahere",
                            Map.of(
                                    "player", requester.name(),
                                    "seconds", timeoutSeconds,
                                    "request", creation.request().id()
                            )
                    );

                    users.load(target.orElseThrow().uuid())
                            .thenAccept(user -> {
                                if (user.preferences.teleportAutoAccept) {
                                    accept(
                                            target.orElseThrow(),
                                            Optional.of(new TeleportRequestSelector.RequestId(creation.request().id())),
                                            true
                                    );
                                }
                            });

                    return completed(TeleportCommandResult.success(
                            "commands.teleport.request.sent",
                            Map.of(
                                    "player", target.orElseThrow().name(),
                                    "request", creation.request().id()
                            )
                    ));
                });
    }

    @Override
    public CompletableFuture<TeleportCommandResult> createAll(
            CellPlayer requester,
            boolean bypassPreference
    ) {
        var candidates = players.onlinePlayers().stream()
                .filter(player -> !player.uuid().equals(requester.uuid()))
                .filter(player -> vanish
                        .map(service -> service.canSee(requester, player.uuid()))
                        .orElse(true)
                )
                .limit(maximumBulkTargets)
                .toList();
        var counts = new int[4];

        CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);
        for (var target : candidates) {
            chain = chain.thenCompose(_ -> allowed(target, bypassPreference)
                    .thenAccept(allowed -> {
                        if (!allowed) {
                            counts[1]++;
                            return;
                        }

                        var creation = requests.create(
                                requester,
                                target,
                                TeleportRequestType.TARGET_TO_REQUESTER,
                                timeoutSeconds
                        );
                        if (!creation.created()) {
                            counts[2]++;
                            return;
                        }

                        counts[0]++;
                        notify(
                                target,
                                "commands.teleport.request.received-tpahere",
                                Map.of(
                                        "player", requester.name(),
                                        "seconds", timeoutSeconds,
                                        "request", creation.request().id()
                                )
                        );
                    })
                    .exceptionally(_ -> {
                        counts[3]++;
                        return null;
                    })
            );
        }

        return chain.thenApply(_ -> counts[3] == 0 ?
                TeleportCommandResult.success(
                        "commands.teleport.tpa-all-command.reply.requests-sent",
                        Map.of(
                                "sent", counts[0],
                                "blocked", counts[1],
                                "existing", counts[2],
                                "failed", counts[3]
                        )
                ) :
                TeleportCommandResult.partial(
                        "commands.teleport.tpa-all-command.reply.requests-sent",
                        Map.of(
                                "sent", counts[0],
                                "blocked", counts[1],
                                "existing", counts[2],
                                "failed", counts[3]
                        )
                )
        );
    }

    @Override
    public CompletableFuture<TeleportCommandResult> accept(
            CellPlayer target,
            Optional<TeleportRequestSelector> selector,
            boolean automatic
    ) {
        return selectIncoming(target, selector)
                .thenCompose(selection -> {
                    if (selection instanceof TeleportRequestSelectionResult.None) {
                        return completed(failure(
                                NOT_FOUND,
                                "commands.teleport.tp-accept-command.error.there-no-pending-teleport-request"
                        ));
                    }

                    if (selection instanceof TeleportRequestSelectionResult.Ambiguous ambiguous) {
                        return completed(ambiguous(ambiguous));
                    }

                    var request = ((TeleportRequestSelectionResult.Selected) selection).request();
                    var claimed = requests.claim(request.id());

                    if (claimed.isEmpty()) {
                        return completed(failure(
                                REQUEST_CHANGED,
                                "commands.teleport.request.changed"
                        ));
                    }

                    var value = claimed.orElseThrow();
                    var requester = players.onlinePlayer(value.requester());

                    if (requester.isEmpty()) {
                        requests.complete(value.id());
                        return completed(failure(
                                NOT_FOUND,
                                "commands.teleport.tp-accept-command.error.requesting-player-no-longer-online"
                        ));
                    }

                    var mover = value.type() == TeleportRequestType.REQUESTER_TO_TARGET
                            ? requester.orElseThrow()
                            : target;
                    var destinationPlayer = value.type() == TeleportRequestType.REQUESTER_TO_TARGET
                            ? target
                            : requester.orElseThrow();

                    return serverThread
                            .submit(() -> locations.currentLocation(destinationPlayer))
                            .thenCompose(destination -> teleports.teleport(
                                    mover,
                                    destination,
                                    TeleportOptions.defaults().withWarmup(warmupSeconds)
                            ))
                            .handle((result, throwable) -> {
                                if (throwable != null || !result.success()) {
                                    var released = requests.release(value.id());
                                    return failure(
                                            released
                                                    ? PLATFORM_FAILURE
                                                    : REQUEST_CHANGED,
                                            released
                                                    ? "commands.teleport.request.failed"
                                                    : "commands.teleport.request.release-failed"
                                    );
                                }

                                if (!requests.complete(value.id())) {
                                    return failure(
                                            REQUEST_CHANGED,
                                            "commands.teleport.request.complete-failed"
                                    );
                                }

                                notify(
                                        requester.orElseThrow(),
                                        "commands.teleport.request.accepted-by-target",
                                        Map.of(
                                                "player", target.name(),
                                                "request", value.id()
                                        )
                                );

                                return TeleportCommandResult.success(
                                        automatic
                                                ? "commands.teleport.request.auto-accepted"
                                                : "commands.teleport.tp-accept-command.reply.teleport-request-accepted",
                                        Map.of(
                                                "player", requester.orElseThrow().name(),
                                                "request", value.id()
                                        )
                                );
                            });
                });
    }

    @Override
    public CompletableFuture<TeleportCommandResult> deny(
            CellPlayer target,
            Optional<TeleportRequestSelector> selector
    ) {
        return selectIncoming(target, selector)
                .thenApply(selection -> {
                    if (selection instanceof TeleportRequestSelectionResult.None) {
                        return failure(
                                NOT_FOUND,
                                "commands.teleport.tp-deny-command.error.no-pending-request"
                        );
                    }

                    if (selection instanceof TeleportRequestSelectionResult.Ambiguous ambiguous) {
                        return ambiguous(ambiguous);
                    }

                    var request = ((TeleportRequestSelectionResult.Selected) selection).request();

                    if (!requests.remove(request.id())) {
                        return failure(
                                REQUEST_CHANGED,
                                "commands.teleport.request.changed"
                        );
                    }

                    players.onlinePlayer(request.requester())
                            .ifPresent(player -> notify(
                                    player,
                                    "commands.teleport.request.denied-by-target",
                                    Map.of(
                                            "player", target.name(),
                                            "request", request.id()
                                    )
                            ));

                    return TeleportCommandResult.success(
                            "commands.teleport.tp-deny-command.reply.denied",
                            Map.of("request", request.id())
                    );
                });
    }

    @Override
    public CompletableFuture<TeleportCommandResult> cancel(
            CellPlayer requester,
            Optional<TeleportRequestSelector> selector
    ) {
        return selectOutgoing(requester, selector)
                .thenApply(selection -> {
                    if (selection instanceof TeleportRequestSelectionResult.None) {
                        return failure(
                                NOT_FOUND,
                                "commands.teleport.tp-cancel-command.error.no-pending-request"
                        );
                    }

                    if (selection instanceof TeleportRequestSelectionResult.Ambiguous ambiguous) {
                        return ambiguous(ambiguous);
                    }

                    var request = ((TeleportRequestSelectionResult.Selected) selection).request();

                    if (!requests.remove(request.id())) {
                        return failure(
                                REQUEST_CHANGED,
                                "commands.teleport.request.changed"
                        );
                    }

                    players.onlinePlayer(request.target()).ifPresent(player -> notify(
                            player,
                            "commands.teleport.request.cancelled-by-requester",
                            Map.of(
                                    "player", requester.name(),
                                    "request", request.id()
                            )
                    ));

                    return TeleportCommandResult.success(
                            "commands.teleport.tp-cancel-command.reply.cancelled",
                            Map.of("request", request.id())
                    );
                });
    }

    private CompletableFuture<TeleportRequestSelectionResult> selectOutgoing(
            CellPlayer requester,
            Optional<TeleportRequestSelector> selector
    ) {
        return selectorIds(requester, selector)
                .thenApply(ids -> requests.selectOutgoing(
                        requester.uuid(),
                        ids.player(),
                        ids.request()
                ));
    }

    private static CompletableFuture<TeleportCommandResult> completed(TeleportCommandResult result) {
        return CompletableFuture.completedFuture(result);
    }

    private static TeleportCommandResult failure(
            TeleportCommandStatus status,
            String key,
            Map<String, ?> values
    ) {
        return TeleportCommandResult.failure(status, key, values);
    }

    private static TeleportCommandResult failure(TeleportCommandStatus status, String key) {
        return TeleportCommandResult.failure(status, key);
    }

    private CompletableFuture<Boolean> allowed(CellPlayer target, boolean bypass) {
        return bypass
                ? CompletableFuture.completedFuture(true)
                : users.load(target.uuid())
                        .thenApply(user -> user.preferences.teleportRequests)
                        .exceptionally(_ -> false);
    }

    private void notify(
            CellPlayer player,
            String key,
            Map<String, ?> values
    ) {
        serverThread.execute(() -> audience.send(
                player,
                renderer.render(audience.locale(player), key, values)
        ));
    }

    private CompletableFuture<TeleportRequestSelectionResult> selectIncoming(
            CellPlayer target,
            Optional<TeleportRequestSelector> selector
    ) {
        return selectorIds(target, selector)
                .thenApply(ids -> requests.selectIncoming(
                        target.uuid(),
                        ids.player(),
                        ids.request()
                ));
    }

    private TeleportCommandResult ambiguous(TeleportRequestSelectionResult.Ambiguous value) {
        var rows = value.requests().stream()
                .map(request -> "%s:%s:%s".formatted(
                        request.id(),
                        displayName(request.requester()),
                        request.type()
                ))
                .toList();
        return TeleportCommandResult.failure(
                AMBIGUOUS,
                "commands.teleport.request.ambiguous",
                Map.of("requests", String.join(", ", rows))
        );
    }

    private CompletableFuture<SelectorIds> selectorIds(
            CellPlayer viewer,
            Optional<TeleportRequestSelector> selector
    ) {
        if (selector.isEmpty()) {
            return CompletableFuture.completedFuture(new SelectorIds(
                    Optional.empty(),
                    Optional.empty()
            ));
        }

        if (selector.orElseThrow() instanceof TeleportRequestSelector.RequestId(UUID id1)) {
            return CompletableFuture.completedFuture(new SelectorIds(
                    Optional.empty(),
                    Optional.of(id1)
            ));
        }

        var name = ((TeleportRequestSelector.PlayerName) selector.orElseThrow()).name();
        return resolver.resolve(name, viewer)
                .thenApply(resolved -> new SelectorIds(
                        resolved.optionalUuid(),
                        Optional.empty()
                ));
    }

    private String displayName(UUID uuid) {
        return players.onlinePlayer(uuid).map(CellPlayer::name)
                .or(() -> users.cached(uuid)
                        .map(user -> user.lastKnownName == null
                                ? uuid.toString()
                                : user.lastKnownName
                        )
                )
                .orElse(uuid.toString());
    }

    private record SelectorIds(
            Optional<UUID> player,
            Optional<UUID> request
    ) {

    }

}
