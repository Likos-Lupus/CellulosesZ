package top.likoslupus.cellulosesz.modules.world.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.entity.EntityPlatformService;
import top.likoslupus.cellulosesz.api.entity.SpawnMobRequest;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformOperationStatus;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.common.command.CommandSuggestionSupport;
import top.likoslupus.cellulosesz.common.command.argument.PlayerNameArgument;
import top.likoslupus.cellulosesz.modules.world.command.argument.EntityTypeArgument;
import top.likoslupus.cellulosesz.modules.world.config.WorldRuntimeSettings;

import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public final class SpawnMobCommand implements CommandContributor {

    private final EntityPlatformService entities;
    private final PlayerDirectory players;
    private final WorldRuntimeSettings config;

    public SpawnMobCommand(
            EntityPlatformService entities,
            PlayerDirectory players,
            WorldRuntimeSettings config
    ) {
        this.entities = requireNonNull(entities, "entities");
        this.players = requireNonNull(players, "players");
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
                .then(Commands.argument("player", PlayerNameArgument.playerName())
                        .requires(source -> context.permissions().has(
                                source,
                                "cellulosesz.command.spawnmob.others"
                        ))
                        .suggests((_, builder) -> CommandSuggestionSupport.suggest(
                                players::onlinePlayerNames,
                                builder
                        ))
                        .executes(command -> execute(
                                context,
                                command,
                                descriptor,
                                IntegerArgumentType.getInteger(command, "amount"),
                                Optional.of(PlayerNameArgument.get(command, "player"))
                        ))
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
            Optional<String> targetName
    ) {
        return WorldCommandSupport.sync(
                registration,
                command,
                descriptor,
                "spawnmob",
                policy -> {
                    var entity = EntityTypeArgument.get(command, "entity");
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

                    Optional<CellPlayer> anchor = targetName.isPresent()
                            ? players.onlinePlayer(targetName.orElseThrow())
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
