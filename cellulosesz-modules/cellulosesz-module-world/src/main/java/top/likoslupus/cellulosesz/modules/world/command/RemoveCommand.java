package top.likoslupus.cellulosesz.modules.world.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformOperationStatus;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.api.world.EntityRemovalPlatformService;
import top.likoslupus.cellulosesz.api.world.EntityRemovalRequest;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.modules.world.command.argument.EntityRemoveSelectorArgument;
import top.likoslupus.cellulosesz.modules.world.config.WorldConfig;

import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public final class RemoveCommand implements CommandContributor {

    private final EntityRemovalPlatformService service;
    private final WorldConfig config;

    public RemoveCommand(
            EntityRemovalPlatformService service,
            WorldConfig config
    ) {
        this.service = requireNonNull(service, "service");
        this.config = requireNonNull(config, "config");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = WorldCommandSupport.descriptor(
                "remove",
                "cellulosesz.world.remove",
                CommandSourceKind.PLAYER_ONLY
        );
        var selector = Commands.argument("selector", EntityRemoveSelectorArgument.selector())
                .executes(command -> execute(
                        context,
                        command,
                        descriptor,
                        config.defaultRemoveRadius
                ))
                .then(Commands.argument("radius", IntegerArgumentType.integer(1, 4_096))
                        .executes(command -> execute(
                                context,
                                command,
                                descriptor,
                                IntegerArgumentType.getInteger(command, "radius")
                        ))
                );

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.remove",
                "/remove <selector> [radius]",
                Commands.literal("remove").then(selector)
        );
    }

    private int execute(
            CommandRegistrationContext registration,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor,
            int radius
    ) {
        return WorldCommandSupport.sync(
                registration,
                command,
                descriptor,
                "remove",
                policy -> {
                    var origin = policy.currentPlayer();
                    if (origin.isEmpty()) {
                        return PlatformResult.failure(
                                PlatformOperationStatus.INVALID_SOURCE,
                                "console-position-required"
                        );
                    }

                    return service.remove(new EntityRemovalRequest(
                            EntityRemoveSelectorArgument.get(command, "selector"),
                            Optional.of(origin.orElseThrow()),
                            radius
                    ));
                }
        );
    }

    @Override
    public String moduleId() {
        return WorldCommandSupport.MODULE;
    }

}
