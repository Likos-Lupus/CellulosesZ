package top.likoslupus.cellulosesz.modules.economy.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.economy.EconomyService;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.user.UserService;
import top.likoslupus.cellulosesz.modules.economy.EconomyConfig;

public final class EcoCommand extends AbstractEconomyCommand {

    public EcoCommand(
            PlatformService platform,
            UserService users,
            EconomyService economy,
            EconomyConfig config
    ) {
        super(platform, users, economy, config);
    }

    @Override
    public String permission() {
        return "cellulosesz.economy.admin";
    }

    @Override
    public String usage() {
        return "/eco <give|take|set> <player> <amount>";
    }

    @Override
    public String name() {
        return "eco";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var args = invocation.args();
        if (args.length != 3) {
            invocation.error("用法: " + usage());
            return 0;
        }

        var target = uuid(invocation, args[1]);
        var amount = amount(invocation, args[2]);
        if (target.isEmpty() || amount.isEmpty()) return 0;

        var cause = cause(invocation, "eco " + args[0]);
        var result = switch (args[0].toLowerCase()) {
            case "give", "add", "deposit" -> economy.deposit(target.get(), amount.get(), cause);
            case "take", "remove", "withdraw" -> economy.withdraw(target.get(), amount.get(), cause);
            case "set" -> economy.setBalance(target.get(), amount.get(), cause);
            default -> null;
        };

        if (result == null) {
            invocation.error("用法: " + usage());
            return 0;
        }
        if (!result.success()) {
            invocation.error(result.message());
            return 0;
        }

        invocation.reply(result.message() + " 当前余额: " + format(economy.balance(target.get())));
        return 1;
    }

}
