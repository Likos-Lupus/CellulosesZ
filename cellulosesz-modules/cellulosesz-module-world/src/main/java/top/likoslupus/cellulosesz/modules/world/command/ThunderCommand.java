package top.likoslupus.cellulosesz.modules.world.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformOperationStatus;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.api.player.PlayerLocationPlatformService;
import top.likoslupus.cellulosesz.api.world.ThunderRequest;
import top.likoslupus.cellulosesz.api.world.WorldPlatformService;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.modules.world.config.WorldRuntimeSettings;

import java.util.List;

import static java.util.Objects.requireNonNull;

public final class ThunderCommand implements CommandContributor {

    private final WorldPlatformService worlds;
    private final PlayerLocationPlatformService locations;
    private final WorldRuntimeSettings config;

    public ThunderCommand(
            WorldPlatformService worlds,
            PlayerLocationPlatformService locations,
            WorldRuntimeSettings config
    ) {
        this.worlds = requireNonNull(worlds, "worlds");
        this.locations = requireNonNull(locations, "locations");
        this.config = requireNonNull(config, "config");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = WorldCommandSupport.descriptor(
                "thunder",
                "cellulosesz.command.thunder",
                CommandSourceKind.PLAYER_ONLY
        );
        var enabled = Commands.argument("enabled", BoolArgumentType.bool())
                .executes(command -> execute(
                        context,
                        command,
                        descriptor,
                        config.defaultWeatherSeconds()
                ))
                .then(Commands.argument("seconds", IntegerArgumentType.integer(1, 107_374_182))
                        .executes(command -> execute(
                                context,
                                command,
                                descriptor,
                                IntegerArgumentType.getInteger(command, "seconds")
                        ))
                );

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.thunder",
                "/thunder <true|false> [seconds]",
                Commands.literal("thunder").then(enabled)
        );
    }

    private int execute(
            CommandRegistrationContext registration,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor,
            int seconds
    ) {
        return WorldCommandSupport.sync(
                registration,
                command,
                descriptor,
                "thunder",
                policy -> {
                    var player = WorldCommandSupport.current(policy);
                    if (player.isEmpty()) {
                        return PlatformResult.failure(
                                PlatformOperationStatus.INVALID_SOURCE,
                                "player-only"
                        );
                    }

                    var ticks = Math.multiplyExact(seconds, 20);
                    return worlds.setThunder(
                            locations.currentLocation(player.orElseThrow()).world(),
                            new ThunderRequest(BoolArgumentType.getBool(command, "enabled"), ticks)
                    );
                }
        );
    }

    @Override
    public String moduleId() {
        return WorldCommandSupport.MODULE;
    }

}
