package top.likoslupus.cellulosesz.modules.command;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandRegistry;
import top.likoslupus.cellulosesz.api.module.ModuleContext;

import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.stream.IntStream;

public final class HelpCommand implements CellCommand {

    private final ModuleContext context;
    private final CommandConfig config;

    public HelpCommand(
            ModuleContext context,
            CommandConfig config
    ) {
        this.context = context;
        this.config = config;
    }

    @Override
    public String permission() {
        return "cellulosesz.command.help";
    }

    @Override
    public String description() {
        return "Lists registered CellulosesZ commands.";
    }

    @Override
    public String usage() {
        return "/help [query] [page]";
    }

    @Override
    public String name() {
        return "help";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var args = invocation.args();
        if (args.length > 2) {
            invocation.errorKey("commands.command.help-usage", Map.of("usage", usage()));
            return 0;
        }
        var query = "";
        var page = 1;

        if (args.length > 0) {
            try {
                page = Integer.parseInt(args[0]);
            } catch (NumberFormatException _) {
                query = args[0].toLowerCase(Locale.ROOT);
                if (args.length > 1) {
                    try {
                        page = Integer.parseInt(args[1]);
                    } catch (NumberFormatException _) {
                        invocation.errorKey("commands.common.invalid-page");
                        return 0;
                    }
                }
            }
        }

        if (page < 1) {
            invocation.errorKey("commands.common.invalid-page");
            return 0;
        }

        var search = query;
        var registry = context.services().require(CommandRegistry.class);
        var visible = registry.commands().stream()
                .filter(command -> command.permission().isBlank()
                        || invocation.hasPermission(command.permission())
                )
                .filter(command -> switch (command.sourceKind()) {
                    case ANY -> true;
                    case PLAYER_ONLY -> invocation.player();
                    case CONSOLE_ONLY -> !invocation.player();
                })
                .filter(command -> search.isBlank()
                        || command.name().toLowerCase(Locale.ROOT).contains(search)
                        || command.aliases().stream().anyMatch(alias -> alias.toLowerCase(Locale.ROOT).contains(search))
                        || command.description().toLowerCase(Locale.ROOT).contains(search)
                        || command.usage().toLowerCase(Locale.ROOT).contains(search)
                )
                .sorted(Comparator.comparing(CellCommand::name))
                .toList();

        if (visible.isEmpty()) {
            invocation.errorKey(
                    "commands.command.help-empty",
                    Map.of("query", query)
            );
            return 0;
        }

        var pageSize = Math.max(1, config.helpPageSize);
        var pages = (visible.size() + pageSize - 1) / pageSize;

        if (page > pages) {
            invocation.errorKey(
                    "commands.common.page-out-of-range",
                    Map.of("pages", pages)
            );
            return 0;
        }

        invocation.replyKey(
                "commands.command.help-header",
                Map.of(
                        "page", page,
                        "pages", pages,
                        "query", query
                )
        );

        final int start;
        try {
            start = Math.multiplyExact(page - 1, pageSize);
        } catch (ArithmeticException exception) {
            invocation.errorKey("commands.common.page-out-of-range", Map.of("pages", pages));
            return 0;
        }
        IntStream.range(start, (int) Math.min((long) start + pageSize, visible.size()))
                .mapToObj(visible::get)
                .forEach(command -> invocation.replyKey(
                        "commands.command.help-entry",
                        Map.of(
                                "command", command.name(),
                                "description", command.description(),
                                "usage", command.usage()
                        )
                ));
        return 1;
    }

}
