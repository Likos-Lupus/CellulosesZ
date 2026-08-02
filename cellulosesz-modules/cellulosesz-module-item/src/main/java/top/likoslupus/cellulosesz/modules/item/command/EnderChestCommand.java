package top.likoslupus.cellulosesz.modules.item.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformOperationStatus;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.common.player.MinecraftPlayers;
import top.likoslupus.cellulosesz.modules.item.application.InventoryCommandService;

import java.util.List;

import static java.util.Objects.requireNonNull;

public final class EnderChestCommand implements CommandContributor {

    private final InventoryCommandService service;

    public EnderChestCommand(InventoryCommandService service) {
        this.service = requireNonNull(service, "service");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = ItemCommandSupport.descriptor(
                "enderchest",
                "cellulosesz.item.enderchest",
                CommandSourceKind.ANY
        );

        var root = Commands.literal("enderchest")
                .executes(command -> executeSelf(
                        context,
                        command,
                        descriptor
                ))
                .then(Commands.argument(
                                        "player",
                                        EntityArgument.player()
                                )
                                .requires(source -> context.hasPermission(
                                        source,
                                        "cellulosesz.item.enderchest.others"
                                ))
                                .executes(command -> executeTarget(
                                        context,
                                        command,
                                        descriptor
                                ))
                );

        var node = context.registerDirect(
                moduleId(),
                descriptor,
                List.of("echest"),
                "commands.description.enderchest",
                "/enderchest [player]",
                root
        );

        context.registerAlias(
                moduleId(),
                descriptor,
                "echest",
                node
        );
    }

    private int executeSelf(
            CommandRegistrationContext context,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor
    ) {
        return ItemCommandSupport.sync(
                context,
                command,
                descriptor,
                "enderchest self",
                policy -> {
                    var player = ItemCommandSupport.current(policy);

                    return player
                            .<PlatformResult<?>>map(value ->
                                    service.openEnderChest(value, value)
                            )
                            .orElseGet(() -> PlatformResult.failure(
                                    PlatformOperationStatus.INVALID_SOURCE,
                                    "target-required"
                            ));
                }
        );
    }

    private int executeTarget(
            CommandRegistrationContext context,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor
    ) throws CommandSyntaxException {
        var target = MinecraftPlayers.wrap(
                EntityArgument.getPlayer(command, "player")
        );

        return ItemCommandSupport.sync(
                context,
                command,
                descriptor,
                "enderchest target",
                policy -> {
                    var viewer = ItemCommandSupport.current(policy).orElse(target);

                    return service.openEnderChest(
                            viewer,
                            target
                    );
                }
        );
    }

    @Override
    public String moduleId() {
        return ItemCommandSupport.MODULE;
    }

}
