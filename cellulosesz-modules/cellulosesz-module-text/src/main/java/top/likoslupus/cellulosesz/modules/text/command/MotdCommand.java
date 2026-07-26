package top.likoslupus.cellulosesz.modules.text.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.text.TextService;

public final class MotdCommand extends PagedTextCommand {

    public MotdCommand(TextService texts) {
        super(texts);
    }

    @Override
    public String permission() {
        return "cellulosesz.text.motd";
    }

    @Override
    public String usage() {
        return "/motd [page]";
    }

    @Override
    public String name() {
        return "motd";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var page = page(invocation, 0);
        if (page == 0) {
            invocation.errorKey("commands.common.invalid-page");
            return 0;
        }
        return show(invocation, "commands.text.motd-title", texts.motd(), page);
    }

}
