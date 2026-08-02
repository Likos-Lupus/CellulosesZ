package top.likoslupus.cellulosesz.common.item;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Filterable;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.component.WritableBookContent;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import top.likoslupus.cellulosesz.api.item.*;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformOperationStatus;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.common.lifecycle.MinecraftServerHandle;
import top.likoslupus.cellulosesz.common.player.MinecraftPlayerUnavailableException;
import top.likoslupus.cellulosesz.common.player.MinecraftPlayers;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static java.util.Objects.requireNonNull;

public final class MinecraftInventoryOperations implements InventoryPlatformService {

    private static final int MAIN_END = 36;
    private static final int ARMOR_END = 40;
    private static final int OFFHAND_SLOT = 40;
    private static final int MAX_BOOK_TITLE = 32;
    private static final int MAX_BOOK_AUTHOR = 16;

    private final MinecraftServerHandle server;
    private final MinecraftInventoryStore store;

    public MinecraftInventoryOperations(MinecraftServerHandle server) {
        this.server = requireNonNull(server, "server");
        this.store = new MinecraftInventoryStore(server);
    }

    @Override
    public PlatformResult<Void> openInventory(CellPlayer viewer, CellPlayer target) {
        return onServerThread(() -> {
            var viewerPlayer = MinecraftPlayers.requireOnline(server, viewer);
            var targetPlayer = MinecraftPlayers.requireOnline(server, target);
            var mirror = new InventoryMirror(targetPlayer.getInventory(), 54);

            viewerPlayer.openMenu(new SimpleMenuProvider(
                    (id, inventory, _) -> ChestMenu.sixRows(
                            id,
                            inventory,
                            mirror
                    ),
                    Component.literal(target.name())
            ));

            return PlatformResult.success();
        });
    }

    @Override
    public PlatformResult<Void> openEnderChest(CellPlayer viewer, CellPlayer target) {
        return onServerThread(() -> {
            var viewerPlayer = MinecraftPlayers.requireOnline(server, viewer);
            var targetPlayer = MinecraftPlayers.requireOnline(server, target);
            viewerPlayer.openMenu(new SimpleMenuProvider(
                    (id, inventory, ignored) -> ChestMenu.threeRows(
                            id,
                            inventory,
                            targetPlayer.getEnderChestInventory()
                    ),
                    Component.literal(target.name())
            ));

            return PlatformResult.success();
        });
    }

    @Override
    public PlatformResult<List<InventoryItemSnapshot>> inventorySnapshot(CellPlayer player) {
        return onServerThread(() -> store.snapshot(player));
    }

    @Override
    public PlatformResult<Boolean> plainSnapshot(InventoryItemSnapshot snapshot) {
        return onServerThread(() -> store.plain(requireNonNull(snapshot, "snapshot")));
    }

    @Override
    public PlatformResult<InventoryMutation> prepareExchange(
            CellPlayer player,
            List<InventoryItemRequest> removals,
            List<InventoryItemRequest> additions
    ) {
        return onServerThread(() -> store.prepareExchange(player, removals, additions));
    }

    @Override
    public PlatformResult<InventoryMutation> prepareGrant(
            CellPlayer player,
            List<InventoryItemSnapshot> snapshots
    ) {
        return onServerThread(() -> store.prepareGrant(player, snapshots));
    }

    @Override
    public PlatformResult<List<InventorySlotView>> inventorySlots(CellPlayer player) {
        return onServerThread(() -> {
            var snapshots = store.snapshot(player);
            if (!snapshots.successful()) {
                return PlatformResult.failure(snapshots.status(), snapshots.detail());
            }

            var views = new ArrayList<InventorySlotView>();
            for (var snapshot : snapshots.value().orElseThrow()) {
                var descriptor = store.describe(snapshot);
                if (!descriptor.successful()) {
                    return PlatformResult.failure(descriptor.status(), descriptor.detail());
                }

                var plain = store.plain(snapshot);
                if (!plain.successful()) {
                    return PlatformResult.failure(plain.status(), plain.detail());
                }

                views.add(new InventorySlotView(
                        snapshot,
                        descriptor.value().orElseThrow(),
                        slotKind(snapshot.slot()),
                        plain.value().orElseThrow()
                ));
            }

            return PlatformResult.success(List.copyOf(views));
        });
    }

