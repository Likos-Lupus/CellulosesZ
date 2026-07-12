package top.likoslupus.cellulosesz.modules.item.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.item.ItemAutomationService;
import top.likoslupus.cellulosesz.api.item.ItemService;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.modules.item.ItemConfig;

import java.util.List;
import java.util.Map;

public final class EnderChestCommand extends AbstractItemCommand {

    public EnderChestCommand(
            PlatformService platform,
            ItemService items,
            ItemAutomationService automation,
            ItemConfig config
    ) {
        super(platform, items, automation, config);
    }

    @Override
    public List<String> aliases() {
        return List.of("echest");
    }

    @Override
    public String permission() {
        return "cellulosesz.item.enderchest";
    }

    @Override
    public CommandSourceKind sourceKind() {
        return CommandSourceKind.PLAYER_ONLY;
    }

    @Override
    public String usage() {
        return "/enderchest [player]";
    }

    @Override
    public String name() {
        return "enderchest";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var self = player(invocation);
        if (self.isEmpty()) return 0;

        var args = invocation.args();
        if (args.length > 1) {
            invocation.errorKey(
                    "commands.item.ender-chest-command.error.1",
                    Map.of("value0", usage())
            );
            return 0;
        }

        var target = self;
        if (args.length == 1) {
            if (!invocation.hasPermission("cellulosesz.item.enderchest.other")) {
                invocation.errorKey("commands.item.ender-chest-command.error.2");
                return 0;
            }

            target = target(invocation, args[0]);
            if (target.isEmpty()) return 0;
        }

        if (!platform.openEnderChest(self.get(), target.get())) {
            invocation.errorKey("commands.item.ender-chest-command.error.3");
            return 0;
        }

        return 1;
    }

}
