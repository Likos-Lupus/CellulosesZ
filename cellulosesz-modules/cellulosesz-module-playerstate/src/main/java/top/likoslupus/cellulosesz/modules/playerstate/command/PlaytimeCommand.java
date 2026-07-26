package top.likoslupus.cellulosesz.modules.playerstate.command;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.user.UserService;

import java.util.Map;
import java.util.UUID;

public final class PlaytimeCommand implements CellCommand {

    private final PlatformService platform;
    private final UserService users;

    public PlaytimeCommand(
            PlatformService platform,
            UserService users
    ) {
        this.platform = platform;
        this.users = users;
    }

    @Override
    public String permission() {
        return "cellulosesz.playerstate.playtime";
    }

    @Override
    public String usage() {
        return "/playtime [player]";
    }

    @Override
    public String name() {
        return "playtime";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var args = invocation.args();
        final UUID uuid;
        final String name;

        if (args.length == 0) {
            var self = platform.player(invocation);
            if (self.isEmpty()) {
                invocation.errorKey("commands.common.player-required");
                return 0;
            }

            uuid = self.get().uuid();
            name = self.get().name();
        } else {
            if (!invocation.hasPermission("cellulosesz.playerstate.playtime.others")) {
                invocation.errorKey("common.no-permission");
                return 0;
            }

            var resolved = invocation.resolvePlayer(args[0]);
            if (resolved.optionalUuid().isEmpty()) {
                invocation.errorKey(
                        "commands.common.unknown-player",
                        Map.of("player", args[0])
                );
                return 0;
            }

            uuid = resolved.optionalUuid().orElseThrow();
            name = resolved.name();
        }

        users.load(uuid).whenComplete((user, failure) -> {
            if (failure != null) {
                invocation.errorKey("service.user.load-failed");
                return;
            }
            try {
                var total = user.timestamps.playTimeMillis;
                if (user.timestamps.activeSessionStartedAt != null) {
                    total = Math.addExact(total, Math.max(0L, System.currentTimeMillis() - user.timestamps.activeSessionStartedAt));
                }
                invocation.replyKey("commands.playerstate.playtime", Map.of(
                        "player", name, "playtime", PlayerTimeFormat.duration(total)));
            } catch (ArithmeticException _) {
                invocation.errorKey("commands.playerstate.playtime-invalid");
            }
        });
        return 1;
    }

}