    @Override
    public PlatformResult<InventorySlotView> heldSlot(CellPlayer player) {
        return onServerThread(() -> {
            var snapshot = store.heldSnapshot(player);
            if (!snapshot.successful()) {
                return PlatformResult.failure(snapshot.status(), snapshot.detail());
            }

            var value = snapshot.value().orElseThrow();
            var descriptor = store.describe(value);
            if (!descriptor.successful()) {
                return PlatformResult.failure(descriptor.status(), descriptor.detail());
            }

            var plain = store.plain(value);
            if (!plain.successful()) {
                return PlatformResult.failure(plain.status(), plain.detail());
            }

            return PlatformResult.success(new InventorySlotView(
                    value,
                    descriptor.value().orElseThrow(),
                    InventorySlotKind.MAIN,
                    plain.value().orElseThrow()
            ));
        });
    }

    @Override
    public PlatformResult<InventoryMutation> prepareRemoval(
            CellPlayer player,
            List<InventoryStackSelection> selections
    ) {
        return onServerThread(() -> store.prepareRemoval(player, selections));
    }

    @Override
    public PlatformResult<ItemDescriptor> describeSnapshot(InventoryItemSnapshot snapshot) {
        return onServerThread(() -> store.describe(requireNonNull(snapshot, "snapshot")));
    }

    @Override
    public PlatformResult<HeldStackChange> setHeldCount(
            CellPlayer player,
            int targetCount,
            int permittedMaximum
    ) {
        if (targetCount < 1
                || permittedMaximum < 1
                || targetCount > permittedMaximum
        ) {
            return PlatformResult.failure(
                    PlatformOperationStatus.INVALID_ARGUMENT,
                    "Requested stack count is outside the permitted range"
            );
        }

        return onServerThread(() -> {
            var nativePlayer = MinecraftPlayers.requireOnline(server, player);
            var held = nativePlayer.getMainHandItem();
            if (held.isEmpty()) {
                return PlatformResult.failure(
                        PlatformOperationStatus.STATE_NOT_ALLOWED,
                        "Main hand is empty"
                );
            }

            var normalMaximum = held.getMaxStackSize();
            var previous = held.getCount();
            var replacement = held.copy();

            replacement.setCount(targetCount);
            nativePlayer.setItemInHand(InteractionHand.MAIN_HAND, replacement);

            return PlatformResult.success(new HeldStackChange(
                    MinecraftItems.id(replacement),
                    previous,
                    targetCount,
                    normalMaximum
            ));
        });
    }

    @Override
    public PlatformResult<HatResult> hat(
            CellPlayer player,
            HatAction action,
            boolean ignoreBindingCurse
    ) {
        requireNonNull(action, "action");
        return onServerThread(() -> {
            var nativePlayer = MinecraftPlayers.requireOnline(server, player);
            var inventory = nativePlayer.getInventory();
            var helmet = nativePlayer.getItemBySlot(EquipmentSlot.HEAD);

            if (!helmet.isEmpty() && bindingCursed(helmet) && !ignoreBindingCurse) {
                return PlatformResult.failure(
                        PlatformOperationStatus.STATE_NOT_ALLOWED,
                        "Helmet has binding curse"
                );
            }

            if (action == HatAction.REMOVE) {
                if (helmet.isEmpty()) {
                    return PlatformResult.failure(
                            PlatformOperationStatus.STATE_NOT_ALLOWED,
                            "Helmet slot is empty"
                    );
                }

                var destination = firstInsertionSlot(inventory, helmet);
                if (destination < 0) {
                    return PlatformResult.failure(
                            PlatformOperationStatus.STATE_NOT_ALLOWED,
                            "Inventory has no space"
                    );
                }

                var before = MinecraftItems.id(helmet);
                insertExact(inventory, destination, helmet.copy());
                nativePlayer.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
                inventory.setChanged();
                return PlatformResult.success(new HatResult(
                        Optional.of(before),
                        Optional.empty()
                ));
            }

            var held = nativePlayer.getMainHandItem();
            if (held.isEmpty()) {
                return PlatformResult.failure(
                        PlatformOperationStatus.STATE_NOT_ALLOWED,
                        "Main hand is empty"
                );
            }

            var previous = helmet.isEmpty()
                    ? Optional.<String>empty()
                    : Optional.of(MinecraftItems.id(helmet));
            var next = MinecraftItems.id(held);
            var heldCopy = held.copy();
            var helmetCopy = helmet.copy();

            nativePlayer.setItemSlot(EquipmentSlot.HEAD, heldCopy);
            nativePlayer.setItemInHand(InteractionHand.MAIN_HAND, helmetCopy);
            inventory.setChanged();

            return PlatformResult.success(new HatResult(previous, Optional.of(next)));
        });
    }

