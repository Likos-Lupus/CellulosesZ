package top.likoslupus.cellulosesz.modules.kit.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.kit.KitService;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.user.UserService;

import java.util.Map;

public final class KitResetCommand extends AbstractKitCommand {

    private final UserService users;

    public KitResetCommand(
            PlatformService platform,
            KitService kits,
            UserService users
    ) {
        super(platform, kits);
        this.users = users;
    }

    @Override
    public String permission() {
        return "cellulosesz.kit.reset";
    }

    @Override
    public String usage() {
        return "/kitreset <player> <kit>";
    }

    @Override
    public String name() {
        return "kitreset";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var args = invocation.args();
        if (args.length != 2) {
            invocation.errorKey(
                    "commands.kit.kit-reset-command.error.1",
                    Map.of("value0", usage())
            );
            return 0;
        }

        var resolved = invocation.resolvePlayer(args[0]);
        var uuid = resolved.optionalUuid();
        if (uuid.isEmpty()) {
            invocation.errorKey(
                    "commands.kit.kit-reset-command.error.2",
                    Map.of("value0", args[0])
            );
            return 0;
        }

        kits.resetCooldown(uuid.get(), args[1])
                .thenRun(() -> invocation.replyKey("commands.kit.kit-reset-command.reply.1"));
        return 1;
    }

}
