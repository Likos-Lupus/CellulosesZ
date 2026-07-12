package top.likoslupus.cellulosesz.modules.kit.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.kit.KitService;
import top.likoslupus.cellulosesz.api.platform.PlatformService;

import java.util.Map;

public final class DelKitCommand extends AbstractKitCommand {

    public DelKitCommand(
            PlatformService platform,
            KitService kits
    ) {
        super(platform, kits);
    }

    @Override
    public String permission() {
        return "cellulosesz.kit.delete";
    }

    @Override
    public String usage() {
        return "/delkit <name>";
    }

    @Override
    public String name() {
        return "delkit";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var args = invocation.args();
        if (args.length != 1) {
            invocation.errorKey(
                    "commands.kit.del-kit-command.error.1",
                    Map.of("value0", usage())
            );
            return 0;
        }

        kits.delete(args[0]).thenAccept(deleted -> {
            if (deleted) {
                invocation.replyKey(
                        "commands.kit.del-kit-command.reply.1",
                        Map.of("value0", args[0])
                );
            } else {
                invocation.errorKey(
                        "commands.kit.del-kit-command.error.2",
                        Map.of("value0", args[0])
                );
            }
        });
        return 1;
    }

}
