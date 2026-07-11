package top.likoslupus.cellulosesz.modules.item.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.item.ItemAutomationService;
import top.likoslupus.cellulosesz.api.item.ItemService;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.modules.item.ItemConfig;

public final class ItemCommand extends AbstractItemCommand {

    public ItemCommand(
            PlatformService platform,
            ItemService items,
            ItemAutomationService automation,
            ItemConfig config
    ) {
        super(platform, items, automation, config);
    }

    @Override
    public String permission() {
        return "cellulosesz.item.spawn";
    }

    @Override
    public CommandSourceKind sourceKind() {
        return CommandSourceKind.PLAYER_ONLY;
    }

    @Override
    public String usage() {
        return "/item <id> [count] [components]";
    }

    @Override
    public String name() {
        return "item";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var self = player(invocation);
        if (self.isEmpty()) return 0;

        var descriptor = items.parse(join(invocation.args(), 0));
        if (descriptor.isEmpty()) {
            invocation.error("无效物品描述。用法: " + usage());
            return 0;
        }

        if (!items.give(self.get(), descriptor.get())) {
            invocation.error("物品发放失败。");
            return 0;
        }

        invocation.reply("已获得 %d 个 %s。".formatted(descriptor.get().count, descriptor.get().normalizedItem()));
        return 1;
    }

}
