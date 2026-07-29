package top.likoslupus.cellulosesz.modules.kit.application;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import top.likoslupus.cellulosesz.api.command.execution.ServerThreadExecutor;
import top.likoslupus.cellulosesz.api.item.*;
import top.likoslupus.cellulosesz.api.kit.KitClaimResult;
import top.likoslupus.cellulosesz.api.kit.KitDefinition;
import top.likoslupus.cellulosesz.api.kit.KitItem;
import top.likoslupus.cellulosesz.api.kit.KitService;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformOperationStatus;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.api.player.PlayerResolver;
import top.likoslupus.cellulosesz.api.player.ResolvedPlayer;
import top.likoslupus.cellulosesz.api.player.ResolvedPlayerState;
import top.likoslupus.cellulosesz.api.text.LocalizedMessage;
import top.likoslupus.cellulosesz.core.i18n.GeneratedMessageKeys;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

final class DefaultKitCommandServiceTest {

    private static final UUID SELF_ID = UUID.fromString("00000000-0000-0000-0000-000000000030");
    private static final UUID OTHER_ID = UUID.fromString("00000000-0000-0000-0000-000000000031");
    private static final CellPlayer SELF = new CellPlayer(
            SELF_ID,
            "Tester",
            new Object()
    );

    @Test
    void listClaimAndSuggestionsFilterKitPermission() {
        var kits = new FakeKits();
        kits.values.add(kit(
                "starter",
                "",
                0
        ));
        kits.values.add(kit(
                "vip",
                "cellulosesz.kit.vip",
                0
        ));

        var service = service(kits, new FakeInventory(), resolver(true));
        var listed = service.list(_ -> false);

        assertTrue(listed.success());
        assertEquals("starter", listed.message().placeholders().get("kits"));
        assertEquals(List.of("starter"), service.claimableNames(_ -> false));
        assertFalse(service.claim(SELF, "vip", _ -> false).join().success());
        assertTrue(service.claim(SELF, "vip", _ -> true).join().success());
        assertEquals("vip", kits.lastClaimed);
    }

    private static KitDefinition kit(
            String id,
            String permission,
            long cooldown
    ) {
        var kit = new KitDefinition();
        kit.id = id;
        kit.displayName = id;
        kit.permission = permission;
        kit.cooldownSeconds = cooldown;
        return kit;
    }

    private static DefaultKitCommandService service(
            FakeKits kits,
            FakeInventory inventory,
            PlayerResolver resolver
    ) {
        return new DefaultKitCommandService(kits, inventory, resolver, new ImmediateExecutor());
    }

    @NullMarked
    private static PlayerResolver resolver(boolean known) {
        return new PlayerResolver() {
            @Override
            public ResolvedPlayer resolveKnown(String input, @Nullable CellPlayer viewer) {
                return resolveValue(input, known);
            }

            @Override
            public ResolvedPlayer resolveKnown(UUID uuid, @Nullable CellPlayer viewer) {
                return new ResolvedPlayer(
                        ResolvedPlayerState.ONLINE,
                        SELF_ID,
                        "Tester",
                        SELF,
                        false
                );
            }

            @Override
            public CompletableFuture<ResolvedPlayer> resolve(String input, @Nullable CellPlayer viewer) {
                return CompletableFuture.completedFuture(resolveValue(input, known));
            }

            private ResolvedPlayer resolveValue(String input, boolean found) {
                if (!found) {
                    return new ResolvedPlayer(
                            ResolvedPlayerState.UNKNOWN,
                            null,
                            input,
                            null,
                            false
                    );
                }
                if (input.equalsIgnoreCase("Other")) {
                    return new ResolvedPlayer(
                            ResolvedPlayerState.OFFLINE,
                            OTHER_ID,
                            "Other",
                            null,
                            false
                    );
                }
                return new ResolvedPlayer(
                        ResolvedPlayerState.ONLINE,
                        SELF_ID,
                        "Tester",
                        SELF,
                        false
                );
            }
        };
    }

    @Test
    void showUsesStableFullSnapshotDescriptionsAndRejectsInvalidItem() {
        var kits = new FakeKits();
        var definition = kit("tools", "", 0);
        definition.items = List.of(
                new KitItem(9, "stack-nine"),
                new KitItem(1, "stack-one")
        );
        kits.values.add(definition);

        var inventory = new FakeInventory();
        var service = service(kits, inventory, resolver(true));

        var shown = service.show("tools");
        assertTrue(shown.success());

        var entries = String.valueOf(shown.message().placeholders().get("entries"));
        assertTrue(entries.indexOf("[1]") < entries.indexOf("[9]"));

        inventory.invalidDescription = true;
        var invalid = service.show("tools");

        assertFalse(invalid.success());
        assertEquals(
                GeneratedMessageKeys.COMMANDS_KIT_SHOW_KIT_COMMAND_ERROR_INVALID_ITEM,
                invalid.message().key()
        );
    }

