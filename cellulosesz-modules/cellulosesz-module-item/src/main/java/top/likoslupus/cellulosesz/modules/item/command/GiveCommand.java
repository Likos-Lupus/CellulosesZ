package top.likoslupus.cellulosesz.modules.item.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.item.ItemAutomationService;
import top.likoslupus.cellulosesz.api.item.ItemService;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.modules.item.ItemConfig;

import java.util.Map;

public final class GiveCommand extends AbstractItemCommand {

    public GiveCommand(
            PlatformService platform,
            ItemService items,
            ItemAutomationService automation,
            ItemConfig config
    ) {
        super(platform, items, automation, config);
    }

    @Override
    public String permission() {
        return "cellulosesz.item.give";
    }

    @Override
    public String usage() {
        return "/give <player> <id> [count] [components]";
    }

    @Override
    public String name() {
        return "give";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var args = invocation.args();
        if (args.length < 2) {
            invocation.errorKey(
                    "commands.item.give-command.error.1",
                    Map.of("value0", usage())
            );
            return 0;
        }

        var target = target(invocation, args[0]);
        if (target.isEmpty()) return 0;

        var descriptor = items.parse(join(args, 1));
        if (descriptor.isEmpty()) {
            invocation.errorKey("commands.item.give-command.error.2");
            return 0;
        }

        if (!items.give(target.get(), descriptor.get())) {
            invocation.errorKey("commands.item.give-command.error.3");
            return 0;
        }

        invocation.replyKey(
                "commands.item.give-command.reply.1",
                Map.of(
                        "value0", target.get().name(),
                        "value1", descriptor.get().count,
                        "value2", descriptor.get().normalizedItem()
                )
        );
        return 1;
    }

}