    @Override
    public PlatformResult<ItemStackDetails> heldItemDetails(CellPlayer player) {
        return onServerThread(() -> {
            var stack = MinecraftPlayers.requireOnline(server, player).getMainHandItem();
            if (stack.isEmpty()) {
                return PlatformResult.failure(
                        PlatformOperationStatus.STATE_NOT_ALLOWED,
                        "Main hand is empty"
                );
            }

            var plain = ItemStack.isSameItemSameComponents(
                    stack,
                    new ItemStack(stack.getItem(), stack.getCount())
            );
            var maximumDamage = stack.getMaxDamage();

            return PlatformResult.success(new ItemStackDetails(
                    MinecraftItems.id(stack),
                    stack.getHoverName().getString(),
                    stack.getCount(),
                    stack.getMaxStackSize(),
                    !plain,
                    stack.isDamageableItem(),
                    maximumDamage == 0
                            ? 0
                            : Math.max(0, maximumDamage - stack.getDamageValue()),
                    maximumDamage
            ));
        });
    }

    @Override
    public PlatformResult<BookDetails> heldBook(CellPlayer player) {
        return onServerThread(() -> details(MinecraftPlayers
                .requireOnline(server, player)
                .getMainHandItem()));
    }

    @Override
    public PlatformResult<BookMutationResult> mutateBook(CellPlayer player, BookRequest request) {
        requireNonNull(request, "request");
        return onServerThread(() -> {
            var nativePlayer = MinecraftPlayers.requireOnline(server, player);
            var original = nativePlayer.getMainHandItem();
            var current = details(original);
            if (!current.successful()) {
                return PlatformResult.failure(current.status(), current.detail());
            }

            if (
                    (
                            request.action() == BookAction.SET_TITLE
                                    && request.value().length() > MAX_BOOK_TITLE
                    ) || (
                            request.action() == BookAction.SET_AUTHOR
                                    && request.value().length() > MAX_BOOK_AUTHOR
                    ) || request.value().codePoints().anyMatch(Character::isISOControl)
            ) {
                return PlatformResult.failure(
                        PlatformOperationStatus.INVALID_ARGUMENT,
                        "Book metadata is invalid"
                );
            }

            var replacement = switch (request.action()) {
                case UNLOCK -> unlock(original);
                case SIGN -> sign(
                        original,
                        request.actingPlayerName(),
                        request.value()
                );
                case SET_TITLE -> rewriteMetadata(
                        original,
                        Optional.of(request.value()),
                        Optional.empty()
                );
                case SET_AUTHOR -> rewriteMetadata(
                        original,
                        Optional.empty(),
                        Optional.of(request.value())
                );
            };

            if (replacement.isEmpty()) {
                return PlatformResult.failure(
                        PlatformOperationStatus.STATE_NOT_ALLOWED,
                        "Book action is not valid for the held book"
                );
            }

            nativePlayer.setItemInHand(InteractionHand.MAIN_HAND, replacement.orElseThrow());
            var after = details(replacement.orElseThrow());
            if (!after.successful() || after.value().isEmpty()) {
                nativePlayer.setItemInHand(InteractionHand.MAIN_HAND, original);
                return PlatformResult.failure(
                        PlatformOperationStatus.ROLLBACK_FAILED,
                        "Book verification failed"
                );
            }

            return PlatformResult.success(new BookMutationResult(
                    request.action(),
                    after.value().orElseThrow()
            ));
        });
    }

    @Override
    public CompletableFuture<PlatformResult<SkullResult>> skull(SkullRequest request) {
        requireNonNull(request, "request");
        var currentServer = server.requireRunning();
        return currentServer.getProfileCache()
                .getAsync(request.owner())
                .thenCompose(profile -> {
                    if (profile.isEmpty()) {
                        return CompletableFuture.completedFuture(PlatformResult.failure(
                                PlatformOperationStatus.TARGET_NOT_FOUND,
                                "Profile was not found"
                        ));
                    }

                    var future = new CompletableFuture<PlatformResult<SkullResult>>();
                    currentServer.execute(() -> {
                        try {
                            future.complete(applySkull(request, profile.orElseThrow()));
                        } catch (RuntimeException failure) {
                            future.completeExceptionally(failure);
                        }
                    });

                    return future;
                })
                .exceptionally(failure -> PlatformResult.failure(
                        PlatformOperationStatus.INTERNAL_ERROR,
                        failure.getClass().getSimpleName()
                ));
    }

