package top.likoslupus.cellulosesz.modules.teleport.application;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.api.player.PlayerLocationPlatformService;
import top.likoslupus.cellulosesz.api.player.PlayerResolver;
import top.likoslupus.cellulosesz.api.playerstate.VanishService;
import top.likoslupus.cellulosesz.api.teleport.TeleportOptions;
import top.likoslupus.cellulosesz.api.teleport.TeleportService;
import top.likoslupus.cellulosesz.api.text.MessageArguments;
import top.likoslupus.cellulosesz.api.text.MessageRenderer;
import top.likoslupus.cellulosesz.api.user.UserService;
import top.likoslupus.cellulosesz.common.text.PlayerAudienceService;
import top.likoslupus.cellulosesz.core.command.execution.ServerThreadExecutor;
import top.likoslupus.cellulosesz.modules.teleport.TeleportRuntimeSettings;
import top.likoslupus.cellulosesz.modules.teleport.domain.TeleportRequestSelectionResult;
import top.likoslupus.cellulosesz.modules.teleport.domain.TeleportRequestSelector;
import top.likoslupus.cellulosesz.modules.teleport.domain.TeleportRequestType;
import top.likoslupus.cellulosesz.modules.teleport.service.TeleportRequestService;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.jspecify.annotations.Nullable;

import static top.likoslupus.cellulosesz.modules.teleport.application.TeleportCommandStatus.*;

