package top.likoslupus.cellulosesz.modules.item.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.item.ItemAutomationService;
import top.likoslupus.cellulosesz.api.item.ItemService;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.modules.item.ItemConfig;

public final class UnlimitedCommand extends AbstractItemCommand {

    public UnlimitedCommand(
            PlatformService platform,
            ItemService items,
            ItemAutomationService automation,
            ItemConfig config
    ) {
        super(platform, items, automation, config);
    }

    @Override
    public String permission() {
        return "cellulosesz.item.unlimited";
    }

    @Override
    public CommandSourceKind sourceKind() {
        return CommandSourceKind.PLAYER_ONLY;
    }

    @Override
    public String usage() {
        return "/unlimited [on|off|list|clear]";
    }

    @Override
    public String name() {
        return "unlimited";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var self = player(invocation);
        if (self.isEmpty()) return 0;

        var args = invocation.args();
        if (args.length == 1 && args[0].equalsIgnoreCase("list")) {
            var configured = automation.unlimitedItems(self.get().uuid());
            invocation.reply(configured.isEmpty()
                    ? "没有无限物品。"
                    : "无限物品: %s".formatted(String.join(", ", configured)));
            return 1;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("clear")) {
            automation.unlimitedItems(self.get().uuid())
                    .forEach(itemId -> automation.setUnlimited(
                            self.get().uuid(),
                            itemId,
                            false
                    ));
            invocation.reply("已清除全部无限物品。");
            return 1;
        }

        if (args.length > 1) {
            invocation.error("用法: %s".formatted(usage()));
            return 0;
        }

        var held = items.heldItemId(self.get());
        if (held.isEmpty()) {
            invocation.error("请先手持一个物品。");
            return 0;
        }

        var enabled = args.length == 0
                ? !automation.unlimited(self.get().uuid(), held.get())
                : args[0].equalsIgnoreCase("on")
                  || args[0].equalsIgnoreCase("enable")
                  || args[0].equalsIgnoreCase("true");
        if (args.length == 1 && !(
                enabled || args[0].equalsIgnoreCase("off")
                        || args[0].equalsIgnoreCase("disable")
                        || args[0].equalsIgnoreCase("false")
        )) {
            invocation.error("用法: " + usage());
            return 0;
        }

        automation.setUnlimited(
                self.get().uuid(),
                held.get(),
                enabled
        );
        automation.maintainUnlimited(self.get());
        invocation.reply("%s%s 的无限补充。".formatted(enabled ? "已启用 " : "已禁用 ", held.get()));
        return 1;
    }

}