    private PlatformResult<SkullResult> applySkull(SkullRequest request, GameProfile profile) {
        var target = MinecraftPlayers.requireOnline(server, request.recipient());
        if (target.hasDisconnected()) {
            return PlatformResult.failure(
                    PlatformOperationStatus.TARGET_NOT_FOUND,
                    "Recipient disconnected"
            );
        }

        var skull = new ItemStack(Items.PLAYER_HEAD);
        skull.set(
                DataComponents.PROFILE,
                new ResolvableProfile(profile)
        );
        if (request.spawn()) {
            if (!target.getInventory().add(skull)) {
                return PlatformResult.failure(
                        PlatformOperationStatus.STATE_NOT_ALLOWED,
                        "Recipient inventory is full"
                );
            }

            return PlatformResult.success(new SkullResult(
                    request.owner(),
                    request.recipient().name(),
                    true
            ));
        }

        var expected = request.expectedHeld().orElseThrow();
        var currentSnapshot = store.heldSnapshot(request.recipient());

        if (!currentSnapshot.successful()) {
            if (currentSnapshot.status() == PlatformOperationStatus.STATE_NOT_ALLOWED) {
                return PlatformResult.failure(
                        PlatformOperationStatus.CONFLICT,
                        "Held item changed while profile resolved"
                );
            }

            return PlatformResult.failure(currentSnapshot.status(), currentSnapshot.detail());
        }

        var currentSnapshotValue = currentSnapshot.value().orElseThrow();
        if (currentSnapshotValue.slot() != expected.slot()
                || !currentSnapshotValue.stack().equals(expected.stack())
        ) {
            return PlatformResult.failure(
                    PlatformOperationStatus.CONFLICT,
                    "Held item changed while profile resolved"
            );
        }

        var current = target.getMainHandItem();

        if (!current.is(Items.PLAYER_HEAD)) {
            return PlatformResult.failure(
                    PlatformOperationStatus.STATE_NOT_ALLOWED,
                    "Held item is not a player head"
            );
        }

        var replacement = current.copy();
        replacement.set(
                DataComponents.PROFILE,
                new ResolvableProfile(profile)
        );
        target.setItemInHand(InteractionHand.MAIN_HAND, replacement);

        return PlatformResult.success(new SkullResult(
                request.owner(),
                request.recipient().name(),
                false
        ));
    }

    private Optional<ItemStack> unlock(ItemStack original) {
        var content = original.get(DataComponents.WRITTEN_BOOK_CONTENT);
        if (!original.is(Items.WRITTEN_BOOK) || content == null) {
            return Optional.empty();
        }

        var replacement = original.transmuteCopy(Items.WRITABLE_BOOK);
        var pages = content.pages().stream()
                .map(page -> Filterable.passThrough(page.raw().getString()))
                .toList();
        replacement.remove(DataComponents.WRITTEN_BOOK_CONTENT);
        replacement.set(DataComponents.WRITABLE_BOOK_CONTENT, new WritableBookContent(pages));

        return Optional.of(replacement);
    }

    private Optional<ItemStack> sign(
            ItemStack original,
            String author,
            String title
    ) {
        var content = original.getOrDefault(
                DataComponents.WRITABLE_BOOK_CONTENT,
                WritableBookContent.EMPTY
        );
        if (!original.is(Items.WRITABLE_BOOK)) {
            return Optional.empty();
        }

        var actualTitle = title.isBlank()
                ? "Book"
                : title;
        if (actualTitle.length() > MAX_BOOK_TITLE || author.length() > MAX_BOOK_AUTHOR) {
            return Optional.empty();
        }

        var pages = content.pages().stream()
                .map(page -> Filterable.passThrough(Component.literal(page.raw())))
                .toList();
        var replacement = original.transmuteCopy(Items.WRITTEN_BOOK);

        replacement.remove(DataComponents.WRITABLE_BOOK_CONTENT);
        replacement.set(
                DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(
                        Filterable.passThrough(actualTitle),
                        author,
                        0,
                        pages,
                        true
                )
        );

        return Optional.of(replacement);
    }

    private Optional<ItemStack> rewriteMetadata(
            ItemStack original,
            Optional<String> title,
            Optional<String> author
    ) {
        var content = original.get(DataComponents.WRITTEN_BOOK_CONTENT);
        if (!original.is(Items.WRITTEN_BOOK) || content == null) {
            return Optional.empty();
        }

        var replacement = original.copy();
        replacement.set(
                DataComponents.WRITTEN_BOOK_CONTENT,
                new WrittenBookContent(
                        Filterable.passThrough(title.orElse(content.title().raw())),
                        author.orElse(content.author()),
                        content.generation(),
                        content.pages(),
                        content.resolved()
                )
        );

        return Optional.of(replacement);
    }

