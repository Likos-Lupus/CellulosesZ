package top.likoslupus.cellulosesz.modules.world.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import top.likoslupus.cellulosesz.api.admin.AdminResult;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.player.PlayerLocationPlatformService;
import top.likoslupus.cellulosesz.api.world.WeatherType;
import top.likoslupus.cellulosesz.api.world.WorldDirectory;
import top.likoslupus.cellulosesz.api.world.WorldService;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.modules.world.command.argument.WeatherTypeArgument;
import top.likoslupus.cellulosesz.modules.world.config.WorldRuntimeSettings;

import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public final class WeatherCommand implements CommandContributor {

    private final WorldService service;
    private final WorldDirectory worlds;
    private final PlayerLocationPlatformService locations;
    private final WorldRuntimeSettings config;

    public WeatherCommand(
            WorldService service,
            WorldDirectory worlds,
            PlayerLocationPlatformService locations,
            WorldRuntimeSettings config
    ) {
        this.service = requireNonNull(service, "service");
        this.worlds = requireNonNull(worlds, "worlds");
        this.locations = requireNonNull(locations, "locations");
        this.config = requireNonNull(config, "config");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = WorldCommandSupport.descriptor(
                "weather",
                "cellulosesz.world.weather",
                CommandSourceKind.ANY
        );
        var type = Commands.argument("type", WeatherTypeArgument.weatherType())
                .executes(command -> execute(
                        context,
                        command,
                        descriptor,
                        config.defaultWeatherSeconds(),
                        Optional.empty()
                ))
                .then(Commands.argument("seconds", IntegerArgumentType.integer(1, 1_000_000))
                        .executes(command -> execute(
                                context,
                                command,
                                descriptor,
                                IntegerArgumentType.getInteger(command, "seconds"),
                                Optional.empty()
                        ))
                        .then(Commands.argument("world", DimensionArgument.dimension())
                                .executes(command -> execute(
                                        context,
                                        command,
                                        descriptor,
                                        IntegerArgumentType.getInteger(command, "seconds"),
                                        Optional.of(
                                                DimensionArgument.getDimension(command, "world")
                                                        .dimension()
                                                        .identifier()
                                                        .toString()
                                        )
                                ))
                        )
                );

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.weather",
                "/weather <clear|rain|thunder> [seconds] [world]",
                Commands.literal("weather").then(type)
        );
    }

    private int execute(
            CommandRegistrationContext registration,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor,
            int seconds,
            Optional<String> explicitWorld
    ) {
        WeatherType type = WeatherTypeArgument.get(command, "type");
        return WorldCommandSupport.admin(
                registration,
                command,
                descriptor,
                "weather",
                policy -> WorldCommandSupport.world(
                                policy,
                                worlds,
                                locations,
                                explicitWorld
                        )
                        .map(world -> service.setWeather(world, type, seconds))
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
