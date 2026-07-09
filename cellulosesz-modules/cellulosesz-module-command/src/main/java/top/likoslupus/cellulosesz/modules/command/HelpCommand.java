package top.likoslupus.cellulosesz.modules.command;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandRegistry;
import top.likoslupus.cellulosesz.api.module.ModuleContext;

public final class HelpCommand implements CellCommand {

    private final ModuleContext context;

    public HelpCommand(ModuleContext context) {
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
    public String name() {
        return "help";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var registry = context.services().require(CommandRegistry.class);
        invocation.reply("CellulosesZ commands:");
        registry.commands().stream()
                .filter(command -> command.permission().isBlank() || invocation.hasPermission(command.permission()))
                .forEach(command -> invocation.reply(" /" + command.name() + " - " + command.description()));
        return 1;
    }

}