    @Test
    void createSupportsSecondsAndOnceAndDoesNotPublishFailedSave() {
        var kits = new FakeKits();
        var inventory = new FakeInventory();

        inventory.slots = List.of(slot(0, "stone"));
        var service = service(kits, inventory, resolver(true));

        assertTrue(service.create(
                SELF,
                "daily",
                new KitCooldown.Seconds(60)
        ).join().success());
        assertEquals(60L, kits.lastSaved.cooldownSeconds);
        assertTrue(service.create(
                SELF,
                "once",
                new KitCooldown.Once()
        ).join().success());
        assertEquals(-1L, kits.lastSaved.cooldownSeconds);

        kits.failSave = true;
        assertFalse(service.create(
                SELF,
                "broken",
                new KitCooldown.Seconds(0)
        ).join().success());
        assertTrue(
                kits.values.stream()
                        .noneMatch(kit -> kit.id.equals("broken"))
        );
    }

    private static InventorySlotView slot(int slot, String stack) {
        return new InventorySlotView(
                new InventoryItemSnapshot(slot, stack),
                new ItemDescriptor("minecraft:stone", 1),
                InventorySlotKind.MAIN,
                true
        );
    }

    @Test
    void emptyOrInvalidInventoryFailsBeforeSave() {
        var kits = new FakeKits();
        var inventory = new FakeInventory();
        var service = service(kits, inventory, resolver(true));

        assertEquals(
                GeneratedMessageKeys.COMMANDS_KIT_CREATE_KIT_COMMAND_ERROR_EMPTY,
                service.create(
                        SELF,
                        "empty",
                        new KitCooldown.Seconds(0)
                ).join().message().key()
        );

        inventory.snapshotFailure = true;
        assertEquals(
                GeneratedMessageKeys.COMMANDS_KIT_CREATE_KIT_COMMAND_ERROR_SNAPSHOT,
                service.create(
                        SELF,
                        "bad",
                        new KitCooldown.Seconds(0)
                ).join().message().key()
        );
        assertNull(kits.lastSaved);
    }

    @Test
    void deleteSeparatesMissingAndPersistenceFailure() {
        var kits = new FakeKits();
        kits.values.add(kit("starter", "", 0));
        var service = service(kits, new FakeInventory(), resolver(true));

        assertTrue(service.delete("starter").join().success());
        assertFalse(service.delete("missing").join().success());

        kits.failDelete = true;
        assertEquals(
                GeneratedMessageKeys.SERVICE_KIT_PERSISTENCE_FAILED,
                service.delete("broken").join().message().key()
        );
    }

    @Test
    void resetSelfConsoleOthersOfflineAndStorageFailuresAreDistinct() {
        var kits = new FakeKits();
        kits.values.add(kit("starter", "", 0));

        var service = service(kits, new FakeInventory(), resolver(true));
        var self = service.reset(new KitCommandService.ResetRequest(
                Optional.of(SELF),
                "starter",
                Optional.empty(),
                false
        )).join();

        assertTrue(self.success());
        assertEquals(SELF_ID, kits.lastReset);

        var consoleMissing = service.reset(new KitCommandService.ResetRequest(
                Optional.empty(),
                "starter",
                Optional.empty(),
                false
        )).join();
        assertEquals(
                GeneratedMessageKeys.COMMANDS_KIT_KIT_RESET_COMMAND_ERROR_PLAYER_REQUIRED,
                consoleMissing.message().key()
        );

        var denied = service.reset(new KitCommandService.ResetRequest(
                Optional.of(SELF),
                "starter",
                Optional.of("Other"),
                false
        )).join();
        assertEquals(
                GeneratedMessageKeys.COMMANDS_KIT_KIT_RESET_COMMAND_ERROR_OTHERS,
                denied.message().key()
        );

        var others = service.reset(new KitCommandService.ResetRequest(
                Optional.of(SELF),
                "starter",
                Optional.of("Other"),
                true
        )).join();
        assertTrue(others.success());
        assertEquals(OTHER_ID, kits.lastReset);
        assertEquals("Other", others.message().placeholders().get("player"));

        var unknownService = service(
                kits,
                new FakeInventory(),
                resolver(false)
        );
        assertEquals(
                GeneratedMessageKeys.COMMANDS_KIT_KIT_RESET_COMMAND_ERROR_PLAYER_NOT_FOUND,
                unknownService.reset(new KitCommandService.ResetRequest(
                        Optional.empty(),
                        "starter",
                        Optional.of("Ghost"),
                        true
                )).join().message().key()
        );

        kits.failReset = true;
        assertEquals(
                GeneratedMessageKeys.SERVICE_KIT_PERSISTENCE_FAILED,
                service.reset(new KitCommandService.ResetRequest(
                        Optional.of(SELF),
                        "starter",
                        Optional.empty(),
                        false
                )).join().message().key()
        );
    }

