package top.likoslupus.cellulosesz.modules.teleport.command;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.api.world.WorldDirectory;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.common.command.CommandSuggestionSupport;
import top.likoslupus.cellulosesz.modules.teleport.application.RandomTeleportCommandService;

import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public final class SetTprCommand implements CommandContributor {

    private final RandomTeleportCommandService service;
    private final PlayerDirectory players;
    private final WorldDirectory worlds;

    public SetTprCommand(
            RandomTeleportCommandService service,
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
                "settpr",
                "cellulosesz.teleport.settpr",
                CommandSourceKind.ANY
        );

        var center = Commands.literal("center")
                .executes(command -> TeleportCommandResults.async(
                        context,
                        command,
                        descriptor,
                        "settpr center",
                        policy -> service.center(
                                TeleportCommandResults.current(policy, players),
                                StringArgumentType.getString(command, "world"),
                                Optional.empty()
                        )
                ))
                .then(Commands.argument("x", DoubleArgumentType.doubleArg())
                        .then(Commands.argument("z", DoubleArgumentType.doubleArg())
                                .executes(command -> TeleportCommandResults.async(
                                        context,
                                        command,
                                        descriptor,
                                        "settpr center coordinates",
                                        policy -> service.center(
                                                TeleportCommandResults.current(policy, players),
                                                StringArgumentType.getString(command, "world"),
                                                Optional.of(new RandomTeleportCommandService.Coordinates(
                                                        DoubleArgumentType.getDouble(command, "x"),
                                                        DoubleArgumentType.getDouble(command, "z")
                                                ))
                                        )
                                ))
                        )
                );

        var minimum = Commands.literal("minrange")
                .executes(command -> TeleportCommandResults.async(
                        context,
                        command,
                        descriptor,
                        "settpr min query",
                        _ -> service.minimum(
                                StringArgumentType.getString(command, "world"),
                                Optional.empty()
                        )
                ))
                .then(Commands.argument("radius", IntegerArgumentType.integer(0))
                        .executes(command -> TeleportCommandResults.async(
                                context,
                                command,
                                descriptor,
                                "settpr min set",
                                _ -> service.minimum(
                                        StringArgumentType.getString(command, "world"),
                                        Optional.of(IntegerArgumentType.getInteger(command, "radius"))
                                )
                        ))
                );

        var maximum = Commands.literal("maxrange")
                .executes(command -> TeleportCommandResults.async(
                        context,
                        command,
                        descriptor,
                        "settpr max query",
                        _ -> service.maximum(
                                StringArgumentType.getString(command, "world"),
                                Optional.empty()
                        )
                ))
                .then(Commands.argument("radius", IntegerArgumentType.integer(1))
                        .executes(command -> TeleportCommandResults.async(
                                context,
                                command,
                                descriptor,
                                "settpr max set",
                                _ -> service.maximum(
                                        StringArgumentType.getString(command, "world"),
                                        Optional.of(IntegerArgumentType.getInteger(command, "radius"))
                                )
                        ))
                );

        var root = Commands.literal("settpr")
                .then(Commands.argument("world", StringArgumentType.word())
                        .suggests((_, builder) ->
                                CommandSuggestionSupport.suggest(worlds::loadedWorldIds, builder)
                        )
                        .then(center)
                        .then(minimum)
                        .then(maximum)
                );

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.settpr",
                "/settpr <world> <center|minrange|maxrange> [value]",
                root
        );
    }

    @Override
    public String moduleId() {
        return TeleportCommandResults.MODULE;
    }

}
