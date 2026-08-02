package top.likoslupus.cellulosesz.modules.world.command;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformOperationStatus;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.api.player.PlayerLocationPlatformService;
import top.likoslupus.cellulosesz.api.world.LightningRequest;
import top.likoslupus.cellulosesz.api.world.PlayerTargetingService;
import top.likoslupus.cellulosesz.api.world.WorldPlatformService;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.common.player.MinecraftPlayers;
import top.likoslupus.cellulosesz.modules.world.config.WorldRuntimeSettings;

import java.util.List;

import static java.util.Objects.requireNonNull;

public final class LightningCommand implements CommandContributor {

    private final WorldPlatformService worlds;
    private final PlayerTargetingService targeting;
    private final PlayerLocationPlatformService locations;
    private final WorldRuntimeSettings config;

    public LightningCommand(
            WorldPlatformService worlds,
            PlayerTargetingService targeting,
            PlayerLocationPlatformService locations,
            WorldRuntimeSettings config
    ) {
        this.worlds = requireNonNull(worlds, "worlds");
        this.targeting = requireNonNull(targeting, "targeting");
        this.locations = requireNonNull(locations, "locations");
        this.config = requireNonNull(config, "config");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = WorldCommandSupport.descriptor(
                "lightning",
                "cellulosesz.command.lightning",
                CommandSourceKind.ANY
        );
        var target = Commands.argument("player", EntityArgument.player())
                .requires(source -> context.permissions().has(
                        source,
                        "cellulosesz.command.lightning.others"
                ))
                .executes(command -> target(
                        context,
                        command,
                        descriptor,
                        0.0D
                ))
                .then(Commands.argument(
                                        "damage",
                                        DoubleArgumentType.doubleArg(0.0D, config.lightningMaximumDamage())
                                )
                                .executes(command -> target(
                                        context,
                                        command,
                                        descriptor,
                                        DoubleArgumentType.getDouble(command, "damage")
                                ))
                );
        var root = Commands.literal("lightning")
                .executes(command -> sight(context, command, descriptor))
                .then(target);

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.lightning",
                "/lightning [player] [damage]",
                root
        );
    }

    private int sight(
            CommandRegistrationContext registration,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor
    ) {
        return WorldCommandSupport.sync(
                registration,
                command,
                descriptor,
                "lightning sight",
                policy -> {
                    var player = WorldCommandSupport.current(policy);
                    if (player.isEmpty()) {
                        return PlatformResult.failure(
                                PlatformOperationStatus.INVALID_SOURCE,
                                "player-only"
                        );
                    }

                    var target = targeting.targetLocation(
                            player.orElseThrow(),
                            config.targetDistance()
                    );
                    return target.successful() && target.value().isPresent()
                            ?
                            worlds.strikeLightning(new LightningRequest(
                                    target.value().orElseThrow(),
                                    false,
                                    0.0D
                            ))
                            : target;
                }
        );
    }

    private int target(
            CommandRegistrationContext registration,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor,
            double damage
    ) throws CommandSyntaxException {
        var player = MinecraftPlayers.wrap(
                EntityArgument.getPlayer(command, "player")
        );

        return WorldCommandSupport.sync(
                registration,
                command,
                descriptor,
                "lightning player",
                _ -> worlds.strikeLightning(new LightningRequest(
                        locations.currentLocation(player),
                        false,
                        damage
                ))
        );
    }

    @Override
    public String moduleId() {
        return WorldCommandSupport.MODULE;
    }

}
