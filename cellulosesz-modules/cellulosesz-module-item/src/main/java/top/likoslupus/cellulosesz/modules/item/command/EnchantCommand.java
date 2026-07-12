package top.likoslupus.cellulosesz.modules.item.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.item.ItemAutomationService;
import top.likoslupus.cellulosesz.api.item.ItemService;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.modules.item.ItemConfig;

import java.util.Map;

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
            invocation.errorKey(
                    "commands.item.enchant-command.error.1",
                    Map.of("value0", usage())
            );
            return 0;
        }

        var level = 1;
        if (args.length == 2) {
            try {
                level = Integer.parseInt(args[1]);
            } catch (NumberFormatException _) {
                invocation.errorKey("commands.item.enchant-command.error.2");
                return 0;
            }
        }

        if (level <= 0 || (!config.allowUnsafeEnchantments && level > 255)) {
            invocation.errorKey("commands.item.enchant-command.error.3");
            return 0;
        }

        if (!platform.enchantHeldItem(self.get(), args[0], level)) {
            invocation.errorKey("commands.item.enchant-command.error.4");
            return 0;
        }

        invocation.replyKey(
                "commands.item.enchant-command.reply.1",
                Map.of(
                        "value0", args[0],
                        "value1", level
                )
        );
        return 1;
    }

}
