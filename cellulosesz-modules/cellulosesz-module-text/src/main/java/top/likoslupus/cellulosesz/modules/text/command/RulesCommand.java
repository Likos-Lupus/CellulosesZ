package top.likoslupus.cellulosesz.modules.text.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.text.TextService;

public final class RulesCommand extends PagedTextCommand {

    public RulesCommand(TextService texts) {
        super(texts);
    }

    @Override
    public String permission() {
        return "cellulosesz.text.rules";
    }

    @Override
    public String usage() {
        return "/rules [page]";
    }

    @Override
    public String name() {
        return "rules";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var page = page(invocation, 0);
        if (page == 0) {
            invocation.errorKey("commands.common.invalid-page");
            return 0;
        }
        return show(invocation, "commands.text.rules-title", texts.rules(), page);
    }

}
