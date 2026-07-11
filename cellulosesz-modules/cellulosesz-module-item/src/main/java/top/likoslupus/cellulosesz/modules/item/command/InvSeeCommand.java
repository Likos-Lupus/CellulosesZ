package top.likoslupus.cellulosesz.modules.item.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.item.ItemAutomationService;
import top.likoslupus.cellulosesz.api.item.ItemService;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.modules.item.ItemConfig;

public final class InvSeeCommand extends AbstractItemCommand {

    public InvSeeCommand(
            PlatformService platform,
            ItemService items,
            ItemAutomationService automation,
            ItemConfig config
    ) {
        super(platform, items, automation, config);
    }

    @Override
    public String permission() {
        return "cellulosesz.item.invsee";
    }

    @Override
    public CommandSourceKind sourceKind() {
        return CommandSourceKind.PLAYER_ONLY;
    }

    @Override
    public String usage() {
        return "/invsee <player>";
    }

    @Override
    public String name() {
        return "invsee";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var self = player(invocation);
        if (self.isEmpty()) return 0;

        var args = invocation.args();
        if (args.length != 1) {
            invocation.error("用法: %s".formatted(usage()));
            return 0;
        }

        var target = target(invocation, args[0]);
        if (target.isEmpty()) return 0;

        if (target.get().uuid().equals(self.get().uuid())) {
            invocation.error("不能查看自己的背包。");
            return 0;
        }

        if (!platform.openInventory(self.get(), target.get())) {
            invocation.error("无法打开目标背包。");
            return 0;
        }

        invocation.reply("正在查看 %s 的背包。".formatted(target.get().name()));
        return 1;
    }

}
