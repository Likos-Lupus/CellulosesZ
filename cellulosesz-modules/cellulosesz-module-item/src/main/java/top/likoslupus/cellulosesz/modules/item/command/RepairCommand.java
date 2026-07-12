package top.likoslupus.cellulosesz.modules.item.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.item.ItemAutomationService;
import top.likoslupus.cellulosesz.api.item.ItemService;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.modules.item.ItemConfig;

import java.util.Map;

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
            invocation.errorKey(
                    "commands.item.repair-command.error.1",
                    Map.of("value0", usage())
            );
            return 0;
        }

        if (all && (!config.repairAllEnabled || !invocation.hasPermission("cellulosesz.item.repair.all"))) {
            invocation.errorKey("commands.item.repair-command.error.2");
            return 0;
        }

        var repaired = platform.repairItems(self.get(), all);
        if (repaired <= 0) {
            invocation.errorKey("commands.item.repair-command.error.3");
            return 0;
        }

        invocation.replyKey(
                "commands.item.repair-command.reply.1",
                Map.of("value0", repaired)
        );
        return 1;
    }

}
