package top.likoslupus.cellulosesz.modules.command;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.i18n.MessageService;
import top.likoslupus.cellulosesz.api.module.ModuleContext;
import top.likoslupus.cellulosesz.api.runtime.RuntimeService;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class RootCellulosesZCommand implements CellCommand {

    private final ModuleContext context;

    public RootCellulosesZCommand(ModuleContext context) {
        this.context = context;
    }

    @Override
    public List<String> aliases() {
        return List.of("cellz", "cz");
    }

    @Override
    public String permission() {
        return "cellulosesz.command.root";
    }

    @Override
    public String description() {
        return "CellulosesZ administration command.";
    }

    @Override
    public String usage() {
        return "/cellulosesz <version|reload|modules|debug>";
    }

    @Override
    public String name() {
        return "cellulosesz";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var runtime = context.services().require(RuntimeService.class);
        var messages = context.services().require(MessageService.class);
        var args = invocation.args();

        if (args.length == 0) {
            invocation.reply(messages.message("cellulosesz.version", Map.of("version", runtime.version())));
            invocation.reply("Usage: " + usage());
            return 1;
        }

        var subcommand = args[0].toLowerCase(Locale.ROOT);
        return switch (subcommand) {
            case "version" -> version(invocation, runtime, messages);
            case "reload" -> reload(invocation, runtime, messages);
            case "modules" -> modules(invocation, runtime, messages);
            case "debug" -> debug(invocation, runtime);
            default -> {
                invocation.error(messages.message("cellulosesz.unknown-subcommand"));
                yield 0;
            }
        };
    }

    private int version(
            CommandInvocation invocation,
            RuntimeService runtime,
            MessageService messages
    ) {
        invocation.reply(messages.message("cellulosesz.version", Map.of("version", runtime.version())));
        return 1;
    }

    private int reload(
            CommandInvocation invocation,
            RuntimeService runtime,
            MessageService messages
    ) {
        if (!invocation.hasPermission("cellulosesz.command.reload")) {
            invocation.error(messages.message("common.no-permission"));
            return 0;
        }
        runtime.reload();
        invocation.reply(messages.message("cellulosesz.reloaded"));
        return 1;
    }

    private int modules(
            CommandInvocation invocation,
            RuntimeService runtime,
            MessageService messages
    ) {
        if (!invocation.hasPermission("cellulosesz.command.modules")) {
            invocation.error(messages.message("common.no-permission"));
            return 0;
        }

        invocation.reply(messages.message("cellulosesz.modules-header"));
        runtime.modules().stream()
                .map(module -> " - %s [%s] %s".formatted(
                        module.id(),
                        module.enabled()
                                ? "enabled"
                                : "disabled",
                        module.phase()
                ))
                .forEach(invocation::reply);
        return 1;
    }

    private int debug(CommandInvocation invocation, RuntimeService runtime) {
        if (!invocation.hasPermission("cellulosesz.command.debug")) {
            invocation.error(context.services().require(MessageService.class).message("common.no-permission"));
            return 0;
        }
        invocation.reply("CellulosesZ debug: version=" + runtime.version() + ", modules=" + runtime.modules().size());
        return 1;
    }

}
