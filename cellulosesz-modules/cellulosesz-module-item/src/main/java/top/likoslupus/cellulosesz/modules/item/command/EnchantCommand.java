package top.likoslupus.cellulosesz.modules.item.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.item.ItemAutomationService;
import top.likoslupus.cellulosesz.api.item.ItemService;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.modules.item.ItemConfig;

public final class EnchantCommand extends AbstractItemCommand {

    public EnchantCommand(
            PlatformService platform,
            ItemService items,
            ItemAutomationService automation,
            ItemConfig config
    ) {
        super(platform, items, automation, config);
    }

    @Override
    public String permission() {
        return "cellulosesz.item.enchant";
    }

    @Override
    public CommandSourceKind sourceKind() {
        return CommandSourceKind.PLAYER_ONLY;
    }

    @Override
    public String usage() {
        return "/enchant <enchantment> [level]";
    }

    @Override
    public String name() {
        return "enchant";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var self = player(invocation);
        if (self.isEmpty()) return 0;

        var args = invocation.args();
        if (args.length < 1 || args.length > 2) {
            invocation.error("用法: " + usage());
            return 0;
        }

        var level = 1;
        if (args.length == 2) {
            try {
                level = Integer.parseInt(args[1]);
            } catch (NumberFormatException _) {
                invocation.error("附魔等级必须是整数。");
                return 0;
            }
        }

        if (level <= 0 || (!config.allowUnsafeEnchantments && level > 255)) {
            invocation.error("附魔等级超出允许范围。");
            return 0;
        }

        if (!platform.enchantHeldItem(self.get(), args[0], level)) {
            invocation.error("附魔失败，请确认手持物品与附魔 ID 有效。");
            return 0;
        }

        invocation.reply("已应用附魔 %s %d。".formatted(args[0], level));
        return 1;
    }

}
