package top.likoslupus.cellulosesz.modules.text.command;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.text.TextService;

import java.util.Map;

public final class CustomTextCommand extends PagedTextCommand {

    public CustomTextCommand(TextService texts) {
        super(texts);
    }

    @Override
    public String permission() {
        return "cellulosesz.text.customtext";
    }

    @Override
    public String usage() {
        return "/customtext <name> [page]";
    }

    @Override
    public String name() {
        return "customtext";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var args = invocation.args();
        if (args.length < 1 || args.length > 2) {
            invocation.errorKey("commands.text.custom-usage", Map.of("usage", usage()));
            return 0;
        }
        var lines = texts.custom(args[0]);
        if (lines.isEmpty()) {
            invocation.errorKey("commands.text.custom-missing", Map.of("name", args[0]));
            return 0;
        }
        var page = page(invocation, 1);
        if (page == 0) {
            invocation.errorKey("commands.common.invalid-page");
            return 0;
        }
        return show(invocation, "commands.text.custom-title", lines, page);
    }

}
