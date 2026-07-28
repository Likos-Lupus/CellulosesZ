package top.likoslupus.cellulosesz.modules.teleport.command;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.teleport.TeleportOptions;
import top.likoslupus.cellulosesz.api.teleport.TeleportService;
import top.likoslupus.cellulosesz.api.user.UserService;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class TpAllCommand implements CellCommand {

    private final PlatformService platform;
    private final TeleportService teleports;
    private final UserService users;

    public TpAllCommand(PlatformService platform, TeleportService teleports, UserService users) {
        this.platform = platform;
        this.teleports = teleports;
        this.users = users;
    }

    @Override
    public String permission() {
        return "cellulosesz.teleport.tpall";
    }

    @Override
    public String usage() {
        return "/tpall [player]";
    }

    @Override
    public String name() {
        return "tpall";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        if (invocation.args().length > 1) {
            invocation.errorKey("commands.teleport.request.usage", Map.of("usage", usage()));
            return 0;
        }
        final CellPlayer destination;
        if (invocation.args().length == 0) {
            var self = platform.player(invocation);
            if (self.isEmpty()) {
                invocation.errorKey("commands.teleport.tpall.target-required");
                return 0;
            }
            destination = self.orElseThrow();
        } else {
            var resolved = invocation.resolvePlayer(invocation.args()[0]).online();
            if (resolved.isEmpty()) {
                invocation.errorKey("commands.teleport.abstract-teleport-command.error.online-player-not-found", Map.of("player", invocation.args()[0]));
                return 0;
            }
            destination = resolved.orElseThrow();
        }
        var destinationLocation = platform.location(destination);
        var bypass = invocation.hasPermission("cellulosesz.teleport.tptoggle.bypass");
        var candidates = platform.onlinePlayers().stream()
                .filter(player -> !player.uuid().equals(destination.uuid()))
                .toList();
        if (candidates.isEmpty()) {
            invocation.errorKey("commands.teleport.tpall.no-targets");
            return 0;
        }
        var results = new ArrayList<CompletableFuture<TeleportDecision>>(candidates.size());
        for (var player : candidates) {
            results.add(users.load(player.uuid()).thenCompose(user -> {
                if (!user.preferences.teleportRequests && !bypass) {
                    return CompletableFuture.completedFuture(TeleportDecision.BLOCKED);
                }
                return platform.callOnServerThread(() -> teleports.teleport(player, destinationLocation, new TeleportOptions()))
                        .thenCompose(value -> value)
                        .thenApply(result -> result.success() ? TeleportDecision.SUCCESS : TeleportDecision.FAILED)
                        .exceptionally(_ -> TeleportDecision.FAILED);
            }).exceptionally(_ -> TeleportDecision.FAILED));
        }
        CompletableFuture.allOf(results.toArray(CompletableFuture[]::new)).thenRun(() -> {
            var success = results.stream().map(future -> future.getNow(TeleportDecision.FAILED))
                    .filter(result -> result == TeleportDecision.SUCCESS).count();
            var blocked = results.stream().map(future -> future.getNow(TeleportDecision.FAILED))
                    .filter(result -> result == TeleportDecision.BLOCKED).count();
            var failed = results.size() - success - blocked;
            invocation.replyKey("commands.teleport.tpall.result-detailed", Map.of(
                    "success", success, "blocked", blocked, "failed", failed,
                    "total", results.size(), "player", destination.name()));
        });
        return 1;
    }

    private enum TeleportDecision {
        SUCCESS,
        BLOCKED,
        FAILED
    }

}
