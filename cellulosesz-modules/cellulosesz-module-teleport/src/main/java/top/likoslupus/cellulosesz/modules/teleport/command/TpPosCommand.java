package top.likoslupus.cellulosesz.modules.teleport.command;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.api.world.WorldDirectory;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.common.command.CommandSuggestionSupport;
import top.likoslupus.cellulosesz.modules.teleport.application.TeleportCommandService;

import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public final class TpPosCommand implements CommandContributor {

    private final TeleportCommandService service;
    private final PlayerDirectory players;
    private final WorldDirectory worlds;

    public TpPosCommand(
            TeleportCommandService service,
            PlayerDirectory players,
            WorldDirectory worlds
    ) {
        this.service = requireNonNull(service, "service");
        this.players = requireNonNull(players, "players");
        this.worlds = requireNonNull(worlds, "worlds");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = TeleportCommandResults.descriptor(
                "tppos",
                "cellulosesz.teleport.tppos",
                CommandSourceKind.PLAYER_ONLY
        );

        var z = Commands.argument("z", DoubleArgumentType.doubleArg())
                .executes(command -> run(
                        context,
                        command,
                        descriptor,
                        Optional.empty()
                ))
                .then(Commands.argument("world", StringArgumentType.word())
                        .suggests((_, builder) ->
                                CommandSuggestionSupport.suggest(
                                        worlds::loadedWorldIds, builder
                                )
                        )
                        .executes(command -> run(
                                context,
                                command,
                                descriptor,
                                Optional.of(StringArgumentType.getString(command, "world"))
                        ))
                );

        var root = Commands.literal("tppos")
                .then(Commands.argument("x", DoubleArgumentType.doubleArg())
                        .then(Commands.argument("y", DoubleArgumentType.doubleArg())
                                .then(z)
                        )
                );

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.tppos",
                "/tppos <x> <y> <z> [world]",
                root
        );
    }

    private int run(
            CommandRegistrationContext context,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor,
            Optional<String> world
    ) {
        return TeleportCommandResults.player(
                context,
                command,
                descriptor,
                "tppos",
                players,
                player -> service.position(
                        player,
                        DoubleArgumentType.getDouble(command, "x"),
                        DoubleArgumentType.getDouble(command, "y"),
                        DoubleArgumentType.getDouble(command, "z"),
                        world
                )
        );
    }

    @Override
    public String moduleId() {
        return TeleportCommandResults.MODULE;
    }

}
