package top.likoslupus.cellulosesz.modules.item.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.item.ItemAutomationService;
import top.likoslupus.cellulosesz.api.item.ItemService;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.modules.item.ItemConfig;

import java.util.List;

public final class PowerToolCommand extends AbstractItemCommand {

    public PowerToolCommand(
            PlatformService platform,
            ItemService items,
            ItemAutomationService automation,
            ItemConfig config
    ) {
        super(platform, items, automation, config);
    }

    @Override
    public List<String> aliases() {
        return List.of("pt");
    }

    @Override
    public String permission() {
        return "cellulosesz.item.powertool";
    }

    @Override
    public CommandSourceKind sourceKind() {
        return CommandSourceKind.PLAYER_ONLY;
    }

    @Override
    public String usage() {
        return "/powertool <command|clear|list|clearall>";
    }

    @Override
    public String name() {
        return "powertool";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var self = player(invocation);
        if (self.isEmpty()) return 0;

        var args = invocation.args();
        if (args.length == 1 && args[0].equalsIgnoreCase("list")) {
            var configured = automation.powerTools(self.get().uuid());
            if (configured.isEmpty()) {
                invocation.reply("没有 PowerTool 绑定。");
            } else {
                invocation.reply("PowerTool: %s".formatted(
                        configured.entrySet().stream()
                                .map(entry -> entry.getKey() + " -> /" + entry.getValue())
                                .sorted()
                                .reduce((left, right) -> left + "; " + right)
                                .orElse("")
                ));
            }
            return 1;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("clearall")) {
            automation.powerTools(self.get().uuid()).keySet()
                    .forEach(itemId -> automation.clearPowerTool(self.get().uuid(), itemId));
            invocation.reply("已清除全部 PowerTool 绑定。");
            return 1;
        }

        var held = items.heldItemId(self.get());
        if (held.isEmpty()) {
            invocation.error("请先手持一个物品。");
            return 0;
        }

        if (args.length == 0 || (args.length == 1 && args[0].equalsIgnoreCase("clear"))) {
            automation.clearPowerTool(self.get().uuid(), held.get());
            invocation.reply("已清除 %s 的 PowerTool 绑定。".formatted(held.get()));
            return 1;
        }

        var command = join(args, 0).replaceFirst("^/+", "").trim();
        if (command.isBlank()) {
            invocation.error("用法: %s".formatted(usage()));
            return 0;
        }

        automation.setPowerTool(self.get().uuid(), held.get(), command);
        invocation.reply("已将 %s 绑定到 /%s。".formatted(held.get(), command));
        return 1;
    }

}
