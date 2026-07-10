package top.likoslupus.cellulosesz.modules.economy.command;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.economy.WorthService;

public final class WorthCommand implements CellCommand {

    private final WorthService worths;

    public WorthCommand(WorthService worths) {
        this.worths = worths;
    }

    @Override
    public String permission() {
        return "cellulosesz.economy.worth";
    }

    @Override
    public String usage() {
        return "/worth <item>";
    }

    @Override
    public String name() {
        return "worth";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var args = invocation.args();
        if (args.length != 1) {
            invocation.error("用法: " + usage());
            return 0;
        }
        var worth = worths.worth(args[0]);
        invocation.reply(worth.map(value -> "%s 价值: %s".formatted(args[0], value.toPlainString()))
                .orElse(args[0] + " 没有设置价值。"));
        return 1;
    }

}
