package top.likoslupus.cellulosesz.modules.world.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.core.registries.Registries;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.entity.EntityPlatformService;
import top.likoslupus.cellulosesz.api.entity.SpawnMobRequest;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformOperationStatus;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.common.player.MinecraftPlayers;
import top.likoslupus.cellulosesz.modules.world.config.WorldRuntimeSettings;

import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public final class SpawnMobCommand implements CommandContributor {

    private final EntityPlatformService entities;
    private final WorldRuntimeSettings config;

    public SpawnMobCommand(
            EntityPlatformService entities,
            WorldRuntimeSettings config
    ) {
        this.entities = requireNonNull(entities, "entities");
        this.config = requireNonNull(config, "config");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = WorldCommandSupport.descriptor(
                "spawnmob",
                "cellulosesz.command.spawnmob",
                CommandSourceKind.ANY
        );

        var amount = Commands.argument(
                        "amount",
                        IntegerArgumentType.integer(1, config.spawnMobMaximumAmount())
                )
                .executes(command -> execute(
                        context,
                        command,
                        descriptor,
                        IntegerArgumentType.getInteger(command, "amount"),
                        Optional.empty()
                ))
                .then(Commands.argument("player", EntityArgument.player())
                        .requires(source -> context.hasPermission(
                                source,
                                "cellulosesz.command.spawnmob.others"
                        ))
                        .executes(command -> {
                            var targetPlayer = MinecraftPlayers.wrap(
                                    EntityArgument.getPlayer(command, "player")
                            );

                            return execute(
                                    context,
                                    command,
                                    descriptor,
                                    IntegerArgumentType.getInteger(command, "amount"),
                                    Optional.of(targetPlayer)
                            );
                        })
                );

        var entity = Commands.argument(
                        "entity",
                        ResourceArgument.resource(context.buildContext(), Registries.ENTITY_TYPE)
                )
                .executes(command -> execute(
                        context,
                        command,
                        descriptor,
                        1,
                        Optional.empty()
                ))
                .then(amount);

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.spawnmob",
                "/spawnmob <entity> [amount] [player]",
                Commands.literal("spawnmob").then(entity)
        );
    }

    private int execute(
            CommandRegistrationContext registration,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor,
            int amount,
            Optional<CellPlayer> target
    ) {
        return WorldCommandSupport.sync(
                registration,
                command,
                descriptor,
                "spawnmob",
                policy -> {
                    var entity = ResourceArgument.getEntityType(command, "entity")
                            .key()
                            .identifier()
                            .toString();
                    var permission = "cellulosesz.command.spawnmob.entity."
                            + entity.replace(':', '.')
                            .replaceAll("[^a-z0-9_.-]", "_");

                    if (!policy.hasPermission("cellulosesz.command.spawnmob.entity.*")
                            && !policy.hasPermission(permission)
                    ) {
                        return PlatformResult.failure(
                                PlatformOperationStatus.PERMISSION_DENIED,
                                permission
                        );
                    }

                    Optional<CellPlayer> anchor = target.isPresent()
                            ? target
                            : policy.currentPlayer();
                    return anchor
                            .<PlatformResult<?>>map(player -> entities.spawnMob(
                                    new SpawnMobRequest(entity, amount, player)
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
