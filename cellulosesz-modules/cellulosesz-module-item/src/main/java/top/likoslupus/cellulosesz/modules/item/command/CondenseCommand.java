package top.likoslupus.cellulosesz.modules.item.command;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.item.InventoryItemRequest;
import top.likoslupus.cellulosesz.api.item.ItemService;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.recipe.CompressionRule;
import top.likoslupus.cellulosesz.api.recipe.RecipePlatformService;
import top.likoslupus.cellulosesz.modules.item.ItemConfig;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class CondenseCommand implements CellCommand {

    private final PlatformService platform;
    private final ItemService items;
    private final RecipePlatformService recipes;
    private final ItemConfig config;

    public CondenseCommand(
            PlatformService platform,
            ItemService items,
            RecipePlatformService recipes,
            ItemConfig config
    ) {
        this.platform = platform;
        this.items = items;
        this.recipes = recipes;
        this.config = config;
    }

    @Override
    public String permission() {
        return "cellulosesz.command.condense";
    }

    @Override
    public CommandSourceKind sourceKind() {
        return CommandSourceKind.PLAYER_ONLY;
    }

    @Override
    public String usage() {
        return "/condense [item]";
    }

    @Override
    public String name() {
        return "condense";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        if (invocation.args().length > 1) return usage(invocation);
        var filter = Optional.<String>empty();
        if (invocation.args().length == 1) {
            var parsed = items.parse(invocation.args()[0]);
            if (parsed.isEmpty()) {
                invocation.errorKey("commands.item.condense.invalid-item", Map.of("item", invocation.args()[0]));
                return 0;
            }
            filter = Optional.of(parsed.orElseThrow().normalizedItem());
        }
        var rulesResult = recipes.compressionRules(filter, config.maximumCondenseRules);
        if (!rulesResult.successful() || rulesResult.value().isEmpty()) {
            invocation.platformError(rulesResult.status());
            return 0;
        }
        var player = platform.player(invocation).orElseThrow();
        var snapshot = platform.inventorySnapshot(player);
        if (snapshot.isEmpty()) {
            invocation.errorKey("commands.item.condense.platform-failed", Map.of("reason", "inventory-snapshot"));
            return 0;
        }
        var plainCounts = new LinkedHashMap<String, Integer>();
        for (var stack : snapshot.orElseThrow()) {
            if (!platform.plainInventoryItem(stack)) continue;
            var described = platform.describeInventoryItem(stack);
            if (described.isEmpty()) continue;
            plainCounts.merge(described.orElseThrow().normalizedItem(), described.orElseThrow().count, Math::addExact);
        }
        var removals = new ArrayList<InventoryItemRequest>();
        var additions = new ArrayList<InventoryItemRequest>();
        var conversions = new ArrayList<Conversion>();
        var totalBatches = 0;
        for (CompressionRule rule : rulesResult.value().orElseThrow()) {
            var available = plainCounts.getOrDefault(rule.inputItem(), 0);
            var batches = Math.min(available / rule.inputCount(), config.maximumCondenseBatches - totalBatches);
            if (batches <= 0) continue;
            try {
                var removed = Math.multiplyExact(batches, rule.inputCount());
                var added = Math.multiplyExact(batches, rule.outputCount());
                removals.add(new InventoryItemRequest(rule.inputItem(), removed));
                additions.add(new InventoryItemRequest(rule.outputItem(), added));
                conversions.add(new Conversion(rule.inputItem(), removed, rule.outputItem(), added));
                totalBatches = Math.addExact(totalBatches, batches);
                if (totalBatches >= config.maximumCondenseBatches) break;
            } catch (ArithmeticException failure) {
                invocation.errorKey("commands.item.condense.overflow");
                return 0;
            }
        }
        if (conversions.isEmpty()) {
            invocation.errorKey("commands.item.condense.none");
            return 0;
        }
        var mutation = platform.prepareInventoryExchange(player, removals, additions);
        if (mutation.isEmpty() || !mutation.orElseThrow().commit()) {
            invocation.errorKey("commands.item.condense.no-space-or-conflict");
            return 0;
        }
        for (var conversion : conversions) {
            invocation.replyKey("commands.item.condense.conversion", Map.of(
                    "input", conversion.input(), "removed", conversion.removed(),
                    "output", conversion.output(), "added", conversion.added()
            ));
        }
        invocation.replyKey("commands.item.condense.success", Map.of(
                "rules", conversions.size(), "batches", totalBatches
        ));
        return conversions.size();
    }

    private int usage(CommandInvocation invocation) {
        invocation.errorKey("commands.item.condense.usage", Map.of("usage", usage()));
        return 0;
    }

    private record Conversion(
            String input,
            int removed,
            String output,
            int added
    ) {

    }

}
