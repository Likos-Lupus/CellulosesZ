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
                    "commands.kit.show-kit-command.error.usage",
                    Map.of("usage", usage())
            );
            return 0;
        }

        var kit = kits.kit(args[0]);
        if (kit.isEmpty()) {
            invocation.errorKey(
                    "commands.kit.show-kit-command.error.kit-does-not-exist",
                    Map.of("kit", args[0])
            );
            return 0;
        }

        var entries = new StringBuilder();
        for (var item : kit.get().items) {
            var descriptor = platform.describeInventoryItem(item);
            if (descriptor.isEmpty()) {
                invocation.errorKey("commands.kit.show-kit-command.error.invalid-item");
                return 0;
            }

            entries.append("\n- [")
                    .append(item.slot)
                    .append("] ")
                    .append(descriptor.orElseThrow().normalizedItem())
                    .append(" x")
                    .append(descriptor.orElseThrow().count);
        }

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
