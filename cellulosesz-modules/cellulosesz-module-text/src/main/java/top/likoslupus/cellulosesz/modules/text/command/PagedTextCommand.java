package top.likoslupus.cellulosesz.modules.text.command;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.text.TextService;

import java.util.List;
import java.util.Map;

abstract class PagedTextCommand implements CellCommand {

    protected final TextService texts;

    PagedTextCommand(TextService texts) {
        this.texts = texts;
    }

    protected int show(CommandInvocation invocation, String titleKey, List<String> lines, int page) {
        if (page < 1) {
            invocation.errorKey("commands.common.invalid-page");
            return 0;
        }
        if (lines.isEmpty()) {
            invocation.errorKey("commands.text.empty");
            return 0;
        }
        var pageSize = Math.max(1, texts.pageSize());
        var pages = (lines.size() + pageSize - 1) / pageSize;
        if (page > pages) {
            invocation.errorKey("commands.common.page-out-of-range", Map.of("pages", pages));
            return 0;
        }
        invocation.replyKey(titleKey, Map.of("page", page, "pages", pages));
        final int start;
        try {
            start = Math.multiplyExact(page - 1, pageSize);
        } catch (ArithmeticException exception) {
            invocation.errorKey("commands.common.page-out-of-range", Map.of("pages", pages));
            return 0;
        }
        var end = Math.min((long) start + pageSize, lines.size());
        for (var index = start; index < end; index++) {
            invocation.replyKey("commands.text.line", Map.of("line", lines.get(index)));
        }
        return 1;
    }

    protected int page(CommandInvocation invocation, int argumentIndex) {
        if (invocation.args().length <= argumentIndex) return 1;
        try {
            return Integer.parseInt(invocation.args()[argumentIndex]);
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

}
