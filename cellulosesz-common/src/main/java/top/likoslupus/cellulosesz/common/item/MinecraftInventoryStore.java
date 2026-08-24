package top.likoslupus.cellulosesz.common.item;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.JsonOps;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import top.likoslupus.cellulosesz.api.item.InventoryMutation;
import top.likoslupus.cellulosesz.api.item.ItemDescriptor;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformOperationStatus;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.common.lifecycle.MinecraftServerHandle;
import top.likoslupus.cellulosesz.common.player.MinecraftPlayers;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.Objects.requireNonNull;

/**
 * Exact-slot inventory snapshots and transactions.
 */
final class MinecraftInventoryStore {

    private final MinecraftServerHandle server;

    MinecraftInventoryStore(MinecraftServerHandle server) {
        this.server = requireNonNull(server, "server");
    }

    PlatformResult<List<InventoryItemSnapshot>> snapshot(CellPlayer player) {
        var inventory = MinecraftPlayers.requireOnline(server, player).getInventory();
        var snapshots = new ArrayList<InventoryItemSnapshot>();

        for (var slot = 0; slot < inventory.getContainerSize(); slot++) {
            var stack = inventory.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }

            var encoded = encode(stack);
            if (!encoded.successful()) {
                return PlatformResult.failure(encoded.status(), encoded.detail());
            }

            snapshots.add(new InventoryItemSnapshot(slot, encoded.value()));
        }

