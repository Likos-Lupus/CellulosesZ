package top.likoslupus.cellulosesz.modules.item.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.item.ItemAutomationService;
import top.likoslupus.cellulosesz.api.item.ItemService;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.modules.item.ItemConfig;

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
            invocation.error("用法: " + usage());
            return 0;
        }

        var target = target(invocation, args[0]);
        if (target.isEmpty()) return 0;

        var descriptor = items.parse(join(args, 1));
        if (descriptor.isEmpty()) {
            invocation.error("无效物品描述。");
            return 0;
        }

        if (!items.give(target.get(), descriptor.get())) {
            invocation.error("物品发放失败。");
            return 0;
        }

        invocation.reply("已给予 %s %d 个 %s。".formatted(
                target.get().name(),
                descriptor.get().count,
                descriptor.get().normalizedItem()
        ));
        return 1;
    }

}
