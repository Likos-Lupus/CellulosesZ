package top.likoslupus.cellulosesz.modules.text.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.text.TextService;

public final class InfoCommand extends PagedTextCommand {

    public InfoCommand(TextService texts) {
        super(texts);
    }

    @Override
    public String permission() {
        return "cellulosesz.text.info";
    }

    @Override
    public String usage() {
        return "/info [page]";
    }

    @Override
    public String name() {
        return "info";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var page = page(invocation, 0);
        if (page == 0) {
            invocation.errorKey("commands.common.invalid-page");
            return 0;
        }
        return show(invocation, "commands.text.info-title", texts.info(), page);
    }

}
