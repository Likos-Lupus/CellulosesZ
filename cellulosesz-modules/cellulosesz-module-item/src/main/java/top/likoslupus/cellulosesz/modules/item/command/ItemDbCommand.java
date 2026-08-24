package top.likoslupus.cellulosesz.modules.item.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.item.ItemArgument;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.item.ItemDescriptor;
import top.likoslupus.cellulosesz.api.item.ItemService;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformOperationStatus;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.common.command.CommandSuggestionSupport;
import top.likoslupus.cellulosesz.common.item.InventoryPlatformService;
import top.likoslupus.cellulosesz.modules.item.command.argument.ItemDescriptors;

import java.util.List;

import static java.util.Objects.requireNonNull;

public final class ItemDbCommand implements CommandContributor {

    private final InventoryPlatformService inventory;
    private final ItemService items;

    public ItemDbCommand(
            InventoryPlatformService inventory,
            ItemService items
    ) {
        this.inventory = requireNonNull(inventory, "inventory");
        this.items = requireNonNull(items, "items");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        ItemDescriptors.prepare(items);
        var descriptor = ItemCommandSupport.descriptor(
                "itemdb",
                "cellulosesz.command.itemdb",
                CommandSourceKind.ANY
        );

        var root = Commands.literal("itemdb")
                .executes(command -> held(
                        context,
                        command,
                        descriptor
                ))
                .then(Commands.argument("item", ItemArgument.item(context.buildContext()))
                        .executes(command -> lookup(
                                context,
                                command,
                                descriptor,
                                ItemDescriptors.vanilla(command, "item")
                        ))
                )
                .then(Commands.argument("itemAlias", StringArgumentType.word())
                        .suggests((_, builder) -> CommandSuggestionSupport.suggest(
                                items::itemNames,
                                builder
                        ))
                        .executes(command -> lookup(
                                context,
                                command,
                                descriptor,
                                ItemDescriptors.custom(command, "itemAlias", items)
                        ))
                );

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.itemdb",
                "/itemdb [item]",
                root
        );
    }

    private int held(
            CommandRegistrationContext context,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor
    ) {
        return ItemCommandSupport.sync(
                context,
                command,
                descriptor,
                "itemdb held",
                policy -> ItemCommandSupport.current(policy)
                        .<PlatformResult<?>>map(inventory::heldItemDetails)
                        .orElseGet(() -> PlatformResult.failure(
                                PlatformOperationStatus.INVALID_SOURCE,
                                "item-required"
                        ))
        );
    }

    private int lookup(
            CommandRegistrationContext context,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor,
            ItemDescriptor descriptorInput
    ) {
        return ItemCommandSupport.sync(
                context,
                command,
                descriptor,
                "itemdb item",
                _ -> {
                    var parsed = items.parse(descriptorInput.normalizedItem());
                    if (!parsed.successful()) {
                        return PlatformResult.failure(parsed.status(), parsed.detail());
                    }

                    var resolved = parsed.value();
                    return resolved != null
                            ? PlatformResult.success(resolved)
                            : PlatformResult.failure(
                                    PlatformOperationStatus.NOT_FOUND,
                                    "unknown-item"
                            );
                }
        );
    }

    @Override
    public String moduleId() {
        return ItemCommandSupport.MODULE;
    }

}
