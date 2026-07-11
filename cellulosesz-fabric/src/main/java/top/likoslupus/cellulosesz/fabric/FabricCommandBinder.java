package top.likoslupus.cellulosesz.fabric;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.core.bootstrap.CellulosesZBootstrap;
import top.likoslupus.cellulosesz.fabric.mixin.CommandNodeAccessor;

public final class FabricCommandBinder {

    private final CellulosesZBootstrap bootstrap;
    private final FabricVanillaCommandBridge vanillaCommands;

    public FabricCommandBinder(
            CellulosesZBootstrap bootstrap,
            FabricVanillaCommandBridge vanillaCommands
    ) {
        this.bootstrap = bootstrap;
        this.vanillaCommands = vanillaCommands;
    }

    public void bind(
            CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess,
            Commands.CommandSelection environment
    ) {
        vanillaCommands.capture(dispatcher);
        bootstrap.commandRegistry().commands().forEach(command -> {
            register(dispatcher, command, command.name());
            command.aliases().forEach(alias ->
                    register(dispatcher, command, alias)
            );
        });
    }

    private void register(
            CommandDispatcher<CommandSourceStack> dispatcher,
            CellCommand command,
            String label
    ) {
        if (dispatcher.getRoot().getChild(label) != null) {
            removeRootCommand(dispatcher, label);
        }
        dispatcher.register(Commands.literal(label)
                .requires(source -> permitted(source, command))
                .executes(context -> execute(
                        context.getSource(),
                        command,
                        label,
                        new String[0]
                ))
                .then(Commands.argument("args", StringArgumentType.greedyString())
                        .executes(context -> execute(
                                context.getSource(),
                                command,
                                label,
                                split(StringArgumentType.getString(context, "args"))
                        ))
                )
        );
    }

    private void removeRootCommand(CommandDispatcher<CommandSourceStack> dispatcher, String label) {
        @SuppressWarnings("unchecked")
        var root = (CommandNodeAccessor<CommandSourceStack>) dispatcher.getRoot();
        root.cellulosesz$children().remove(label);
        root.cellulosesz$literals().remove(label);
    }

    private boolean permitted(CommandSourceStack source, CellCommand command) {
        if (!bootstrap.permissionService().has(source, command.permission())) {
            return false;
        }
        if (command.sourceKind() == CommandSourceKind.PLAYER_ONLY
                && !(source.getEntity() instanceof ServerPlayer)) {
            return false;
        }
        return command.sourceKind() != CommandSourceKind.CONSOLE_ONLY
                || !(source.getEntity() instanceof ServerPlayer);
    }

    private int execute(
            CommandSourceStack source,
            CellCommand command,
            String label,
            String[] args
    ) {
        return bootstrap.commandRegistry().execute(
                command,
                new FabricCommandInvocation(source, bootstrap.permissionService(), label, args)
        );
    }

    private String[] split(String input) {
        if (input.isBlank()) {
            return new String[0];
        }
        return input.trim().split("\\s+");
    }

}