import static java.util.Objects.requireNonNull;

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
    private final @Nullable VanishService vanish;
    private final TeleportRuntimeSettings settings;

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
            @Nullable VanishService vanish,
            TeleportRuntimeSettings settings
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
        this.settings = requireNonNull(settings, "settings");
    }

    @Override
    public CompletableFuture<TeleportCommandResult> create(
            CellPlayer requester,
            String targetName,
            TeleportRequestType type,
            boolean bypassPreference
    ) {
        var target = players.onlinePlayer(targetName);

        if (target == null) {
            return completed(failure(
                    NOT_FOUND,
                    "commands.common.player-offline",
                    MessageArguments.builder().add(targetName).build()
            ));
        }

        if (target.uuid().equals(requester.uuid())) {
            return completed(failure(
                    INVALID_INPUT,
                    "commands.teleport.request.self"
            ));
        }

        return allowed(target, bypassPreference)
                .thenCompose(allowed -> {
                    if (!allowed) {
                        return completed(failure(
                                BLOCKED,
                                "commands.teleport.request.blocked",
                                MessageArguments.builder().add(target.name()).build()
                        ));
                    }

                    var creation = requests.create(
                            requester,
                            target,
                            type,
                            settings.requestTimeoutSeconds()
                    );

                    if (!creation.created()) {
                        return completed(failure(
                                REQUEST_CHANGED,
                                "commands.teleport.request.already-pending",
                                MessageArguments.builder()
                                        .add(target.name())
                                        .add(creation.request().id())
                                        .build()
                        ));
                    }

                    notify(
                            target,
                            type == TeleportRequestType.REQUESTER_TO_TARGET
                                    ? "commands.teleport.request.received-tpa"
                                    : "commands.teleport.request.received-tpahere",
                            MessageArguments.empty()
                    );

                    users.load(target.uuid())
                            .thenAccept(user -> {
                                if (user.preferences().teleportAutoAccept()) {
                                    accept(
                                            target,
                                            Optional.of(new TeleportRequestSelector.RequestId(
                                                    creation.request().id()
                                            )),
                                            true
                                    );
                                }
                            });

                    return completed(TeleportCommandResult.success(
                            "commands.teleport.request.sent",
                            MessageArguments.builder()
                                    .add(creation.request().id())
                                    .add(target.name())
                                    .build()
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
                .filter(player -> vanish == null
                        || vanish.canSee(requester, player.uuid())
                )
                .limit(settings.requestMaximumBulkTargets())
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
                                settings.requestTimeoutSeconds()
                        );
                        if (!creation.created()) {
                            counts[2]++;
                            return;
                        }

                        counts[0]++;
                        notify(
                                target,
                                "commands.teleport.request.received-tpahere",
                                MessageArguments.empty()
                        );
                    })
                    .exceptionally(_ -> {
                        counts[3]++;
                        return null;
                    })
            );
        }

        return chain.thenApply(_ -> counts[3] == 0
                ?
                TeleportCommandResult.success(
                        "commands.teleport.tpa-all-command.reply.requests-sent",
                        MessageArguments.builder()
                                .add(counts[0])
                                .add(counts[1])
                                .add(counts[2])
                                .add(counts[3])
                                .build()
                )
                : TeleportCommandResult.partial(
                        "commands.teleport.tpa-all-command.reply.requests-sent",
                        MessageArguments.builder()
                                .add(counts[0])
                                .add(counts[1])
                                .add(counts[2])
                                .add(counts[3])
                                .build()
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

                    if (requester == null) {
                        requests.complete(value.id());
                        return completed(failure(
                                NOT_FOUND,
                                "commands.teleport.tp-accept-command.error.requesting-player-no-longer-online"
                        ));
                    }

                    var mover = value.type() == TeleportRequestType.REQUESTER_TO_TARGET
                            ? requester
                            : target;
                    var destinationPlayer = value.type() == TeleportRequestType.REQUESTER_TO_TARGET
                            ? target
                            : requester;

                    return serverThread
                            .submit(() -> locations.currentLocation(destinationPlayer))
                            .thenCompose(destination -> teleports.teleport(
                                    mover,
                                    destination,
                                    TeleportOptions.defaults().withWarmup(settings.warmupSeconds())
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
                                        requester,
                                        "commands.teleport.request.accepted-by-target",
                                        MessageArguments.empty()
                                );

                                return TeleportCommandResult.success(
                                        automatic
                                                ? "commands.teleport.request.auto-accepted"
                                                : "commands.teleport.tp-accept-command.reply.teleport-request-accepted",
                                        MessageArguments.empty()
                                );
                            });
                });
    }

    @Override
    public CompletableFuture<TeleportCommandResult> deny(
            CellPlayer target,
            Optional<TeleportRequestSelector> selector
    ) {
        return selectIncoming(target, selector).thenApply(selection -> {
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

            var onlineRequester = players.onlinePlayer(request.requester());
            if (onlineRequester != null) {
                notify(
                        onlineRequester,
                        "commands.teleport.request.denied-by-target",
                        MessageArguments.empty()
                );
            }

            return TeleportCommandResult.success(
                    "commands.teleport.tp-deny-command.reply.denied",
                    MessageArguments.builder().add(request.id()).build()
            );
        });
    }

    @Override
    public CompletableFuture<TeleportCommandResult> cancel(
            CellPlayer requester,
            Optional<TeleportRequestSelector> selector
    ) {
        return selectOutgoing(requester, selector).thenApply(selection -> {
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

            var onlineTarget = players.onlinePlayer(request.target());
            if (onlineTarget != null) {
                notify(
                        onlineTarget,
                        "commands.teleport.request.cancelled-by-requester",
                        MessageArguments.builder()
                                .add(requester.name())
                                .add(request.id())
                                .build()
                );
            }

            return TeleportCommandResult.success(
                    "commands.teleport.tp-cancel-command.reply.cancelled",
                    MessageArguments.builder().add(request.id()).build()
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
            MessageArguments values
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
                        .thenApply(user -> user.preferences().teleportRequests());
    }

    private void notify(
            CellPlayer player,
            String key,
            MessageArguments values
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
                MessageArguments.builder().add(String.join(", ", rows)).build()
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
                        Optional.ofNullable(resolved.uuid()),
                        Optional.empty()
                ));
    }

    private String displayName(UUID uuid) {
        var online = players.onlinePlayer(uuid);
        if (online != null) {
            return online.name();
        }

        var user = users.cached(uuid);
        if (user != null && user.lastKnownName() != null) {
            return user.lastKnownName();
        }

        return uuid.toString();
    }

    private record SelectorIds(
            Optional<UUID> player,
            Optional<UUID> request
    ) {

    }

}
