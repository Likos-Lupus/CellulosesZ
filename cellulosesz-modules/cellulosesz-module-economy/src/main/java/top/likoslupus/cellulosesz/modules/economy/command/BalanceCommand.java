package top.likoslupus.cellulosesz.modules.economy.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.economy.EconomyService;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.user.UserService;
import top.likoslupus.cellulosesz.modules.economy.EconomyConfig;

import java.util.List;
import java.util.Map;

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
            invocation.replyKey(
                    "commands.economy.balance-command.reply.1",
                    Map.of("value0", format(economy.balance(self.get().uuid())))
            );
            return 1;
        }

        if (!invocation.hasPermission("cellulosesz.economy.balance.other")) {
            invocation.errorKey("commands.economy.balance-command.error.1");
            return 0;
        }

        var target = uuid(invocation, args[0]);
        if (target.isEmpty()) return 0;
        invocation.replyKey(
                "commands.economy.balance-other",
                Map.of(
                        "player", args[0],
                        "balance", format(economy.balance(target.get()))
                )
        );
        return 1;
    }

}
