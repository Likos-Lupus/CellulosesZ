package top.likoslupus.cellulosesz.modules.command;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandRegistry;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.command.catalog.CommandCatalog;
import top.likoslupus.cellulosesz.api.command.service.CommandAliasRegistry;
import top.likoslupus.cellulosesz.api.module.ModuleContext;
import top.likoslupus.cellulosesz.core.command.DefaultCommandRegistry;
import top.likoslupus.cellulosesz.core.i18n.GeneratedMessageKeys;

import java.util.*;
import java.util.stream.IntStream;

public final class HelpCommand implements CellCommand {

    private final ModuleContext context;

    public HelpCommand(
            ModuleContext context,
            CommandConfig ignored
    ) {
        this.context = context;
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
            invocation.errorKey(GeneratedMessageKeys.COMMANDS_COMMAND_HELP_USAGE, Map.of("usage", usage()));
            return 0;
        }
        var query = "";
        var page = 1;
        if (args.length > 0) {
            try {
                page = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {
                query = args[0].toLowerCase(Locale.ROOT);
                if (args.length > 1) {
                    try {
                        page = Integer.parseInt(args[1]);
                    } catch (NumberFormatException failure) {
                        invocation.errorKey(GeneratedMessageKeys.COMMANDS_COMMON_INVALID_PAGE);
                        return 0;
                    }
                }
            }
        }
        if (page < 1) {
            invocation.errorKey(GeneratedMessageKeys.COMMANDS_COMMON_INVALID_PAGE);
            return 0;
        }

        var search = query;
        var visible = entries().stream()
                .filter(entry -> entry.permission().isBlank() || invocation.hasPermission(entry.permission()))
                .filter(entry -> switch (entry.sourceKind()) {
                    case ANY -> true;
                    case PLAYER_ONLY -> invocation.player();
                    case CONSOLE_ONLY -> !invocation.player();
                })
                .filter(entry -> search.isBlank()
                        || entry.name().contains(search)
                        || entry.aliases().stream().anyMatch(alias -> alias.contains(search))
                        || entry.description().toLowerCase(Locale.ROOT).contains(search)
                        || entry.usage().toLowerCase(Locale.ROOT).contains(search))
                .sorted(Comparator.comparing(HelpEntry::name))
                .toList();
        if (visible.isEmpty()) {
            invocation.errorKey(GeneratedMessageKeys.COMMANDS_COMMAND_HELP_EMPTY, Map.of("query", query));
            return 0;
        }

        var config = context.configs().require("module.command", CommandConfig.class);
        var pageSize = Math.max(1, config.helpPageSize);
        var pages = (visible.size() + pageSize - 1) / pageSize;
        if (page > pages) {
            invocation.errorKey(GeneratedMessageKeys.COMMANDS_COMMON_PAGE_OUT_OF_RANGE, Map.of("pages", pages));
            return 0;
        }
        invocation.replyKey(GeneratedMessageKeys.COMMANDS_COMMAND_HELP_HEADER, Map.of(
                "page", page,
                "pages", pages,
                "query", query
        ));
        final int start;
        try {
            start = Math.multiplyExact(page - 1, pageSize);
        } catch (ArithmeticException failure) {
            invocation.errorKey(GeneratedMessageKeys.COMMANDS_COMMON_PAGE_OUT_OF_RANGE, Map.of("pages", pages));
            return 0;
        }
        IntStream.range(start, (int) Math.min((long) start + pageSize, visible.size()))
                .mapToObj(visible::get)
                .forEach(entry -> invocation.replyKey(GeneratedMessageKeys.COMMANDS_COMMAND_HELP_ENTRY, Map.of(
                        "command", entry.name(),
                        "description", entry.description(),
                        "usage", entry.usage()
                )));
        return 1;
    }

    private List<HelpEntry> entries() {
        var result = new LinkedHashMap<String, HelpEntry>();
        var registry = context.services().require(CommandRegistry.class);
        var concrete = context.services().require(DefaultCommandRegistry.class);
        var aliases = context.services().require(CommandAliasRegistry.class);
        registry.commands().stream()
                .filter(command -> !concrete.disabled(command.name()))
                .filter(command -> concrete.moduleId(command)
                        .map(context::moduleEnabled)
                        .orElse(false))
                .map(command -> new HelpEntry(
                        command.name().toLowerCase(Locale.ROOT),
                        aliases.aliases(command.name()),
                        command.permission(),
                        command.sourceKind(),
                        command.description(),
                        command.usage()
                ))
                .forEach(entry -> result.put(entry.name(), entry));
        context.services().require(CommandCatalog.class).directCommands().forEach(entry -> {
            var descriptor = entry.descriptor();
            if (!context.moduleEnabled(descriptor.moduleId())) return;
            var visibleAliases = new java.util.LinkedHashSet<String>();
            visibleAliases.addAll(entry.aliases());
            visibleAliases.addAll(aliases.aliases(descriptor.canonicalName()));
            result.put(descriptor.canonicalName(), new HelpEntry(
                    descriptor.canonicalName(),
                    visibleAliases.stream().map(alias -> alias.toLowerCase(Locale.ROOT)).toList(),
                    descriptor.permission(),
                    descriptor.requiredSourceKind(),
                    entry.description(),
                    entry.usage()
            ));
        });
        return new ArrayList<>(result.values());
    }

    private record HelpEntry(
            String name,
            List<String> aliases,
            String permission,
            CommandSourceKind sourceKind,
            String description,
            String usage
    ) {

    }

}
