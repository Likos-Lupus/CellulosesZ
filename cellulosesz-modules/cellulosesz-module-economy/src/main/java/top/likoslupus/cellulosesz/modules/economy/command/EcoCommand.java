package top.likoslupus.cellulosesz.modules.economy.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.economy.EconomyService;
import top.likoslupus.cellulosesz.api.economy.TransactionResult;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.user.UserService;
import top.likoslupus.cellulosesz.modules.economy.EconomyConfig;

import java.util.Map;
import java.util.Optional;

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
            invocation.errorKey(
                    "commands.economy.eco-command.error.1",
                    Map.of("value0", usage())
            );
            return 0;
        }

        var target = uuid(invocation, args[1]);
        var amount = amount(invocation, args[2]);
        if (target.isEmpty() || amount.isEmpty()) return 0;

        var cause = cause(invocation, "eco " + args[0]);
        Optional<TransactionResult> result = switch (args[0].toLowerCase()) {
            case "give", "add", "deposit" -> Optional.of(economy.deposit(target.get(), amount.get(), cause));
            case "take", "remove", "withdraw" -> Optional.of(economy.withdraw(target.get(), amount.get(), cause));
            case "set" -> Optional.of(economy.setBalance(target.get(), amount.get(), cause));
            default -> Optional.empty();
        };

        if (result.isEmpty()) {
            invocation.errorKey(
                    "commands.economy.eco-command.error.2",
                    Map.of("value0", usage())
            );
            return 0;
        }
        var transaction = result.orElseThrow();
        if (!transaction.success()) {
            invocation.error(transaction.message());
            return 0;
        }

        invocation.replyKey(
                "commands.economy.eco-result",
                Map.of(
                        "result", transaction.message(),
                        "balance", format(transaction.balance())
                )
        );
        return 1;
    }

}
