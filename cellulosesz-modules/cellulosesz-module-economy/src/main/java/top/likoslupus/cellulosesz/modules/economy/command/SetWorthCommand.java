package top.likoslupus.cellulosesz.modules.economy.command;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.economy.WorthService;

import java.math.BigDecimal;
import java.util.Map;

public final class SetWorthCommand implements CellCommand {

    private final WorthService worths;

    public SetWorthCommand(WorthService worths) {
        this.worths = worths;
    }

    @Override
    public String permission() {
        return "cellulosesz.economy.setworth";
    }

    @Override
    public String usage() {
        return "/setworth <item> <amount|remove>";
    }

    @Override
    public String name() {
        return "setworth";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var args = invocation.args();
        if (args.length != 2) {
            invocation.errorKey("common.usage", Map.of("usage", usage()));
            return 0;
        }

        if (args[1].equalsIgnoreCase("remove")) {
            try {
                worths.removeWorth(args[0]).whenComplete((removed, failure) -> {
                    if (failure != null) {
                        invocation.errorKey("common.persistence-failed");
                    } else if (!removed) {
                        invocation.errorKey("commands.economy.worth-missing", Map.of("item", args[0]));
                    } else {
                        invocation.replyKey("commands.economy.worth-removed", Map.of("item", args[0]));
                    }
                });
                return 1;
            } catch (RuntimeException _) {
                invocation.errorKey("commands.economy.sell.invalid-item", Map.of("item", args[0]));
                return 0;
            }
        }

        final BigDecimal amount;
        try {
            amount = new BigDecimal(args[1]);
            if (amount.signum() < 0) throw new NumberFormatException();
        } catch (NumberFormatException _) {
            invocation.errorKey("commands.economy.set-worth-command.error.1", Map.of("value0", args[1]));
            return 0;
        }

        try {
            worths.setWorth(args[0], amount).whenComplete((_, failure) -> {
                if (failure != null) {
                    invocation.errorKey("common.persistence-failed");
                } else {
                    invocation.replyKey(
                            "commands.economy.set-worth-command.reply.1",
                            Map.of("value0", args[0], "value1", amount.stripTrailingZeros().toPlainString())
                    );
                }
            });
            return 1;
        } catch (RuntimeException _) {
            invocation.errorKey("commands.economy.sell.invalid-item", Map.of("item", args[0]));
            return 0;
        }
    }

}
