package top.likoslupus.cellulosesz.modules.teleport.service;

import org.jspecify.annotations.Nullable;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.teleport.*;
import top.likoslupus.cellulosesz.api.text.LocaleResolver;
import top.likoslupus.cellulosesz.api.text.MessageRenderer;
import top.likoslupus.cellulosesz.api.user.UserService;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionException;

public final class TeleportRequestExecutor {

    private final PlatformService platform;
    private final TeleportService teleports;
    private final TeleportRequestService requests;
    private final UserService users;
    private final MessageRenderer renderer;
    private final LocaleResolver locales;
    private final int warmupSeconds;

    public TeleportRequestExecutor(
            PlatformService platform,
            TeleportService teleports,
            TeleportRequestService requests,
            UserService users,
            MessageRenderer renderer,
            LocaleResolver locales,
            int warmupSeconds
    ) {
        this.platform = platform;
        this.teleports = teleports;
        this.requests = requests;
        this.users = users;
        this.renderer = renderer;
        this.locales = locales;
        this.warmupSeconds = Math.max(0, warmupSeconds);
    }

    public TeleportRequestCreateResult create(
            CommandInvocation invocation,
            CellPlayer requester,
            CellPlayer target,
            TeleportRequestType type,
            int timeoutSeconds
    ) {
        var creation = requests.create(requester, target, type, timeoutSeconds);
        if (!creation.created()) return creation;
        var request = creation.request();
        platform.runOnServerThread(() -> platform.sendMessage(
                target,
                renderer.render(
                        locales.locale(target),
                        type == TeleportRequestType.REQUESTER_TO_TARGET
                                ? "commands.teleport.request.received-tpa"
                                : "commands.teleport.request.received-tpahere",
                        Map.of(
                                "player", requester.name(),
                                "seconds", timeoutSeconds,
                                "request", request.id()
                        )
                )
        ));

        users.load(target.uuid()).whenComplete((targetUser, failure) -> {
            if (failure != null || !targetUser.preferences.teleportAutoAccept) return;
            platform.runOnServerThread(() -> online(target.uuid()).ifPresent(onlineTarget ->
                    acceptRequest(invocation, onlineTarget, request.id(), true)
            ));
        });
        return creation;
    }

    public Optional<CellPlayer> online(UUID uuid) {
        return platform.onlinePlayers().stream()
                .filter(player -> player.uuid().equals(uuid))
                .findFirst();
    }

    public boolean acceptRequest(
            CommandInvocation invocation,
            CellPlayer acceptingPlayer,
            UUID requestId,
            boolean automatic
    ) {
        var selected = requests.pending(requestId);
        if (selected.isEmpty() || !selected.orElseThrow().target().equals(acceptingPlayer.uuid())) {
            invocation.errorKey("commands.teleport.tp-accept-command.error.there-no-pending-teleport-request");
            return false;
        }
        var claimed = requests.claim(requestId);
        if (claimed.isEmpty()) {
            invocation.errorKey("commands.teleport.request.changed");
            return false;
        }
        var pending = claimed.orElseThrow();
        var requester = online(pending.requester());
        if (requester.isEmpty()) {
            requests.complete(pending.id());
            invocation.errorKey("commands.teleport.tp-accept-command.error.requesting-player-no-longer-online");
            return false;
        }

        var mover = pending.type() == TeleportRequestType.REQUESTER_TO_TARGET
                ? requester.orElseThrow()
                : acceptingPlayer;
        var destinationPlayer = pending.type() == TeleportRequestType.REQUESTER_TO_TARGET
                ? acceptingPlayer
                : requester.orElseThrow();
        var options = new TeleportOptions().warmupSeconds(warmupSeconds);

        platform.callOnServerThread(() -> teleports.teleport(
                        mover,
                        platform.location(destinationPlayer),
                        options
                ))
                .thenCompose(value -> value)
                .whenComplete((result, throwable) -> platform.runOnServerThread(() -> {
                    if (throwable != null) {
                        requests.release(pending.id());
                        invocation.errorKey(
                                "commands.teleport.request.failed",
                                Map.of("reason", failureMessage(throwable))
                        );
                        return;
                    }
                    if (!result.success()) {
                        requests.release(pending.id());
                        invocation.errorKey(result.message().key(), result.message().placeholders());
                        return;
                    }

                    // A claimed request cannot be claimed again. Even if disconnect cleanup removed it while the
                    // teleport was in flight, the successful movement remains a single-use operation.
                    requests.complete(pending.id());
                    invocation.replyKey(
                            automatic
                                    ? "commands.teleport.request.auto-accepted"
                                    : "commands.teleport.tp-accept-command.reply.teleport-request-accepted",
                            Map.of(
                                    "player", requester.orElseThrow().name(),
                                    "request", pending.id()
                            )
                    );
                    platform.sendMessage(
                            requester.orElseThrow(),
                            renderer.render(
                                    locales.locale(requester.orElseThrow()),
                                    "commands.teleport.request.accepted-by-target",
                                    Map.of(
                                            "player", acceptingPlayer.name(),
                                            "request", pending.id()
                                    )
                            )
                    );
                }));
        return true;
    }

    private String failureMessage(Throwable throwable) {
        var cause = throwable;
        while (cause instanceof CompletionException && cause.getCause() != null) {
            cause = cause.getCause();
        }
        var message = cause.getMessage();
        return message == null || message.isBlank() ? cause.getClass().getSimpleName() : message;
    }

    public boolean accept(
            CommandInvocation invocation,
            CellPlayer acceptingPlayer,
            @Nullable UUID requesterId,
            boolean automatic
    ) {
        Optional<TeleportRequest> request = requesterId == null
                ? requests.newestFor(acceptingPlayer.uuid())
                : requests.pendingFor(acceptingPlayer.uuid(), requesterId);
        if (request.isEmpty()) {
            invocation.errorKey("commands.teleport.tp-accept-command.error.there-no-pending-teleport-request");
            return false;
        }
        return acceptRequest(invocation, acceptingPlayer, request.orElseThrow().id(), automatic);
    }

}
