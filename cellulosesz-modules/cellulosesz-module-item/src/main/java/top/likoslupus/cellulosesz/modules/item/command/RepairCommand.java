package top.likoslupus.cellulosesz.modules.item.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.item.ItemAutomationService;
import top.likoslupus.cellulosesz.api.item.ItemService;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.modules.item.ItemConfig;

public final class RepairCommand extends AbstractItemCommand {

    public RepairCommand(
            PlatformService platform,
            ItemService items,
            ItemAutomationService automation,
            ItemConfig config
    ) {
        super(platform, items, automation, config);
    }

    @Override
    public String permission() {
        return "cellulosesz.item.repair";
    }

    @Override
    public CommandSourceKind sourceKind() {
        return CommandSourceKind.PLAYER_ONLY;
    }

    @Override
    public String usage() {
        return "/repair [hand|all]";
    }

    @Override
    public String name() {
        return "repair";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var self = player(invocation);
        if (self.isEmpty()) return 0;

        var args = invocation.args();
        var all = args.length == 1 && args[0].equalsIgnoreCase("all");
        if (args.length > 1 || (args.length == 1 && !all && !args[0].equalsIgnoreCase("hand"))) {
            invocation.error("用法: " + usage());
            return 0;
        }

        if (all && (!config.repairAllEnabled || !invocation.hasPermission("cellulosesz.item.repair.all"))) {
            invocation.error("你没有权限修复全部物品。");
            return 0;
        }

        var repaired = platform.repairItems(self.get(), all);
        if (repaired <= 0) {
            invocation.error("没有可修复的物品。");
            return 0;
        }

        invocation.reply("已修复 %d 件物品。".formatted(repaired));
        return 1;
    }

}
