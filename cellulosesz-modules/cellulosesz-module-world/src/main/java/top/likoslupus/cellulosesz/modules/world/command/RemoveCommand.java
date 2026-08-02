package top.likoslupus.cellulosesz.modules.world.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformOperationStatus;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.api.world.EntityRemovalRequest;
import top.likoslupus.cellulosesz.api.world.EntityRemoveSelector;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.common.world.MinecraftEntityRemovalOperations;
import top.likoslupus.cellulosesz.modules.world.config.WorldRuntimeSettings;

import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public final class RemoveCommand implements CommandContributor {

    private final MinecraftEntityRemovalOperations service;
    private final WorldRuntimeSettings config;

    public RemoveCommand(
            MinecraftEntityRemovalOperations service,
            WorldRuntimeSettings config
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
        var root = Commands.literal("remove");
        addLiteral(root, context, descriptor, "all", EntityRemoveSelector.Kind.ALL);
        addLiteral(root, context, descriptor, "animals", EntityRemoveSelector.Kind.ANIMALS);
        addLiteral(root, context, descriptor, "monsters", EntityRemoveSelector.Kind.MONSTERS);
        addLiteral(root, context, descriptor, "items", EntityRemoveSelector.Kind.ITEMS);
        addLiteral(root, context, descriptor, "projectiles", EntityRemoveSelector.Kind.PROJECTILES);
        addLiteral(root, context, descriptor, "boats", EntityRemoveSelector.Kind.BOATS);
        addLiteral(root, context, descriptor, "minecarts", EntityRemoveSelector.Kind.MINECARTS);

        var entity = Commands.argument(
                        "entity",
                        ResourceArgument.resource(context.buildContext(), Registries.ENTITY_TYPE)
                )
                .executes(command -> executeEntity(
                        context,
                        command,
                        descriptor,
                        ResourceArgument.getEntityType(command, "entity").value(),
                        config.defaultRemoveRadius()
                ))
                .then(Commands.argument("radius", IntegerArgumentType.integer(1, 4_096))
                        .executes(command -> executeEntity(
                                context,
                                command,
                                descriptor,
                                ResourceArgument.getEntityType(command, "entity").value(),
                                IntegerArgumentType.getInteger(command, "radius")
                        ))
                );
        root.then(entity);

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.remove",
                "/remove <all|animals|monsters|items|projectiles|boats|minecarts|entity> [radius]",
                root
        );
    }

    private void addLiteral(
            LiteralArgumentBuilder<CommandSourceStack> root,
            CommandRegistrationContext context,
            CommandDescriptor descriptor,
            String literal,
            EntityRemoveSelector.Kind kind
    ) {
        var selector = EntityRemoveSelector.of(kind);
        root.then(Commands.literal(literal)
                .executes(command -> execute(
                        context,
                        command,
                        descriptor,
                        selector,
                        config.defaultRemoveRadius()
                ))
                .then(Commands.argument("radius", IntegerArgumentType.integer(1, 4_096))
                        .executes(command -> execute(
                                context,
                                command,
                                descriptor,
                                selector,
                                IntegerArgumentType.getInteger(command, "radius")
                        ))
                )
        );
    }

    private int executeEntity(
            CommandRegistrationContext registration,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor,
            EntityType<?> type,
            int radius
    ) {
        return WorldCommandSupport.sync(
                registration,
                command,
                descriptor,
                "remove entity",
                policy -> {
                    var origin = policy.currentPlayer();
                    if (origin.isEmpty()) {
                        return PlatformResult.failure(
                                PlatformOperationStatus.INVALID_SOURCE,
                                "console-position-required"
                        );
                    }

                    return service.remove(
                            type,
                            origin.orElseThrow(),
                            radius
                    );
                }
        );
    }

    private int execute(
            CommandRegistrationContext registration,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor,
            EntityRemoveSelector selector,
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
                            selector,
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
