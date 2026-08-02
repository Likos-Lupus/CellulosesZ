package top.likoslupus.cellulosesz.modules.kit.service;

import org.junit.jupiter.api.Test;
import top.likoslupus.cellulosesz.api.command.execution.ServerThreadExecutor;
import top.likoslupus.cellulosesz.api.item.InventoryPlatformService;
import top.likoslupus.cellulosesz.api.kit.KitDefinition;
import top.likoslupus.cellulosesz.api.kit.KitItem;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.storage.StorageService;
import top.likoslupus.cellulosesz.api.user.CellUser;
import top.likoslupus.cellulosesz.api.user.UserService;
import top.likoslupus.cellulosesz.api.user.UserUpdate;
import top.likoslupus.cellulosesz.modules.kit.KitConfig;
import top.likoslupus.cellulosesz.modules.kit.persistence.KitDocument;
import top.likoslupus.cellulosesz.modules.kit.persistence.KitMapper;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import static org.junit.jupiter.api.Assertions.*;

final class DefaultKitServiceTest {

    @Test
    void savePublishesOnlyAfterPersistenceAndKeepsImmutableDefinition() {
        var storage = new DelayedStorage();
        var config = new KitConfig();

        config.createStarterKitWhenEmpty = false;
        var service = new DefaultKitService(
                storage,
                new NoopUsers(),
                noopInventory(),
                immediateServerThread(),
                Optional.empty(),
                config,
                Path.of("kits")
        );
        var definition = kit("daily", "Daily");

        var save = service.save(definition);
        assertTrue(service.kit("daily").isEmpty(), "unpersisted kit must not be visible");

        storage.completeSave();
        save.join();

        var published = service.kit("daily").orElseThrow();
        assertSame(definition, published);
        assertEquals("Daily", published.displayName());
        assertThrows(
                UnsupportedOperationException.class, () -> published.items().add(
                        new KitItem(1, "{id:\"minecraft:dirt\",count:1}")
                )
        );
    }