        return PlatformResult.success(List.copyOf(snapshots));
    }

    PlatformResult<String> encode(ItemStack stack) {
        requireNonNull(stack, "stack");
        if (stack.isEmpty()) {
            return PlatformResult.failure(
                    PlatformOperationStatus.INVALID_ARGUMENT,
                    "Cannot encode an empty item stack"
            );
        }

        var operations = server.requireRunning()
                .registryAccess()
                .createSerializationContext(JsonOps.INSTANCE);
        var encoded = ItemStack.CODEC.encodeStart(operations, stack).result();

        if (encoded.isEmpty()) {
            return PlatformResult.failure(
                    PlatformOperationStatus.INTERNAL_ERROR,
                    "Item stack codec did not produce a serialized value"
            );
        }

        return PlatformResult.success(encoded.orElseThrow().toString());
    }

    PlatformResult<InventoryItemSnapshot> heldSnapshot(CellPlayer player) {
        var inventory = MinecraftPlayers.requireOnline(server, player).getInventory();
        var slot = inventory.getSelectedSlot();
        var stack = inventory.getItem(slot);

        if (stack.isEmpty()) {
            return PlatformResult.failure(
                    PlatformOperationStatus.STATE_NOT_ALLOWED,
                    "Main hand is empty"
            );
        }

        var encoded = encode(stack);
        if (!encoded.successful()) {
            return PlatformResult.failure(encoded.status(), encoded.detail());
        }

        return PlatformResult.success(new InventoryItemSnapshot(
                slot,
                encoded.value()
        ));
    }

    PlatformResult<ItemDescriptor> describe(InventoryItemSnapshot snapshot) {
        requireNonNull(snapshot, "snapshot");
        var decoded = decode(snapshot.stack());
        if (!decoded.successful()) {
            return PlatformResult.failure(decoded.status(), decoded.detail());
        }

        var stack = decoded.value();
        if (stack.isEmpty()) {
            return PlatformResult.failure(
                    PlatformOperationStatus.INVALID_ARGUMENT,
                    "Inventory snapshot contains an empty item stack"
            );
        }

        return PlatformResult.success(new ItemDescriptor(
                MinecraftItems.id(stack),
                stack.getCount()
        ));
    }

    PlatformResult<ItemStack> decode(String encoded) {
        requireNonNull(encoded, "encoded");
        if (encoded.isBlank()) {
            return PlatformResult.failure(
                    PlatformOperationStatus.INVALID_ARGUMENT,
                    "Serialized item stack is blank"
            );
        }

        final JsonElement json;
        try {
            json = JsonParser.parseString(encoded);
        } catch (JsonParseException exception) {
            return PlatformResult.failure(
                    PlatformOperationStatus.INVALID_ARGUMENT,
                    "Serialized item stack is not valid JSON: " + exception.getMessage()
            );
        }

        var operations = server.requireRunning()
                .registryAccess()
                .createSerializationContext(JsonOps.INSTANCE);
        var decoded = ItemStack.CODEC.parse(operations, json).result();
        if (decoded.isEmpty()) {
            return PlatformResult.failure(
                    PlatformOperationStatus.INVALID_ARGUMENT,
                    "Serialized item stack is not valid for the current registry"
            );
        }

        return PlatformResult.success(decoded.orElseThrow());
    }

    PlatformResult<Boolean> plain(InventoryItemSnapshot snapshot) {
        requireNonNull(snapshot, "snapshot");
        var decoded = decode(snapshot.stack());
        if (!decoded.successful()) {
            return PlatformResult.failure(decoded.status(), decoded.detail());
        }

        var stack = decoded.value();
        if (stack.isEmpty()) {
            return PlatformResult.failure(
                    PlatformOperationStatus.INVALID_ARGUMENT,
                    "Inventory snapshot contains an empty item stack"
            );
        }

        return PlatformResult.success(ItemStack.isSameItemSameComponents(
                stack,
                new ItemStack(stack.getItem(), stack.getCount())
        ));
    }

    PlatformResult<InventoryMutation> prepareGrant(
            CellPlayer player,
            List<InventoryItemSnapshot> snapshots
    ) {
        requireNonNull(snapshots, "snapshots");
        if (snapshots.isEmpty()) {
            return PlatformResult.failure(
                    PlatformOperationStatus.INVALID_ARGUMENT,
                    "Inventory grant requires at least one item snapshot"
            );
        }

        var inventory = MinecraftPlayers.requireOnline(server, player).getInventory();
        var planned = new LinkedHashMap<Integer, ItemStack>();
        var before = new LinkedHashMap<Integer, ItemStack>();

        for (var snapshot : snapshots) {
            requireNonNull(snapshot, "snapshot");
            if (snapshot.slot() < 0 || snapshot.slot() >= inventory.getContainerSize()) {
                return PlatformResult.failure(
                        PlatformOperationStatus.INVALID_ARGUMENT,
                        "Inventory snapshot slot is outside the target inventory"
                );
            }

            if (planned.containsKey(snapshot.slot())) {
                return PlatformResult.failure(
                        PlatformOperationStatus.INVALID_ARGUMENT,
                        "Inventory grant contains a duplicate slot"
                );
            }

            var decoded = decode(snapshot.stack());

            if (!decoded.successful()) {
                return PlatformResult.failure(decoded.status(), decoded.detail());
            }

            var stack = decoded.value();

            if (stack.isEmpty()) {
                return PlatformResult.failure(
                        PlatformOperationStatus.INVALID_ARGUMENT,
                        "Inventory grant contains an empty item stack"
                );
            }

            var current = inventory.getItem(snapshot.slot());
            if (!current.isEmpty()) {
                return PlatformResult.failure(
                        PlatformOperationStatus.CONFLICT,
                        "Inventory grant destination is no longer empty"
                );
            }

            planned.put(snapshot.slot(), stack.copy());
            before.put(snapshot.slot(), current.copy());
        }

        return PlatformResult.success(new InventoryMutation() {
            private boolean committed;

            @Override
            public PlatformResult<Void> commit() {
                synchronized (inventory) {
                    if (committed) {
                        return PlatformResult.failure(
                                PlatformOperationStatus.INVALID_STATE,
                                "Inventory mutation was already committed"
                        );
                    }

                    for (var entry : before.entrySet()) {
                        if (!same(inventory.getItem(entry.getKey()), entry.getValue())) {
                            return PlatformResult.failure(
                                    PlatformOperationStatus.CONFLICT,
                                    "Inventory changed before commit"
                            );
                        }
                    }

                    planned.forEach((slot, stack) -> inventory.setItem(slot, stack.copy()));
                    inventory.setChanged();
                    committed = true;
                    return PlatformResult.success();
                }
            }

            @Override
            public PlatformResult<Void> rollback() {
                synchronized (inventory) {
                    if (!committed) {
                        return PlatformResult.failure(
                                PlatformOperationStatus.INVALID_STATE,
                                "Inventory mutation has not been committed"
                        );
                    }

                    for (var entry : planned.entrySet()) {
                        if (!same(inventory.getItem(entry.getKey()), entry.getValue())) {
                            return PlatformResult.failure(
                                    PlatformOperationStatus.ROLLBACK_FAILED,
                                    "Inventory changed after commit"
                            );
                        }
                    }

                    before.forEach((slot, stack) -> inventory.setItem(slot, stack.copy()));
                    inventory.setChanged();
                    committed = false;
                    return PlatformResult.success();
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

    PlatformResult<InventoryMutation> prepareRemoval(
            CellPlayer player,
            List<InventoryStackSelection> selections
    ) {
        requireNonNull(selections, "selections");
        if (selections.isEmpty()) {
            return PlatformResult.failure(
                    PlatformOperationStatus.INVALID_ARGUMENT,
                    "Inventory removal requires at least one selection"
            );
        }

        var inventory = MinecraftPlayers.requireOnline(server, player).getInventory();
        var before = copyInventory(inventory);
        var after = copyStacks(before);
        var seen = new HashSet<Integer>();

        for (var selection : selections) {
            requireNonNull(selection, "selection");
            var slot = selection.snapshot().slot();
            if (slot < 0 || slot >= after.size()) {
                return PlatformResult.failure(
                        PlatformOperationStatus.INVALID_ARGUMENT,
                        "Inventory selection slot is outside the target inventory"
                );
            }

            if (!seen.add(slot)) {
                return PlatformResult.failure(
                        PlatformOperationStatus.INVALID_ARGUMENT,
                        "Inventory removal contains a duplicate slot"
                );
            }

            var expected = decode(selection.snapshot().stack());
            if (!expected.successful()) {
                return PlatformResult.failure(expected.status(), expected.detail());
            }

            if (!same(before.get(slot), expected.value())) {
                return PlatformResult.failure(
                        PlatformOperationStatus.CONFLICT,
                        "Inventory changed before removal could be prepared"
                );
            }

            var stack = after.get(slot);
            if (selection.count() > stack.getCount()) {
                return PlatformResult.failure(
                        PlatformOperationStatus.CONFLICT,
                        "Inventory stack no longer contains the requested count"
                );
            }

            if (selection.count() == stack.getCount()) {
                after.set(slot, ItemStack.EMPTY);
            } else {
                stack.shrink(selection.count());
            }
        }

        return PlatformResult.success(mutation(inventory, before, after, Set.copyOf(seen)));
    }

    private static List<ItemStack> copyInventory(Container inventory) {
        return IntStream.range(0, inventory.getContainerSize())
                .mapToObj(slot -> inventory.getItem(slot).copy())
                .collect(Collectors.toCollection(() ->
                        new ArrayList<>(inventory.getContainerSize())
                ));
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
            public PlatformResult<Void> commit() {
                synchronized (inventory) {
                    if (committed) {
                        return PlatformResult.failure(
                                PlatformOperationStatus.INVALID_STATE,
                                "Inventory mutation was already committed"
                        );
                    }

                    if (!matchesAffectedSlots(inventory, before, affectedSlots)) {
                        return PlatformResult.failure(
                                PlatformOperationStatus.CONFLICT,
                                "Inventory changed before commit"
                        );
                    }

                    replace(inventory, after, affectedSlots);
                    committed = true;
                    return PlatformResult.success();
                }
            }

            @Override
            public PlatformResult<Void> rollback() {
                synchronized (inventory) {
                    if (!committed) {
                        return PlatformResult.failure(
                                PlatformOperationStatus.INVALID_STATE,
                                "Inventory mutation has not been committed"
                        );
                    }

                    if (!matchesAffectedSlots(inventory, after, affectedSlots)) {
                        return PlatformResult.failure(
                                PlatformOperationStatus.ROLLBACK_FAILED,
                                "Inventory changed after commit"
                        );
                    }

                    replace(inventory, before, affectedSlots);
                    committed = false;
                    return PlatformResult.success();
                }
            }
        };
    }

    private boolean matchesAffectedSlots(
            Container inventory,
            List<ItemStack> expected,
            Set<Integer> affectedSlots
    ) {
        return inventory.getContainerSize() == expected.size() &&
                affectedSlots.stream()
                        .noneMatch(slot -> slot < 0
                                || slot >= expected.size()
                                || !same(inventory.getItem(slot), expected.get(slot))
                        );
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

    PlatformResult<InventoryMutation> prepareExchange(
            CellPlayer player,
            List<InventoryItemRequest> removals,
            List<InventoryItemRequest> additions
    ) {
        requireNonNull(removals, "removals");
        requireNonNull(additions, "additions");

        if (removals.isEmpty() && additions.isEmpty()) {
            return PlatformResult.failure(
                    PlatformOperationStatus.INVALID_ARGUMENT,
                    "Inventory exchange requires at least one removal or addition"
            );
        }

        var inventory = MinecraftPlayers.requireOnline(server, player).getInventory();
        var before = copyInventory(inventory);
        var after = copyStacks(before);

        for (var request : removals) {
            var parsed = parseItem(request.itemArgument());
            if (parsed.isEmpty()) {
                return PlatformResult.failure(
                        PlatformOperationStatus.INVALID_INPUT,
                        "Inventory removal item argument is invalid: " + request.itemArgument()
                );
            }

            if (!removeMatching(after, parsed.orElseThrow(), request.count())) {
                return PlatformResult.failure(
                        PlatformOperationStatus.CONFLICT,
                        "Inventory does not contain the requested removal"
                );
            }
        }

        for (var request : additions) {
            var parsed = parseItem(request.itemArgument());
            if (parsed.isEmpty()) {
                return PlatformResult.failure(
                        PlatformOperationStatus.INVALID_INPUT,
                        "Inventory addition item argument is invalid: " + request.itemArgument()
                );
            }

            if (!addMatching(after, parsed.orElseThrow(), request.count())) {
                return PlatformResult.failure(
                        PlatformOperationStatus.CONFLICT,
                        "Inventory does not have space for the requested addition"
                );
            }
        }

        var affected = IntStream.range(0, before.size())
                .filter(slot -> !same(before.get(slot), after.get(slot)))
                .boxed()
                .collect(Collectors.toSet());
        if (affected.isEmpty()) {
            return PlatformResult.failure(
                    PlatformOperationStatus.INVALID_STATE,
                    "Inventory exchange would not change the inventory"
            );
        }

        return PlatformResult.success(mutation(inventory, before, after, affected));
    }

    Optional<ItemStack> parseItem(String argument) {
        if (argument.isBlank()) {
            return Optional.empty();
        }

        try {
            var reader = new StringReader(argument.trim());
            var parsed = new ItemParser(server.requireRunning().registryAccess()).parse(reader);
            reader.skipWhitespace();
            if (reader.canRead()) {
                return Optional.empty();
            }

            return Optional.of(parsed.createItemStack(1));
        } catch (CommandSyntaxException | IllegalArgumentException exception) {
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

    Optional<ItemDescriptor> parseDescriptor(String input) {
        if (input.isBlank()) {
            return Optional.empty();
        }

        try {
            var value = input.trim();
            var reader = new StringReader(value);
            var parsed = new ItemParser(server.requireRunning().registryAccess()).parse(reader);
            var itemEnd = reader.getCursor();
            reader.skipWhitespace();
            var count = reader.canRead()
                    ? reader.readInt()
                    : 1;
            reader.skipWhitespace();
            if (reader.canRead() || count <= 0) {
                return Optional.empty();
            }

            var stack = parsed.createItemStack(1);
            return Optional.of(new ItemDescriptor(
                    MinecraftItems.id(stack),
                    count,
                    value.substring(0, itemEnd).trim()
            ));
        } catch (CommandSyntaxException | IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

}
