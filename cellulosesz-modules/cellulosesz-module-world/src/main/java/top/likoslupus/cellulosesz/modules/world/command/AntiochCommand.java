package top.likoslupus.cellulosesz.modules.world.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.entity.EntityPlatformService;
import top.likoslupus.cellulosesz.api.entity.TntBurstRequest;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformOperationStatus;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.api.world.PlayerTargetingService;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.modules.world.config.WorldConfig;

import java.util.List;

import static java.util.Objects.requireNonNull;

public final class AntiochCommand implements CommandContributor {

    private final PlayerTargetingService targeting;
    private final EntityPlatformService entities;
    private final WorldConfig config;

    public AntiochCommand(
            PlayerTargetingService targeting,
            EntityPlatformService entities,
            WorldConfig config
    ) {
        this.targeting = requireNonNull(targeting, "targeting");
        this.entities = requireNonNull(entities, "entities");
        this.config = requireNonNull(config, "config");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = WorldCommandSupport.descriptor(
                "antioch",
                "cellulosesz.command.antioch",
                CommandSourceKind.PLAYER_ONLY
        );
        var root = Commands.literal("antioch")
                .executes(command -> execute(
                        context,
                        command,
                        descriptor,
                        ""
                ))
                .then(Commands.argument("message", StringArgumentType.greedyString())
                        .executes(command -> execute(
                                context,
                                command,
                                descriptor,
                                StringArgumentType.getString(command, "message")
                        )));

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.antioch",
                "/antioch [message...]",
                root
        );
    }

    private int execute(
            CommandRegistrationContext registration,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor,
            String message
    ) {
        return WorldCommandSupport.sync(
                registration,
                command,
                descriptor,
                "antioch message-present=" + !message.isBlank(),
                policy -> {
                    if (!config.destructiveCommandsEnabled) {
                        return PlatformResult.failure(
                                PlatformOperationStatus.STATE_NOT_ALLOWED,
                                "destructive-commands-disabled"
                        );
                    }

                    var player = WorldCommandSupport.current(policy);
                    if (player.isEmpty()) {
                        return PlatformResult.failure(
                                PlatformOperationStatus.INVALID_SOURCE,
                                "player-only"
                        );
                    }

                    var target = targeting.targetLocation(
                            player.orElseThrow(),
                            config.targetDistance
                    );

                    return target.successful() && target.value().isPresent()
                            ?
                            entities.spawnTnt(new TntBurstRequest(
                                    target.value().orElseThrow(),
                                    Math.min(1, config.antiochMaximumEntities),
                                    config.antiochFuseTicks,
                                    config.antiochExplosionPower,
                                    config.explosionBlockDamage,
                                    0.0D,
                                    0.0D
                            ))
                            : target;
                }
        );
    }

    @Override
    public String moduleId() {
        return WorldCommandSupport.MODULE;
    }

}
