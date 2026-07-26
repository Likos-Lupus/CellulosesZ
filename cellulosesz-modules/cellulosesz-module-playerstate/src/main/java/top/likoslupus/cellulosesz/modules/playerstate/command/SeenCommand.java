package top.likoslupus.cellulosesz.modules.playerstate.command;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.playerstate.VanishService;
import top.likoslupus.cellulosesz.api.user.UserService;

import java.time.Instant;
import java.util.Map;

public final class SeenCommand implements CellCommand {

    private final PlatformService platform;
    private final UserService users;
    private final VanishService vanish;

    public SeenCommand(
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
        return "cellulosesz.playerstate.seen";
    }

    @Override
    public String usage() {
        return "/seen <player>";
    }

    @Override
    public String name() {
        return "seen";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        if (invocation.args().length != 1) {
            invocation.errorKey(
                    "commands.playerstate.seen-usage",
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
        var visibleOnline = resolved.online().isPresent()
                && (viewer.isEmpty() || vanish.canSee(viewer.get(), uuid.get()));
        if (visibleOnline) {
            invocation.replyKey(
                    "commands.playerstate.seen-online",
                    Map.of("player", resolved.name())
            );
            return 1;
        }

        users.load(uuid.orElseThrow()).whenComplete((user, failure) -> {
            if (failure != null) {
                invocation.errorKey("service.user.load-failed");
            } else if (user.timestamps.lastQuit <= 0L) {
                invocation.replyKey("commands.playerstate.seen-never", Map.of("player", resolved.name()));
            } else {
                invocation.replyKey("commands.playerstate.seen-offline", Map.of(
                        "player", resolved.name(),
                        "timestamp", Instant.ofEpochMilli(user.timestamps.lastQuit).toString(),
                        "ago", PlayerTimeFormat.duration(Math.max(0L, System.currentTimeMillis() - user.timestamps.lastQuit))
                ));
            }
        });
        return 1;
    }

}
