package top.likoslupus.cellulosesz.modules.world.command;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.entity.EntityPlatformService;
import top.likoslupus.cellulosesz.api.entity.TntBurstRequest;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformOperationStatus;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.api.player.PlayerLocationPlatformService;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.common.player.MinecraftPlayers;
import top.likoslupus.cellulosesz.modules.world.config.WorldRuntimeSettings;

import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public final class NukeCommand implements CommandContributor {

    private final EntityPlatformService entities;
    private final PlayerLocationPlatformService locations;
    private final WorldRuntimeSettings config;

    public NukeCommand(
            EntityPlatformService entities,
            PlayerLocationPlatformService locations,
            WorldRuntimeSettings config
    ) {
        this.entities = requireNonNull(entities, "entities");
        this.locations = requireNonNull(locations, "locations");
        this.config = requireNonNull(config, "config");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = WorldCommandSupport.descriptor(
                "nuke",
                "cellulosesz.command.nuke",
                CommandSourceKind.ANY
        );
        var root = Commands.literal("nuke")
                .executes(command -> execute(
                        context,
                        command,
                        descriptor,
                        Optional.empty()
                ))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(command -> execute(
                                context,
                                command,
                                descriptor,
                                Optional.of(MinecraftPlayers.wrap(
                                        EntityArgument.getPlayer(command, "player")
                                ))
                        ))
                );

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.nuke",
                "/nuke [player]",
                root
        );
    }

    private int execute(
            CommandRegistrationContext registration,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor,
            Optional<CellPlayer> target
    ) {
        return WorldCommandSupport.sync(
                registration,
                command,
                descriptor,
                "nuke",
                policy -> {
                    if (!config.destructiveCommandsEnabled() || !config.nukeEnabled()) {
                        return PlatformResult.failure(
                                PlatformOperationStatus.STATE_NOT_ALLOWED,
                                "nuke-disabled"
                        );
                    }

                    Optional<CellPlayer> anchor = target.isPresent()
                            ? target
                            : policy.currentPlayer();
                    return anchor
                            .<PlatformResult<?>>map(player -> entities.spawnTnt(
                                    new TntBurstRequest(
                                            locations.currentLocation(player),
                                            config.nukeTntPerTarget(),
                                            config.nukeFuseTicks(),
                                            config.nukeExplosionPower(),
                                            config.explosionBlockDamage(),
                                            config.nukeSpread(),
                                            config.nukeHeight()
                                    )
                            ))
                            .orElseGet(() -> PlatformResult.failure(
                                    PlatformOperationStatus.TARGET_NOT_FOUND,
                                    "target-player-required"
                            ));
                }
        );
    }

    @Override
    public String moduleId() {
        return WorldCommandSupport.MODULE;
    }

}
