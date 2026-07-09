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

public final class FabricCommandBinder {

    private final CellulosesZBootstrap bootstrap;

    public FabricCommandBinder(CellulosesZBootstrap bootstrap) {
        this.bootstrap = bootstrap;
    }

    public void bind(
            CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess,
            Commands.CommandSelection environment
    ) {
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
