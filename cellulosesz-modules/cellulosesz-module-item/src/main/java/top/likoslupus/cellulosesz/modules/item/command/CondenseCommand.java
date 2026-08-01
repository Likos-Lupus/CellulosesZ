package top.likoslupus.cellulosesz.modules.item.command;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.item.InventoryItemRequest;
import top.likoslupus.cellulosesz.api.item.InventoryPlatformService;
import top.likoslupus.cellulosesz.api.item.InventorySlotView;
import top.likoslupus.cellulosesz.api.item.ItemService;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformOperationStatus;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.api.recipe.RecipePlatformService;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.common.command.CommandSuggestionSupport;
import top.likoslupus.cellulosesz.modules.item.ItemConfig;
import top.likoslupus.cellulosesz.modules.item.command.argument.ItemIdArgument;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

public final class CondenseCommand implements CommandContributor {

    private final ItemService items;
    private final InventoryPlatformService inventory;
    private final RecipePlatformService recipes;
    private final ItemConfig config;

    public CondenseCommand(
            ItemService items,
            InventoryPlatformService inventory,
            RecipePlatformService recipes,
            ItemConfig config
    ) {
        this.items = requireNonNull(items, "items");
        this.inventory = requireNonNull(inventory, "inventory");
        this.recipes = requireNonNull(recipes, "recipes");
        this.config = requireNonNull(config, "config");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = ItemCommandSupport.descriptor(
                "condense",
                "cellulosesz.command.condense",
                CommandSourceKind.PLAYER_ONLY
        );

        var root = Commands.literal("condense")
                .executes(command -> execute(
                        context,
                        command,
                        descriptor,
                        Optional.empty()
                ))
                .then(Commands.argument(
                                        "item",
                                        ItemIdArgument.itemId(items)
                                )
                                .suggests((_, builder) ->
                                        CommandSuggestionSupport.suggest(
                                                items::names,
                                                builder
                                        )
                                )
                                .executes(command -> execute(
                                        context,
                                        command,
                                        descriptor,
                                        Optional.of(
                                                ItemIdArgument.get(
                                                        command,
                                                        "item"
                                                )
                                        )
                                ))
                );

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.condense",
                "/condense [item]",
                root
        );
    }

    private int execute(
            CommandRegistrationContext context,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor,
            Optional<String> filter
    ) {
        return ItemCommandSupport.sync(
                context,
                command,
                descriptor,
                "condense",
                policy -> {
                    var player = ItemCommandSupport.current(policy);

                    if (player.isEmpty()) {
                        return PlatformResult.failure(
                                PlatformOperationStatus.INVALID_SOURCE,
                                "player-only"
                        );
                    }

                    var currentPlayer = player.orElseThrow();
                    var rules = recipes.compressionRules(
                            filter,
                            config.maximumCondenseRules
                    );
                    var slots = inventory.inventorySlots(currentPlayer);

                    if (!rules.successful() || rules.value().isEmpty()) {
                        return rules;
                    }

                    if (!slots.successful() || slots.value().isEmpty()) {
                        return slots;
                    }

                    var counts = slots.value().orElseThrow().stream()
                            .filter(InventorySlotView::plain)
                            .collect(Collectors.toMap(
                                    slot -> slot.descriptor().normalizedItem(),
                                    slot -> slot.descriptor().count,
                                    Integer::sum,
                                    HashMap::new
                            ));
                    var removals = new ArrayList<InventoryItemRequest>();
                    var additions = new ArrayList<InventoryItemRequest>();
                    var conversions = 0;

                    for (var rule : rules.value().orElseThrow()) {
                        var batches = Math.min(
                                config.maximumCondenseBatches,
                                counts.getOrDefault(
                                        rule.inputItem(),
                                        0
                                ) / rule.inputCount()
                        );

                        if (batches <= 0) {
                            continue;
                        }

                        removals.add(new InventoryItemRequest(
                                rule.inputItem(),
                                Math.multiplyExact(
                                        batches,
                                        rule.inputCount()
                                )
                        ));
                        additions.add(new InventoryItemRequest(
                                rule.outputItem(),
                                Math.multiplyExact(
                                        batches,
                                        rule.outputCount()
                                )
                        ));
                        conversions += batches;
                    }

                    if (conversions == 0) {
                        return PlatformResult.failure(
                                PlatformOperationStatus.NOT_FOUND,
                                "no-compression"
                        );
                    }

                    var mutation = inventory.prepareExchange(
                            currentPlayer,
                            removals,
                            additions
                    );

                    if (!mutation.successful()
                            || mutation.value().isEmpty()) {
                        return mutation;
                    }

                    return mutation.value().orElseThrow().commit()
                            ? PlatformResult.success(conversions)
                            : PlatformResult.failure(
                                    PlatformOperationStatus.CONFLICT,
                                    "inventory-conflict"
                            );
                }
        );
    }

    @Override
    public String moduleId() {
        return ItemCommandSupport.MODULE;
    }

}
