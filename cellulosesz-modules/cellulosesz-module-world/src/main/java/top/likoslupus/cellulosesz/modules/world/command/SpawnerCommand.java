package top.likoslupus.cellulosesz.modules.world.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.core.registries.Registries;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformOperationStatus;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.common.entity.EntityPlatformService;
import top.likoslupus.cellulosesz.common.world.SpawnerRequest;
import top.likoslupus.cellulosesz.common.world.WorldPlatformService;
import top.likoslupus.cellulosesz.modules.world.config.WorldRuntimeSettings;

import java.util.List;

import static java.util.Objects.requireNonNull;

public final class SpawnerCommand implements CommandContributor {

    private final WorldPlatformService worlds;
    private final EntityPlatformService entities;
    private final WorldRuntimeSettings config;

    public SpawnerCommand(
            WorldPlatformService worlds,
            EntityPlatformService entities,
            WorldRuntimeSettings config
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
        var entity = Commands.argument(
                        "entity",
                        ResourceArgument.resource(context.buildContext(), Registries.ENTITY_TYPE)
                )
                .executes(command -> execute(
                        context,
                        command,
                        descriptor,
                        config.spawnerDefaultDelayTicks()
                ))
                .then(Commands.argument(
                                        "delayTicks",
                                        IntegerArgumentType.integer(
                                                config.spawnerMinimumDelayTicks(),
                                                config.spawnerMaximumDelayTicks()
                                        )
                                )
                                .requires(source -> context.hasPermission(
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
    ) throws CommandSyntaxException {
        var entity = ResourceArgument.getEntityType(command, "entity")
                .key()
                .identifier()
                .toString();

        return WorldCommandSupport.sync(
                registration,
                command,
                descriptor,
                "spawner",
                policy -> {
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
                                    config.targetDistance(),
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
