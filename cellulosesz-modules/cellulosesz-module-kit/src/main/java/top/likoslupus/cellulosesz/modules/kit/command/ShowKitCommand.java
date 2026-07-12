package top.likoslupus.cellulosesz.modules.kit.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.kit.KitService;
import top.likoslupus.cellulosesz.api.platform.PlatformService;

import java.util.Map;

public final class ShowKitCommand extends AbstractKitCommand {

    public ShowKitCommand(
            PlatformService platform,
            KitService kits
    ) {
        super(platform, kits);
    }

    @Override
    public String permission() {
        return "cellulosesz.kit.show";
    }

    @Override
    public String usage() {
        return "/showkit <name>";
    }

    @Override
    public String name() {
        return "showkit";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var args = invocation.args();
        if (args.length != 1) {
            invocation.errorKey(
                    "commands.kit.show-kit-command.error.1",
                    Map.of("value0", usage())
            );
            return 0;
        }

        var kit = kits.kit(args[0]);
        if (kit.isEmpty()) {
            invocation.errorKey(
                    "commands.kit.show-kit-command.error.2",
                    Map.of("value0", args[0])
            );
            return 0;
        }

        var entries = new StringBuilder();
        kit.get().items.forEach(item -> entries.append("\n- ")
                .append(item.normalizedItem())
                .append(" x")
                .append(item.count));
        invocation.replyKey(
                "commands.kit.details",
                Map.of(
                        "kit", kit.get().displayName,
                        "entries", entries.toString()
                )
        );
        return 1;
    }

}
