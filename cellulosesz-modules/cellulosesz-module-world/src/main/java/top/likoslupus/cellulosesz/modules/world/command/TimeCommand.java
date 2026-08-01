package top.likoslupus.cellulosesz.modules.world.command;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import top.likoslupus.cellulosesz.api.admin.AdminResult;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.player.PlayerLocationPlatformService;
import top.likoslupus.cellulosesz.api.world.WorldDirectory;
import top.likoslupus.cellulosesz.api.world.WorldService;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.common.command.CommandSuggestionSupport;
import top.likoslupus.cellulosesz.modules.world.command.argument.LoadedWorldArgument;
import top.likoslupus.cellulosesz.modules.world.command.argument.TimeValueArgument;

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
        var time = Commands.argument("time", TimeValueArgument.timeValue())
                .executes(command -> execute(
                        context,
                        command,
                        descriptor,
                        Optional.empty()
                ))
                .then(Commands.argument("world", LoadedWorldArgument.loadedWorld(worlds))
                        .suggests((_, builder) -> CommandSuggestionSupport.suggest(
                                worlds::loadedWorldIds,
                                builder
                        ))
                        .executes(command -> execute(
                                context,
                                command,
                                descriptor,
                                Optional.of(LoadedWorldArgument.get(command, "world"))
                        ))
                );

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
    ) {
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
                        .map(world -> service.setTime(
                                world,
                                TimeValueArgument.get(command, "time")
                        ))
                        .orElseGet(() -> AdminResult.failure(
                                "service.world.world-required"
                        ))
        );
    }

    @Override
    public String moduleId() {
        return WorldCommandSupport.MODULE;
    }

}
