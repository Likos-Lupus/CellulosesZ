package top.likoslupus.cellulosesz.modules.playerstate.command;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.playerstate.VanishService;
import top.likoslupus.cellulosesz.api.user.UserService;

import java.time.Instant;
import java.util.Map;

public final class WhoisCommand implements CellCommand {

    private final PlatformService platform;
    private final UserService users;
    private final VanishService vanish;

    public WhoisCommand(
            PlatformService platform,
            UserService users,
            VanishService vanish
    ) {
        this.platform = platform;
        this.users = users;
        this.vanish = vanish;
    }

    @Override
    public String permission() {
        return "cellulosesz.playerstate.whois";
    }

    @Override
    public String usage() {
        return "/whois <player>";
    }

    @Override
    public String name() {
        return "whois";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        if (invocation.args().length != 1) {
            invocation.errorKey(
                    "commands.playerstate.whois-usage",
                    Map.of("usage", usage())
            );
            return 0;
        }

        var resolved = invocation.resolvePlayer(invocation.args()[0]);
        var uuid = resolved.optionalUuid();
        if (uuid.isEmpty()) {
            invocation.errorKey(
                    "commands.common.unknown-player",
                    Map.of("player", invocation.args()[0])
            );
            return 0;
        }

        var viewer = platform.player(invocation);
        var online = resolved.online().isPresent()
                && (viewer.isEmpty() || vanish.canSee(viewer.orElseThrow(), uuid.orElseThrow()));
        users.load(uuid.orElseThrow()).whenComplete((user, failure) -> {
            if (failure != null) {
                invocation.errorKey("service.user.load-failed");
                return;
            }
            try {
                var total = user.timestamps.playTimeMillis;
                if (user.timestamps.activeSessionStartedAt != null) {
                    total = Math.addExact(total, Math.max(0L, System.currentTimeMillis() - user.timestamps.activeSessionStartedAt));
                }
                invocation.replyKey("commands.playerstate.whois", Map.of(
                        "player", resolved.name(),
                        "uuid", invocation.hasPermission("cellulosesz.playerstate.whois.uuid")
                                ? uuid.orElseThrow()
                                : "-",
                        "online", online,
                        "afk", user.state.afk,
                        "firstJoin", user.timestamps.firstJoin <= 0L
                                ? "unknown"
                                : Instant.ofEpochMilli(user.timestamps.firstJoin),
                        "lastJoin", user.timestamps.lastJoin <= 0L
                                ? "unknown"
                                : Instant.ofEpochMilli(user.timestamps.lastJoin),
                        "lastQuit", user.timestamps.lastQuit <= 0L
                                ? "unknown"
                                : Instant.ofEpochMilli(user.timestamps.lastQuit),
                        "playtime", PlayerTimeFormat.duration(total),
                        "nickname", user.state.nickname == null ? "-" : user.state.nickname
                ));
            } catch (ArithmeticException _) {
                invocation.errorKey("commands.playerstate.playtime-invalid");
            }
        });
        return 1;
    }

}
