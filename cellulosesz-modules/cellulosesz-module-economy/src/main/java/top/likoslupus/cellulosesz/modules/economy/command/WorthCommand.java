package top.likoslupus.cellulosesz.modules.economy.command;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.economy.WorthService;

import java.util.Map;

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
            invocation.errorKey(
                    "commands.economy.worth-command.error.1",
                    Map.of("value0", usage())
            );
            return 0;
        }
        var worth = worths.worth(args[0]);
        if (worth.isPresent()) {
            invocation.replyKey(
                    "commands.economy.worth",
                    Map.of(
                            "item", args[0],
                            "worth", worth.get().toPlainString()
                    )
            );
        } else {
            invocation.replyKey(
                    "commands.economy.worth-missing",
                    Map.of("item", args[0])
            );
        }
        return 1;
    }

}
