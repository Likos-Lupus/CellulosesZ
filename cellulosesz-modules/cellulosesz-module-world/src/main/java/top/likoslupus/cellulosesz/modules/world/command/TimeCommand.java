package top.likoslupus.cellulosesz.modules.world.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.player.PlayerLocationPlatformService;
import top.likoslupus.cellulosesz.api.world.WorldDirectory;
import top.likoslupus.cellulosesz.api.world.WorldResult;
import top.likoslupus.cellulosesz.api.world.WorldService;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.common.command.CommandSuggestionSupport;
import top.likoslupus.cellulosesz.modules.world.command.argument.TimeValues;

import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public final class TimeCommand implements CommandContributor {

    private final WorldService service;
    private final WorldDirectory worlds;
    private final PlayerLocationPlatformService locations;

    public TimeCommand(
            WorldService service,
            WorldDirectory worlds,
            PlayerLocationPlatformService locations
    ) {
        this.service = requireNonNull(service, "service");
        this.worlds = requireNonNull(worlds, "worlds");
        this.locations = requireNonNull(locations, "locations");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = WorldCommandSupport.descriptor(
                "time",
                "cellulosesz.world.time",
                CommandSourceKind.ANY
        );
        var time = Commands.argument("time", StringArgumentType.word())
                .suggests((_, builder) -> CommandSuggestionSupport.suggest(
                        TimeValues.suggestions(),
                        builder
                ))
                .executes(command -> execute(
                        context,
                        command,
                        descriptor,
                        Optional.empty()
                ))
                .then(Commands.argument("world", DimensionArgument.dimension())
                        .executes(command -> execute(
                                context,
                                command,
                                descriptor,
                                Optional.of(
                                        DimensionArgument.getDimension(command, "world")
                                                .dimension()
                                                .identifier()
                                                .toString()
                                )
                        )));

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.time",
                "/time <day|noon|night|midnight|ticks> [world]",
                Commands.literal("time").then(time)
        );
    }

    private int execute(
            CommandRegistrationContext registration,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor,
            Optional<String> explicitWorld
    ) throws CommandSyntaxException {
        var time = TimeValues.parse(StringArgumentType.getString(command, "time"));
        return WorldCommandSupport.admin(
                registration,
                command,
                descriptor,
                "time",
                policy -> WorldCommandSupport.world(
                                policy,
                                worlds,
                                locations,
                                explicitWorld
                        )
                        .map(world ->
                                service.setTime(world, time)
                        )
                        .orElseGet(() -> WorldResult.failure(
                                "service.world.world-required"
                        ))
        );
    }

    @Override
    public String moduleId() {
        return WorldCommandSupport.MODULE;
    }

}
