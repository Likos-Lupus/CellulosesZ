package top.likoslupus.cellulosesz.modules.economy.command;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.economy.WorthService;

import java.math.BigDecimal;
import java.text.MessageFormat;

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
            invocation.error(MessageFormat.format("用法: {0}", usage()));
            return 0;
        }

        if (args[1].equalsIgnoreCase("remove")) {
            worths.removeWorth(args[0]);
            invocation.reply(MessageFormat.format("已移除 {0} 的价值。 ", args[0]));
            return 1;
        }

        try {
            var amount = new BigDecimal(args[1]);
            worths.setWorth(args[0], amount);
            invocation.reply("已设置 %s 价值为 %s。 ".formatted(args[0], amount.toPlainString()));
            return 1;
        } catch (NumberFormatException exception) {
            invocation.error("金额格式错误: %s".formatted(args[1]));
            return 0;
        }
    }

}
