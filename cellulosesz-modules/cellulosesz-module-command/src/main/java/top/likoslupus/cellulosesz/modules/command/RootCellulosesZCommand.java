package top.likoslupus.cellulosesz.modules.command;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
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
        var args = invocation.args();

        if (args.length == 0) {
            invocation.replyKey(
                    "cellulosesz.version",
                    Map.of("version", runtime.version())
            );
            invocation.replyKey(
                    "commands.command.root-celluloses-z-command.reply.1",
                    Map.of("value0", usage())
            );
            return 1;
        }

        var subcommand = args[0].toLowerCase(Locale.ROOT);
        return switch (subcommand) {
            case "version" -> version(invocation, runtime);
            case "reload" -> reload(invocation, runtime);
            case "modules" -> modules(invocation, runtime);
            case "debug" -> debug(invocation, runtime);
            default -> {
                invocation.errorKey("cellulosesz.unknown-subcommand");
                yield 0;
            }
        };
    }

    private int version(
            CommandInvocation invocation,
            RuntimeService runtime
    ) {
        invocation.replyKey(
                "cellulosesz.version",
                Map.of("version", runtime.version())
        );
        return 1;
    }

    private int reload(
            CommandInvocation invocation,
            RuntimeService runtime
    ) {
        if (!invocation.hasPermission("cellulosesz.command.reload")) {
            invocation.errorKey("common.no-permission");
            return 0;
        }
        runtime.reload();
        invocation.replyKey("cellulosesz.reloaded");
        return 1;
    }

    private int modules(
            CommandInvocation invocation,
            RuntimeService runtime
    ) {
        if (!invocation.hasPermission("cellulosesz.command.modules")) {
            invocation.errorKey("common.no-permission");
            return 0;
        }

        invocation.replyKey("cellulosesz.modules-header");
        runtime.modules().stream()
                .map(module -> " - %s [%s] %s".formatted(
                        module.id(),
                        module.enabled()
                                ? "enabled"
                                : "disabled",
                        module.phase()
                ))
                .forEach(row -> invocation.replyKey(
                        "cellulosesz.module-row",
                        Map.of("module", row)
                ));
        return 1;
    }

    private int debug(CommandInvocation invocation, RuntimeService runtime) {
        if (!invocation.hasPermission("cellulosesz.command.debug")) {
            invocation.errorKey("common.no-permission");
            return 0;
        }
        invocation.replyKey(
                "commands.command.root-celluloses-z-command.reply.2",
                Map.of(
                        "value0", runtime.version(),
                        "value1", runtime.modules().size()
                )
        );
        return 1;
    }

}
