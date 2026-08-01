package top.likoslupus.cellulosesz.fabric;

import com.google.gson.JsonParser;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.JsonOps;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import top.likoslupus.cellulosesz.api.item.*;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.Objects.requireNonNull;

/**
 * Exact-slot inventory snapshots and transactions.
 */
final class FabricInventoryStore {

    private final FabricServerAccess access;

    FabricInventoryStore(FabricServerAccess access) {
        this.access = requireNonNull(access, "access");
    }

    Optional<List<InventoryItemSnapshot>> snapshot(CellPlayer player) {
        var inventory = access.player(player).getInventory();
        var snapshots = new ArrayList<InventoryItemSnapshot>();

        for (var slot = 0; slot < inventory.getContainerSize(); slot++) {
            var stack = inventory.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }

            var encoded = encode(stack);
            if (encoded.isEmpty()) {
                return Optional.empty();
            }

            snapshots.add(new InventoryItemSnapshot(slot, encoded.orElseThrow()));
        }

        return Optional.of(List.copyOf(snapshots));
    }

    Optional<String> encode(ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }

        var operations = access.requireServer()
                .registryAccess()
                .createSerializationContext(JsonOps.INSTANCE);

        return ItemStack.CODEC
                .encodeStart(operations, stack)
                .result()
                .map(Object::toString);
    }

    Optional<InventoryItemSnapshot> heldSnapshot(CellPlayer player) {
        var inventory = access.player(player).getInventory();
        var slot = inventory.getSelectedSlot();
        var stack = inventory.getItem(slot);

        if (stack.isEmpty()) {
            return Optional.empty();
        }

        return encode(stack).map(encoded ->
                new InventoryItemSnapshot(slot, encoded)
        );
    }

    Optional<ItemDescriptor> describe(InventoryItemSnapshot snapshot) {
        requireNonNull(snapshot, "snapshot");
        return decode(snapshot.validatedStack())
                .filter(stack -> !stack.isEmpty())
                .map(stack -> new ItemDescriptor(
                        access.itemId(stack),
                        stack.getCount()
                ));
    }

    Optional<ItemStack> decode(String encoded) {
        if (encoded.isBlank()) {
            return Optional.empty();
        }

        try {
            var operations = access.requireServer()
                    .registryAccess()
                    .createSerializationContext(JsonOps.INSTANCE);
            return ItemStack.CODEC.parse(operations, JsonParser.parseString(encoded)).result();
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    boolean plain(InventoryItemSnapshot snapshot) {
        requireNonNull(snapshot, "snapshot");
        return decode(snapshot.validatedStack())
                .filter(stack -> !stack.isEmpty())
                .map(stack -> ItemStack.isSameItemSameComponents(
                        stack,
                        new ItemStack(stack.getItem(), stack.getCount())
                ))
                .orElse(false);
    }

    Optional<InventoryGrant> prepareGrant(
            CellPlayer player,
            List<? extends InventoryItemSnapshot> snapshots
    ) {
        requireNonNull(snapshots, "snapshots");
        if (snapshots.isEmpty()) {
            return Optional.empty();
        }

        var inventory = access.player(player).getInventory();
        var planned = new LinkedHashMap<Integer, ItemStack>();
        var before = new LinkedHashMap<Integer, ItemStack>();

        for (var snapshot : snapshots) {
            requireNonNull(snapshot, "snapshot");
            if (snapshot.slot < 0
                    || snapshot.slot >= inventory.getContainerSize()
                    || planned.containsKey(snapshot.slot)
            ) {
                return Optional.empty();
            }

            var decoded = decode(snapshot.validatedStack());
            if (decoded.isEmpty()
                    || decoded.orElseThrow().isEmpty()
            ) {
                return Optional.empty();
            }

            var current = inventory.getItem(snapshot.slot);
            if (!current.isEmpty()) {
                return Optional.empty();
            }

            planned.put(snapshot.slot, decoded.orElseThrow().copy());
            before.put(snapshot.slot, current.copy());
        }

        return Optional.of(new InventoryGrant() {
            private boolean committed;

            @Override
            public boolean commit() {
                synchronized (inventory) {
                    if (committed) {
                        return false;
                    }

                    for (var entry : before.entrySet()) {
                        if (!same(inventory.getItem(entry.getKey()), entry.getValue())) {
                            return false;
                        }
                    }

                    planned.forEach((slot, stack) -> inventory.setItem(slot, stack.copy()));
                    inventory.setChanged();
                    committed = true;
                    return true;
                }
            }

            @Override
            public boolean rollback() {
                synchronized (inventory) {
                    if (!committed) {
                        return true;
                    }

                    for (var entry : planned.entrySet()) {
                        if (!same(inventory.getItem(entry.getKey()), entry.getValue())) {
                            return false;
                        }
                    }

                    before.forEach((slot, stack) -> inventory.setItem(slot, stack.copy()));
                    inventory.setChanged();
                    committed = false;
                    return true;
                }
            }
        });
    }

    boolean same(ItemStack first, ItemStack second) {
        if (first.isEmpty() || second.isEmpty()) {
            return first.isEmpty() && second.isEmpty();
        }

        return first.getCount() == second.getCount()
                && ItemStack.isSameItemSameComponents(first, second);
    }

    Optional<InventoryMutation> prepareRemoval(
            CellPlayer player,
            List<InventoryStackSelection> selections
    ) {
        requireNonNull(selections, "selections");
        if (selections.isEmpty()) {
            return Optional.empty();
        }

        var inventory = access.player(player).getInventory();
        var before = copyInventory(inventory);
        var after = copyStacks(before);
        var seen = new HashSet<Integer>();

        for (var selection : selections) {
            requireNonNull(selection, "selection");

            var slot = selection.snapshot().slot;
            if (slot < 0
                    || slot >= after.size()
                    || !seen.add(slot)
            ) {
                return Optional.empty();
            }

            var expected = decode(selection.snapshot().validatedStack());
            if (expected.isEmpty()
                    || !same(before.get(slot), expected.orElseThrow())
            ) {
                return Optional.empty();
            }

            var stack = after.get(slot);
            if (selection.count() > stack.getCount()) {
                return Optional.empty();
            }

            if (selection.count() == stack.getCount()) {
                after.set(slot, ItemStack.EMPTY);
            } else {
                stack.shrink(selection.count());
            }
        }

        return Optional.of(mutation(
                inventory,
                before,
                after,
                Set.copyOf(seen)
        ));
    }

    private static List<ItemStack> copyInventory(Container inventory) {
        return IntStream.range(0, inventory.getContainerSize())
                .mapToObj(slot -> inventory.getItem(slot).copy())
                .collect(Collectors.toCollection(() -> new ArrayList<>(inventory.getContainerSize())));
    }

    private static List<ItemStack> copyStacks(List<ItemStack> source) {
        return source.stream()
                .map(ItemStack::copy)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private InventoryMutation mutation(
            Container inventory,
            List<ItemStack> before,
            List<ItemStack> after,
            Set<Integer> affectedSlots
    ) {
        return new InventoryMutation() {
            private boolean committed;

            @Override
            public boolean commit() {
                synchronized (inventory) {
                    if (committed || !matchesAffectedSlots(inventory, before, affectedSlots)) {
                        return false;
                    }

                    replace(inventory, after, affectedSlots);
                    committed = true;
                    return true;
                }
            }

            @Override
            public boolean rollback() {
                synchronized (inventory) {
                    if (!committed) {
                        return true;
                    }

                    if (!matchesAffectedSlots(inventory, after, affectedSlots)) {
                        return false;
                    }

                    replace(inventory, before, affectedSlots);
                    committed = false;
                    return true;
                }
            }
        };
    }

    private boolean matchesAffectedSlots(
            Container inventory,
            List<ItemStack> expected,
            Set<Integer> affectedSlots
    ) {
        if (inventory.getContainerSize() != expected.size()) {
            return false;
        }

        for (var slot : affectedSlots) {
            if (slot < 0
                    || slot >= expected.size()
                    || !same(inventory.getItem(slot), expected.get(slot))
            ) {
                return false;
            }
        }

        return true;
    }

    private static void replace(
            Container inventory,
            List<ItemStack> replacement,
            Set<Integer> affectedSlots
    ) {
        affectedSlots.forEach(slot -> inventory.setItem(
                slot,
                replacement.get(slot).copy()
        ));
        inventory.setChanged();
    }

    Optional<InventoryMutation> prepareExchange(
            CellPlayer player,
            List<InventoryItemRequest> removals,
            List<InventoryItemRequest> additions
    ) {
        requireNonNull(removals, "removals");
        requireNonNull(additions, "additions");

        if (removals.isEmpty() && additions.isEmpty()) {
            return Optional.empty();
        }

        var inventory = access.player(player).getInventory();
        var before = copyInventory(inventory);
        var after = copyStacks(before);

        for (var request : removals) {
            var parsed = parseItem(request.itemArgument());
            if (parsed.isEmpty()) {
                return Optional.empty();
            }

            if (!removeMatching(
                    after,
                    parsed.orElseThrow().createItemStack(1, false),
                    request.count()
            )) {
                return Optional.empty();
            }
        }

        for (var request : additions) {
            var parsed = parseItem(request.itemArgument());
            if (parsed.isEmpty()) {
                return Optional.empty();
            }

            if (!addMatching(
                    after,
                    parsed.orElseThrow().createItemStack(1, false),
                    request.count()
            )) {
                return Optional.empty();
            }
        }

        var affected = IntStream.range(0, before.size())
                .filter(slot -> !same(before.get(slot), after.get(slot)))
                .boxed()
                .collect(java.util.stream.Collectors.toSet());

        if (affected.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(mutation(inventory, before, after, affected));
    }

    Optional<ItemInput> parseItem(String argument) {
        if (argument.isBlank()) {
            return Optional.empty();
        }

        try {
            var reader = new StringReader(argument.trim());
            var parsed = ItemParser.parseForItem(access.requireServer().registryAccess(), reader);
            if (reader.canRead()) {
                return Optional.empty();
            }

            return Optional.of(parsed);
        } catch (CommandSyntaxException exception) {
            return Optional.empty();
        }
    }

    private static boolean removeMatching(
            List<ItemStack> stacks,
            ItemStack template,
            int requested
    ) {
        if (requested <= 0) {
            return false;
        }

        var remaining = requested;
        for (var slot = 0; slot < stacks.size() && remaining > 0; slot++) {
            var stack = stacks.get(slot);
            if (stack.isEmpty()
                    || !ItemStack.isSameItemSameComponents(stack, template)
            ) {
                continue;
            }

            var removed = Math.min(remaining, stack.getCount());
            if (removed == stack.getCount()) {
                stacks.set(slot, ItemStack.EMPTY);
            } else {
                stack.shrink(removed);
            }

            remaining -= removed;
        }

        return remaining == 0;
    }

    private static boolean addMatching(
            List<ItemStack> stacks,
            ItemStack template,
            int requested
    ) {
        if (requested <= 0) {
            return false;
        }

        var remaining = requested;
        for (var stack : stacks) {
            if (remaining == 0) {
                break;
            }

            if (stack.isEmpty() || !ItemStack.isSameItemSameComponents(stack, template)) {
                continue;
            }

            var capacity = Math.max(
                    0,
                    Math.min(stack.getMaxStackSize(), template.getMaxStackSize()) - stack.getCount()
            );

            var inserted = Math.min(capacity, remaining);
            if (inserted > 0) {
                stack.grow(inserted);
                remaining -= inserted;
            }
        }

        for (var slot = 0; slot < stacks.size() && remaining > 0; slot++) {
            if (!stacks.get(slot).isEmpty()) {
                continue;
            }

            var inserted = Math.min(remaining, template.getMaxStackSize());
            stacks.set(slot, template.copyWithCount(inserted));
            remaining -= inserted;
        }

        return remaining == 0;
    }

}
