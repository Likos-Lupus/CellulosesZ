package top.likoslupus.cellulosesz.modules.teleport.command;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.user.UserService;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class TpToggleCommand implements CellCommand {

    private final PlatformService platform;
    private final UserService users;

    public TpToggleCommand(
            PlatformService platform,
            UserService users
    ) {
        this.platform = platform;
        this.users = users;
    }

    @Override
    public String permission() {
        return "cellulosesz.teleport.tptoggle";
    }

    @Override
    public String usage() {
        return "/tptoggle [player] [on|off]";
    }

    @Override
    public String name() {
        return "tptoggle";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var args = invocation.args();

        if (args.length > 2) {
            return usage(invocation);
        }

        UUID uuid;
        String name;
        String mode = "";
        if (args.length == 0 || (args.length == 1 && isMode(args[0]))) {
            var self = platform.player(invocation);
            if (self.isEmpty()) {
                invocation.errorKey("commands.teleport.tp-toggle-command.error.command-can-only-used-by-player");
                return 0;
            }

            uuid = self.get().uuid();
            name = self.get().name();
            if (args.length == 1) {
                mode = args[0];
            }
        } else {
            if (!invocation.hasPermission("cellulosesz.teleport.tptoggle.others")) {
                invocation.errorKey("commands.teleport.tp-toggle-command.error.others");
                return 0;
            }

            var resolved = invocation.resolvePlayer(args[0]);
            if (resolved.optionalUuid().isEmpty()) {
                invocation.errorKey(
                        "commands.teleport.tp-toggle-command.error.player",
                        Map.of("player", args[0])
                );
                return 0;
            }

            uuid = resolved.optionalUuid().orElseThrow();
            name = resolved.name();
            if (args.length == 2) {
                mode = args[1];
            }
        }

        if (!mode.isBlank() && !isMode(mode)) return usage(invocation);

        var requestedMode = mode;
        users.update(uuid, user -> {
            var enabled = requestedMode.isBlank()
                    ? !user.preferences.teleportRequests
                    : enabled(requestedMode);
            user.preferences.teleportRequests = enabled;
            return enabled;
        }).whenComplete((enabled, failure) -> platform.runOnServerThread(() -> {
            if (failure != null) invocation.errorKey("service.user.persistence-failed");
            else invocation.replyKey(
                    enabled
                            ? "commands.teleport.tp-toggle-command.enabled"
                            : "commands.teleport.tp-toggle-command.disabled",
                    Map.of("player", name)
            );
        }));
        return 1;
    }

    private int usage(CommandInvocation invocation) {
        invocation.errorKey(
                "commands.teleport.tp-toggle-command.error.usage",
                Map.of("usage", usage())
        );
        return 0;
    }

    private boolean isMode(String value) {
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "on", "true", "enable", "off", "false", "disable" -> true;
            default -> false;
        };
    }

    private boolean enabled(String value) {
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "on", "true", "enable" -> true;
            default -> false;
        };
    }

}
