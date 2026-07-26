package top.likoslupus.cellulosesz.modules.teleport.command;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.user.UserService;

import java.util.Map;
import java.util.Optional;

public final class TpAutoCommand implements CellCommand {

    private final PlatformService platform;
    private final UserService users;

    public TpAutoCommand(PlatformService platform, UserService users) {
        this.platform = platform;
        this.users = users;
    }

    @Override
    public String permission() {
        return "cellulosesz.teleport.tpauto";
    }

    @Override
    public CommandSourceKind sourceKind() {
        return CommandSourceKind.PLAYER_ONLY;
    }

    @Override
    public String usage() {
        return "/tpauto [on|off]";
    }

    @Override
    public String name() {
        return "tpauto";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var player = platform.player(invocation);
        if (player.isEmpty()) {
            invocation.errorKey("commands.teleport.tp-auto.player-only");
            return 0;
        }
        if (invocation.args().length > 1) {
            invocation.errorKey("commands.teleport.request.usage", Map.of("usage", usage()));
            return 0;
        }
        Optional<Boolean> requested;
        if (invocation.args().length == 0) {
            requested = Optional.empty();
        } else {
            requested = parse(invocation.args()[0]);
            if (requested.isEmpty()) {
                invocation.errorKey("commands.teleport.tp-auto.invalid-state");
                return 0;
            }
        }
        users.update(player.orElseThrow().uuid(), user -> {
            var enabled = requested.orElse(!user.preferences.teleportAutoAccept);
            user.preferences.teleportAutoAccept = enabled;
            return enabled;
        }).whenComplete((enabled, failure) -> platform.runOnServerThread(() -> {
            if (failure != null) invocation.errorKey("service.user.persistence-failed");
            else invocation.replyKey(enabled
                    ? "commands.teleport.tp-auto.enabled"
                    : "commands.teleport.tp-auto.disabled");
        }));
        return 1;
    }

    private Optional<Boolean> parse(String input) {
        return switch (input.toLowerCase()) {
            case "on", "true", "enable", "enabled" -> Optional.of(Boolean.TRUE);
            case "off", "false", "disable", "disabled" -> Optional.of(Boolean.FALSE);
            default -> Optional.empty();
        };
    }

}
