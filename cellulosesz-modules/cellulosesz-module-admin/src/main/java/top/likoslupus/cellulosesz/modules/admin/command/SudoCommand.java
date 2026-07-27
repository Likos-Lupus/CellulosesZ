package top.likoslupus.cellulosesz.modules.admin.command;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.service.CommandDispatchOrigin;
import top.likoslupus.cellulosesz.api.command.service.PlayerCommandDispatchService;
import top.likoslupus.cellulosesz.api.permission.PermissionService;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.modules.admin.config.AdminConfig;

import java.util.Map;

public final class SudoCommand implements CellCommand {

    private final PlatformService platform;
    private final PlayerCommandDispatchService dispatch;
    private final PermissionService permissions;
    private final AdminConfig config;

    public SudoCommand(
            PlatformService platform,
            PlayerCommandDispatchService dispatch,
            PermissionService permissions,
            AdminConfig config
    ) {
        this.platform = platform;
        this.dispatch = dispatch;
        this.permissions = permissions;
        this.config = config;
    }

    @Override
    public String permission() {
        return "cellulosesz.command.sudo";
    }

    @Override
    public String usage() {
        return "/sudo <player> <command...>";
    }

    @Override
    public String name() {
        return "sudo";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        if (invocation.args().length != 2) return usage(invocation);
        var target = invocation.resolvePlayer(invocation.args()[0]).online();
        if (target.isEmpty()) {
            invocation.errorKey("commands.common.unknown-player", Map.of("player", invocation.args()[0]));
            return 0;
        }
        var self = platform.player(invocation);
        if (self.isPresent() && self.orElseThrow().uuid().equals(target.orElseThrow().uuid())) {
            invocation.errorKey("commands.admin.sudo.self");
            return 0;
        }
        if (permissions.has(target.orElseThrow().nativeHandle(), "cellulosesz.command.sudo.exempt")) {
            invocation.errorKey("commands.admin.sudo.exempt", Map.of("player", target.orElseThrow().name()));
            return 0;
        }
        var command = normalize(invocation.args()[1]);
        if (!valid(command)) {
            invocation.errorKey("commands.admin.sudo.invalid-command", Map.of("maximum", config.sudoMaximumCommandLength));
            return 0;
        }
        if (command.regionMatches(true, 0, "c:", 0, 2)) {
            invocation.errorKey("commands.admin.sudo.chat-unavailable");
            return 0;
        }
        var result = dispatch.dispatch(target.orElseThrow(), command, CommandDispatchOrigin.SUDO);
        if (!result.successful()) {
            invocation.platformError(result.status());
            return 0;
        }
        invocation.replyKey("commands.admin.sudo.success", Map.of(
                "player", target.orElseThrow().name(),
                "result", result.commandResult()
        ));
        return Math.max(1, result.commandResult());
    }

    private int usage(CommandInvocation invocation) {
        invocation.errorKey("commands.admin.sudo.usage", Map.of("usage", usage()));
        return 0;
    }

    private static String normalize(String command) {
        var value = command.strip();
        return value.startsWith("/") ? value.substring(1) : value;
    }

    private boolean valid(String command) {
        if (command.isBlank() || command.length() > config.sudoMaximumCommandLength) return false;
        for (int index = 0; index < command.length(); index++) {
            var value = command.charAt(index);
            if (value == '\r' || value == '\n' || value == '\0' || Character.isISOControl(value)) return false;
        }
        return true;
    }

}
