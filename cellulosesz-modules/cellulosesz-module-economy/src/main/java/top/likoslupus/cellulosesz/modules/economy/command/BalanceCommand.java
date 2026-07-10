package top.likoslupus.cellulosesz.modules.economy.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.economy.EconomyService;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.user.UserService;
import top.likoslupus.cellulosesz.modules.economy.EconomyConfig;

import java.util.List;

public final class BalanceCommand extends AbstractEconomyCommand {

    public BalanceCommand(
            PlatformService platform,
            UserService users,
            EconomyService economy,
            EconomyConfig config
    ) {
        super(platform, users, economy, config);
    }

    @Override
    public List<String> aliases() {
        return List.of("bal", "money");
    }

    @Override
    public String permission() {
        return "cellulosesz.economy.balance";
    }

    @Override
    public String usage() {
        return "/balance [player]";
    }

    @Override
    public String name() {
        return "balance";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var args = invocation.args();
        if (args.length == 0) {
            var self = player(invocation);
            if (self.isEmpty()) return 0;
            invocation.reply("余额: " + format(economy.balance(self.get().uuid())));
            return 1;
        }

        if (!invocation.hasPermission("cellulosesz.economy.balance.other")) {
            invocation.error("你没有权限查看其他玩家余额。");
            return 0;
        }

        var target = uuid(invocation, args[0]);
        if (target.isEmpty()) return 0;
        invocation.reply(args[0] + " 的余额: " + format(economy.balance(target.get())));
        return 1;
    }

}
