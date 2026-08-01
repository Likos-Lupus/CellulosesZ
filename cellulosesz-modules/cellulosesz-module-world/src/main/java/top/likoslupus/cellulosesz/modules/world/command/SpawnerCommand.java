package top.likoslupus.cellulosesz.modules.world.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.entity.EntityPlatformService;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformOperationStatus;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.api.world.SpawnerRequest;
import top.likoslupus.cellulosesz.api.world.WorldPlatformService;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.common.command.CommandSuggestionSupport;
import top.likoslupus.cellulosesz.modules.world.command.argument.EntityTypeArgument;
import top.likoslupus.cellulosesz.modules.world.config.WorldConfig;

import java.util.List;

import static java.util.Objects.requireNonNull;

public final class SpawnerCommand implements CommandContributor {

    private final WorldPlatformService worlds;
    private final EntityPlatformService entities;
    private final WorldConfig config;

    public SpawnerCommand(
            WorldPlatformService worlds,
            EntityPlatformService entities,
            WorldConfig config
    ) {
        this.worlds = requireNonNull(worlds, "worlds");
        this.entities = requireNonNull(entities, "entities");
        this.config = requireNonNull(config, "config");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = WorldCommandSupport.descriptor(
                "spawner",
                "cellulosesz.command.spawner",
                CommandSourceKind.PLAYER_ONLY
        );
        var entity = Commands.argument("entity", EntityTypeArgument.livingEntity(entities))
                .suggests((_, builder) -> CommandSuggestionSupport.suggest(
                        entities::livingEntityIds,
                        builder
                ))
                .executes(command -> execute(
                        context,
                        command,
                        descriptor,
                        config.spawnerDefaultDelayTicks
                ))
                .then(Commands.argument(
                                        "delayTicks",
                                        IntegerArgumentType.integer(
                                                config.spawnerMinimumDelayTicks,
                                                config.spawnerMaximumDelayTicks
                                        )
                                )
                                .requires(source -> context.permissions().has(
                                        source,
                                        "cellulosesz.command.spawner.delay"
                                ))
                                .executes(command -> execute(
                                        context,
                                        command,
                                        descriptor,
                                        IntegerArgumentType.getInteger(command, "delayTicks")
                                ))
                );

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.spawner",
                "/spawner <entity> [delayTicks]",
                Commands.literal("spawner").then(entity)
        );
    }

    private int execute(
            CommandRegistrationContext registration,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor,
            int delay
    ) {
        return WorldCommandSupport.sync(
                registration,
                command,
                descriptor,
                "spawner",
                policy -> {
                    var entity = EntityTypeArgument.get(command, "entity");
                    var permission = "cellulosesz.command.spawner.entity."
                            + entity.replace(':', '.')
                            .replaceAll("[^a-z0-9_.-]", "_");

                    if (!policy.hasPermission("cellulosesz.command.spawner.entity.*")
                            && !policy.hasPermission(permission)
                    ) {
                        return PlatformResult.failure(
                                PlatformOperationStatus.PERMISSION_DENIED,
                                permission
                        );
                    }

                    var player = WorldCommandSupport.current(policy);
                    return player
                            .<PlatformResult<?>>map(value -> worlds.configureSpawner(
                                    value,
                                    config.targetDistance,
                                    new SpawnerRequest(entity, delay)
                            ))
                            .orElseGet(() -> PlatformResult.failure(
                                    PlatformOperationStatus.INVALID_SOURCE,
                                    "player-only"
                            ));
                }
        );
    }

    @Override
    public String moduleId() {
        return WorldCommandSupport.MODULE;
    }

}
