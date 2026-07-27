package top.likoslupus.cellulosesz.modules.admin.command;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.permission.PermissionService;
import top.likoslupus.cellulosesz.api.playerstate.KillKind;
import top.likoslupus.cellulosesz.api.playerstate.PlayerStatePlatformService;

import java.util.Map;

public final class KillCommand implements CellCommand {

    private final PlayerStatePlatformService players;
    private final PermissionService permissions;

    public KillCommand(
            PlayerStatePlatformService players,
            PermissionService permissions
    ) {
        this.players = players;
        this.permissions = permissions;
    }

    @Override
    public String permission() {
        return "cellulosesz.command.kill";
    }

    @Override
    public String usage() {
        return "/kill <player>";
    }

    @Override
    public String name() {
        return "kill";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        if (invocation.args().length != 1) return usage(invocation);
        var target = invocation.resolvePlayer(invocation.args()[0]).online();
        if (target.isEmpty()) {
            invocation.errorKey("commands.common.unknown-player", Map.of("player", invocation.args()[0]));
            return 0;
        }
        var force = invocation.hasPermission("cellulosesz.command.kill.force");
        if (permissions.has(target.orElseThrow().nativeHandle(), "cellulosesz.command.kill.exempt") && !force) {
            invocation.errorKey("commands.admin.kill.exempt", Map.of("player", target.orElseThrow().name()));
            return 0;
        }
        var result = players.kill(target.orElseThrow(), KillKind.ADMIN, force);
        if (!result.successful()) {
            invocation.platformError(result.status());
            return 0;
        }
        invocation.replyKey("commands.admin.kill.success", Map.of("player", target.orElseThrow().name()));
        return 1;
    }

    private int usage(CommandInvocation invocation) {
        invocation.errorKey("commands.admin.kill.usage", Map.of("usage", usage()));
        return 0;
    }

}