    private PlatformResult<BookDetails> details(ItemStack stack) {
        if (stack.is(Items.WRITABLE_BOOK)) {
            var content = stack.getOrDefault(
                    DataComponents.WRITABLE_BOOK_CONTENT,
                    WritableBookContent.EMPTY
            );
            return PlatformResult.success(new BookDetails(
                    true,
                    false,
                    Optional.empty(),
                    Optional.empty(),
                    content.pages().size()
            ));
        }

        if (stack.is(Items.WRITTEN_BOOK)) {
            var content = stack.get(DataComponents.WRITTEN_BOOK_CONTENT);
            if (content == null) {
                return PlatformResult.failure(
                        PlatformOperationStatus.INTERNAL_ERROR,
                        "Written book content is missing"
                );
            }

            return PlatformResult.success(new BookDetails(
                    false,
                    true,
                    Optional.of(content.title().raw()),
                    Optional.of(content.author()),
                    content.pages().size()
            ));
        }

        return PlatformResult.failure(
                PlatformOperationStatus.STATE_NOT_ALLOWED,
                "Held item is not a book"
        );
    }

    private static boolean bindingCursed(ItemStack stack) {
        var enchantments = stack.getOrDefault(
                DataComponents.ENCHANTMENTS,
                ItemEnchantments.EMPTY
        );
        return enchantments.keySet().stream()
                .anyMatch(holder -> holder.unwrapKey()
                        .map(key -> key.identifier().toString().equals("minecraft:binding_curse"))
                        .orElse(false));
    }

    private static int firstInsertionSlot(
            Inventory inventory,
            ItemStack stack
    ) {
        for (var slot = 0; slot < MAIN_END; slot++) {
            var current = inventory.getItem(slot);
            if (current.isEmpty()) {
                return slot;
            }

            if (ItemStack.isSameItemSameComponents(current, stack)
                    && ((current.getCount() + stack.getCount()) <= current.getMaxStackSize())
            ) {
                return slot;
            }
        }
        return -1;
    }

    private static void insertExact(
            Inventory inventory,
            int slot,
            ItemStack stack
    ) {
        var current = inventory.getItem(slot);
        if (current.isEmpty()) {
            inventory.setItem(slot, stack);
        } else {
            current.grow(stack.getCount());
        }
    }

    private static InventorySlotKind slotKind(int slot) {
        if (slot < MAIN_END) {
            return InventorySlotKind.MAIN;
        }

        if (slot < ARMOR_END) {
            return InventorySlotKind.ARMOR;
        }

        return slot == OFFHAND_SLOT
                ? InventorySlotKind.OFFHAND
                : InventorySlotKind.MAIN;
    }

    private <T> PlatformResult<T> onServerThread(Supplier<PlatformResult<T>> operation) {
        final var active = server.current();
        if (active.isEmpty()) {
            return PlatformResult.failure(
                    PlatformOperationStatus.NOT_READY,
                    "Minecraft server is not active"
            );
        }

        if (!active.orElseThrow().isSameThread()) {
            return PlatformResult.failure(
                    PlatformOperationStatus.WRONG_THREAD,
                    "Operation requires the server thread"
            );
        }

        try {
            return operation.get();
        } catch (MinecraftPlayerUnavailableException failure) {
            return PlatformResult.failure(
                    PlatformOperationStatus.TARGET_NOT_FOUND,
                    failure.getMessage()
            );
        } catch (IllegalArgumentException failure) {
            return PlatformResult.failure(
                    PlatformOperationStatus.INVALID_ARGUMENT,
                    failure.getMessage() == null
                            ? "Invalid inventory operation"
                            : failure.getMessage()
            );
        } catch (RuntimeException failure) {
            return PlatformResult.failure(
                    PlatformOperationStatus.INTERNAL_ERROR,
                    failure.getClass().getSimpleName()
            );
        }
    }

    private static final class InventoryMirror extends SimpleContainer {

        private final Container target;
        private final int mirroredSlots;
        private final boolean loading;

        private InventoryMirror(Container target, int size) {
            super(size);
            this.target = target;
            this.mirroredSlots = Math.min(target.getContainerSize(), size);
            IntStream.range(0, mirroredSlots)
                    .forEach(slot -> super.setItem(slot, target.getItem(slot).copy()));
            loading = false;
        }

        @Override
        public void setChanged() {
            super.setChanged();
            if (!loading) {
                synchronize();
            }
        }

        private void synchronize() {
            IntStream.range(0, mirroredSlots)
                    .forEach(slot -> target.setItem(slot, getItem(slot).copy()));
            target.setChanged();
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return slot >= 0 && slot < mirroredSlots;
        }

    }

}
