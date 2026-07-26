package top.likoslupus.cellulosesz.modules.teleport.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.user.UserService;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

final class TeleportTargetPolicy {

    private final PlatformService platform;
    private final UserService users;

    TeleportTargetPolicy(PlatformService platform, UserService users) {
        this.platform = platform;
        this.users = users;
    }

    CompletableFuture<Boolean> mayMove(CommandInvocation invocation, CellPlayer subject) {
        var source = platform.player(invocation);
        if (source.isPresent() && source.orElseThrow().uuid().equals(subject.uuid())) {
            return CompletableFuture.completedFuture(true);
        }
        if (invocation.hasPermission("cellulosesz.teleport.tptoggle.bypass")) {
            return CompletableFuture.completedFuture(true);
        }
        return users.load(subject.uuid()).handle((user, failure) -> {
            if (failure != null) {
                invocation.errorKey("service.user.load-failed");
                return false;
            }
            if (user.preferences.teleportRequests) return true;
            invocation.errorKey("commands.teleport.tptoggle.blocks-teleport", Map.of("player", subject.name()));
            return false;
        });
    }

}