    private static InventoryPlatformService noopInventory() {
        return (InventoryPlatformService) Proxy.newProxyInstance(
                InventoryPlatformService.class.getClassLoader(),
                new Class<?>[]{InventoryPlatformService.class},
                (instance, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        return switch (method.getName()) {
                            case "toString" -> "NoopInventory";
                            case "hashCode" -> System.identityHashCode(instance);
                            case "equals" -> instance == args[0];
                            default -> throw new UnsupportedOperationException(method.getName());
                        };
                    }
                    throw new UnsupportedOperationException(method.getName());
                }
        );
    }

    private static ServerThreadExecutor immediateServerThread() {
        return new ServerThreadExecutor() {
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
                } catch (RuntimeException failure) {
                    return CompletableFuture.failedFuture(failure);
                }
            }
        };
    }

    private static KitDefinition kit(String id, String displayName) {
        return new KitDefinition(
                id,
                displayName,
                Optional.of("cellulosesz.kit." + id),
                Duration.ZERO,
                BigDecimal.ZERO,
                List.of(new KitItem(0, "{id:\"minecraft:stone\",count:1}"))
        );
    }

    @Test
    void failedSaveDoesNotPublishKit() {
        var storage = new DelayedStorage();
        var config = new KitConfig();
        config.createStarterKitWhenEmpty = false;
        var service = new DefaultKitService(
                storage,
                new NoopUsers(),
                noopInventory(),
                immediateServerThread(),
                Optional.empty(),
                config,
                Path.of("kits")
        );

        var save = service.save(kit("daily", "Daily"));
        storage.failSave();

        assertThrows(Exception.class, save::join);
        assertTrue(service.kit("daily").isEmpty());
    }

    @Test
    void stagedReloadPublishesOnlyOnCommitAndRollbackRestoresPreviousSnapshot() {
        var storage = new ReloadStorage();
        storage.loaded = List.of(KitMapper.fromDomain(kit("old", "Old")));

        var service = service(storage);
        service.initialize().join();

        storage.loaded = List.of(KitMapper.fromDomain(kit("next", "Next")));
        var prepared = service.prepareReload(
                false,
                true
        ).join();

        assertEquals(List.of("old"), service.kits().stream().map(KitDefinition::id).toList());
        prepared.commit().toCompletableFuture().join();
        assertEquals(List.of("next"), service.kits().stream().map(KitDefinition::id).toList());

        prepared.rollback().toCompletableFuture().join();
        assertEquals(List.of("old"), service.kits().stream().map(KitDefinition::id).toList());
    }

    private static DefaultKitService service(StorageService storage) {
        var config = new KitConfig();
        config.createStarterKitWhenEmpty = false;
        return new DefaultKitService(
                storage,
                new NoopUsers(),
                noopInventory(),
                immediateServerThread(),
                Optional.empty(),
                config,
                Path.of("kits")
        );
    }

    @Test
    void stalePreparedReloadCannotOverwriteNewerDefinitionMutation() {
        var storage = new ReloadStorage();
        storage.loaded = List.of(KitMapper.fromDomain(kit("old", "Old")));
        var service = service(storage);
        service.initialize().join();

        storage.loaded = List.of(KitMapper.fromDomain(kit("next", "Next")));
        var prepared = service.prepareReload(
                false,
                false
        ).join();
        service.save(kit("live", "Live")).join();

        assertThrows(Exception.class, () -> prepared.commit().toCompletableFuture().join());
        assertTrue(service.kit("live").isPresent());
        assertTrue(service.kit("next").isEmpty());
    }

    @Test
    void failedPreparationLeavesLiveSnapshotUntouched() {
        var storage = new ReloadStorage();
        storage.loaded = List.of(KitMapper.fromDomain(kit("old", "Old")));
        var service = service(storage);
        service.initialize().join();

        storage.loaded = List.of(
                KitMapper.fromDomain(kit("duplicate", "First")),
                KitMapper.fromDomain(kit("duplicate", "Second"))
        );

        assertThrows(
                Exception.class,
                () -> service.prepareReload(false, false).join()
        );
        assertEquals(List.of("old"), service.kits().stream().map(KitDefinition::id).toList());
    }

    @NullMarked
    private static final class DelayedStorage implements StorageService {

        private @Nullable CompletableFuture<Void> pendingSave;

        @Override
        public <T> CompletableFuture<T> loadOrDefault(
                Path path,
                Class<T> type,
                Supplier<T> defaultSupplier
        ) {
            return CompletableFuture.completedFuture(defaultSupplier.get());
        }

        @Override
        public <T> CompletableFuture<T> createIfMissing(
                Path path,
                Class<T> type,
                Supplier<T> defaultSupplier
        ) {
            return CompletableFuture.completedFuture(defaultSupplier.get());
        }

        @Override
        public <T> CompletableFuture<Void> save(Path path, T value) {
            if (pendingSave != null) {
                throw new IllegalStateException("test save already pending");
            }
            pendingSave = new CompletableFuture<>();
            return pendingSave;
        }

        @Override
        public CompletableFuture<Boolean> exists(Path path) {
            return CompletableFuture.completedFuture(false);
        }

        @Override
        public CompletableFuture<Boolean> delete(Path path) {
            return CompletableFuture.completedFuture(true);
        }

        @Override
        public <T> CompletableFuture<List<T>> loadDirectory(Path directory, Class<T> type) {
            return CompletableFuture.completedFuture(List.of());
        }

        void completeSave() {
            var current = pendingSave;
            pendingSave = null;
            assertNotNull(current);
            current.complete(null);
        }

        void failSave() {
            var current = pendingSave;
            pendingSave = null;
            assertNotNull(current);
            current.completeExceptionally(new IllegalStateException("disk failure"));
        }

    }

    @NullMarked
    private static final class ReloadStorage implements StorageService {

        private final Map<Path, Object> saved = new LinkedHashMap<>();
        private List<KitDocument> loaded = List.of();

        @Override
        public <T> CompletableFuture<T> loadOrDefault(
                Path path,
                Class<T> type,
                Supplier<T> defaultSupplier
        ) {
            return CompletableFuture.completedFuture(defaultSupplier.get());
        }

        @Override
        public <T> CompletableFuture<T> createIfMissing(
                Path path,
                Class<T> type,
                Supplier<T> defaultSupplier
        ) {
            return CompletableFuture.completedFuture(defaultSupplier.get());
        }

        @Override
        public <T> CompletableFuture<Void> save(Path path, T value) {
            saved.put(path, value);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Boolean> exists(Path path) {
            return CompletableFuture.completedFuture(saved.containsKey(path));
        }

        @Override
        public CompletableFuture<Boolean> delete(Path path) {
            return CompletableFuture.completedFuture(saved.remove(path) != null);
        }

        @Override
        public <T> CompletableFuture<List<T>> loadDirectory(Path directory, Class<T> type) {
            var result = new ArrayList<T>();
            loaded.forEach(value -> result.add(type.cast(value)));
            return CompletableFuture.completedFuture(List.copyOf(result));
        }

    }

    @NullMarked
    private static final class NoopUsers implements UserService {

        @Override
        public CompletableFuture<CellUser> load(UUID uuid) {
            return CompletableFuture.failedFuture(new UnsupportedOperationException());
        }

        @Override
        public CompletableFuture<CellUser> loadFromPlayer(CellPlayer player) {
            return CompletableFuture.failedFuture(new UnsupportedOperationException());
        }

        @Override
        public Optional<CellUser> cached(UUID uuid) {
            return Optional.empty();
        }

        @Override
        public Collection<CellUser> cachedUsers() {
            return List.of();
        }

        @Override
        public Optional<UUID> findUuidByName(String name) {
            return Optional.empty();
        }

        @Override
        public Collection<UUID> knownUuids() {
            return List.of();
        }

        @Override
        public <T> CompletableFuture<T> update(
                UUID uuid,
                Function<CellUser, UserUpdate<T>> mutation
        ) {
            return CompletableFuture.failedFuture(new UnsupportedOperationException());
        }


        @Override
        public CompletableFuture<Void> save(UUID uuid) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Void> saveAll() {
            return CompletableFuture.completedFuture(null);
        }

    }

}
