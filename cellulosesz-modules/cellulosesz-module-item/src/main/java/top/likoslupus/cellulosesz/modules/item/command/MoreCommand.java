package top.likoslupus.cellulosesz.modules.item.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.item.InventoryPlatformService;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformOperationStatus;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.modules.item.ItemRuntimeSettings;
import top.likoslupus.cellulosesz.modules.item.application.InventoryCommandService;

import java.util.List;

import static java.util.Objects.requireNonNull;

public final class MoreCommand implements CommandContributor {

    private final InventoryCommandService service;
    private final InventoryPlatformService inventory;
    private final ItemRuntimeSettings config;

    public MoreCommand(
            InventoryCommandService service,
            InventoryPlatformService inventory,
            ItemRuntimeSettings config
    ) {
        this.service = requireNonNull(service, "service");
        this.inventory = requireNonNull(inventory, "inventory");
        this.config = requireNonNull(config, "config");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = ItemCommandSupport.descriptor(
                "more",
                "cellulosesz.command.more",
                CommandSourceKind.PLAYER_ONLY
        );

        var root = Commands.literal("more")
                .executes(command -> executeDefault(
                        context,
                        command,
                        descriptor
                ))
                .then(Commands.argument(
                                        "amount",
                                        IntegerArgumentType.integer(
                                                1,
                                                config.maximumOversizedStack()
                                        )
                                )
                                .executes(command -> execute(
                                        context,
                                        command,
                                        descriptor,
                                        IntegerArgumentType.getInteger(
                                                command,
                                                "amount"
                                        )
                                ))
                );

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.more",
                "/more [amount]",
                root
        );
    }

    private int executeDefault(
            CommandRegistrationContext context,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor
    ) {
        return ItemCommandSupport.sync(
                context,
                command,
                descriptor,
                "more default",
                policy -> {
                    var player = ItemCommandSupport.current(policy);

                    if (player.isEmpty()) {
                        return PlatformResult.failure(
                                PlatformOperationStatus.INVALID_SOURCE,
                                "player-only"
                        );
                    }

                    var currentPlayer = player.orElseThrow();
                    var held = inventory.heldItemDetails(currentPlayer);

                    if (!held.successful() || held.value().isEmpty()) {
                        return held;
                    }

                    var details = held.value().orElseThrow();

                    return service.more(
                            currentPlayer,
                            details.maximumCount(),
                            details.maximumCount()
                    );
                }
        );
    }

    private int execute(
            CommandRegistrationContext context,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor,
            int amount
    ) {
        return ItemCommandSupport.sync(
                context,
                command,
                descriptor,
                "more amount=" + amount,
                policy -> {
                    var player = ItemCommandSupport.current(policy);

                    if (player.isEmpty()) {
                        return PlatformResult.failure(
                                PlatformOperationStatus.INVALID_SOURCE,
                                "player-only"
                        );
                    }

                    var maximum = policy.hasPermission("cellulosesz.command.more.oversized")
                            && config.allowOversizedStacks()
                            ? config.maximumOversizedStack()
                            : 64;

                    return service.more(
                            player.orElseThrow(),
                            amount,
                            maximum
                    );
                }
        );
    }

    @Override
    public String moduleId() {
        return ItemCommandSupport.MODULE;
    }

}