    @NullMarked
    private static final class FakeKits implements KitService {

        private final List<KitDefinition> values = new ArrayList<>();
        private @Nullable String lastClaimed;
        private @Nullable KitDefinition lastSaved;
        private @Nullable UUID lastReset;
        private boolean failSave;
        private boolean failDelete;
        private boolean failReset;

        @Override
        public CompletableFuture<Void> reload() {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public List<KitDefinition> kits() {
            return List.copyOf(values);
        }

        @Override
        public Optional<KitDefinition> kit(String id) {
            return values.stream()
                    .filter(kit -> kit.id.equalsIgnoreCase(id))
                    .findFirst();
        }

        @Override
        public CompletableFuture<Void> save(KitDefinition kit) {
            if (failSave) {
                return CompletableFuture.failedFuture(new IllegalStateException("save"));
            }

            lastSaved = kit;
            values.removeIf(existing -> existing.id.equalsIgnoreCase(kit.id));
            values.add(kit);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Boolean> delete(String id) {
            if (failDelete) {
                return CompletableFuture.failedFuture(new IllegalStateException("delete"));
            }
            return CompletableFuture.completedFuture(
                    values.removeIf(kit -> kit.id.equalsIgnoreCase(id))
            );
        }

        @Override
        public CompletableFuture<KitClaimResult> claim(CellPlayer player, KitDefinition kit) {
            lastClaimed = kit.id;
            return CompletableFuture.completedFuture(
                    KitClaimResult.success(LocalizedMessage.of("kit.claimed"))
            );
        }

        @Override
        public CompletableFuture<Void> resetCooldown(UUID uuid, String kitId) {
            if (failReset) {
                return CompletableFuture.failedFuture(new IllegalStateException("reset"));
            }
            lastReset = uuid;
            return CompletableFuture.completedFuture(null);
        }

    }

    @NullMarked
    private static final class FakeInventory implements InventoryPlatformService {

        private List<InventorySlotView> slots = List.of();
        private boolean snapshotFailure;
        private boolean invalidDescription;

        @Override
        public PlatformResult<List<InventorySlotView>> inventorySlots(CellPlayer player) {
            return snapshotFailure
                    ? PlatformResult.failure(PlatformOperationStatus.INTERNAL_ERROR, "snapshot")
                    : PlatformResult.success(slots);
        }

        @Override
        public PlatformResult<ItemDescriptor> describeSnapshot(InventoryItemSnapshot snapshot) {
            return invalidDescription
                    ? PlatformResult.failure(PlatformOperationStatus.INVALID_ARGUMENT, "invalid")
                    : PlatformResult.success(new ItemDescriptor("minecraft:stone", 1));
        }

        @Override
        public PlatformResult<HeldStackChange> setHeldCount(
                CellPlayer player,
                int targetCount,
                int permittedMaximum
        ) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PlatformResult<HatResult> hat(
                CellPlayer player,
                HatAction action,
                boolean ignoreBindingCurse
        ) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PlatformResult<ItemStackDetails> heldItemDetails(CellPlayer player) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PlatformResult<BookDetails> heldBook(CellPlayer player) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PlatformResult<BookMutationResult> mutateBook(CellPlayer player, BookRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CompletableFuture<PlatformResult<SkullResult>> skull(SkullRequest request) {
            throw new UnsupportedOperationException();
        }

    }

    @NullMarked
    private static final class ImmediateExecutor implements ServerThreadExecutor {

        @Override
        public boolean isServerThread() {
            return true;
        }

        @Override
        public void execute(Runnable task) {
            task.run();
        }

        @Override
        public <T> CompletableFuture<T> submit(Supplier<T> task) {
            try {
                return CompletableFuture.completedFuture(task.get());
            } catch (Throwable failure) {
                return CompletableFuture.failedFuture(failure);
            }
        }

    }

}
