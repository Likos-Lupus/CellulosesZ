package top.likoslupus.cellulosesz.modules.kit.service;

import org.junit.jupiter.api.Test;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.kit.KitDefinition;
import top.likoslupus.cellulosesz.api.kit.KitItem;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.storage.StorageService;
import top.likoslupus.cellulosesz.api.teleport.CellLocation;
import top.likoslupus.cellulosesz.api.user.CellUser;
import top.likoslupus.cellulosesz.api.user.UserService;
import top.likoslupus.cellulosesz.modules.kit.KitConfig;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

final class DefaultKitServiceTest {

    @Test
    void savePublishesOnlyAfterPersistenceAndDoesNotLeakMutableDefinitions() {
        var storage = new DelayedStorage();
        var config = new KitConfig();
        config.createStarterKitWhenEmpty = false;
        var service = new DefaultKitService(
                storage,
                new NoopUsers(),
                new NoopPlatform(),
                Optional.empty(),
                config,
                Path.of("kits")
        );
        var definition = kit("daily", "Daily");

        var save = service.save(definition);
        assertTrue(service.kit("daily").isEmpty(), "unpersisted kit must not be visible");

        definition.displayName = "mutated by caller";
        storage.completeSave();
        save.join();

        var published = service.kit("daily").orElseThrow();
        assertEquals("Daily", published.displayName);
        published.displayName = "mutated returned copy";
        assertEquals("Daily", service.kit("daily").orElseThrow().displayName);
    }

    private static KitDefinition kit(String id, String displayName) {
        var definition = new KitDefinition();
        definition.id = id;
        definition.displayName = displayName;
        definition.permission = "cellulosesz.kit." + id;
        definition.items.add(new KitItem(0, "{id:\"minecraft:stone\",count:1}"));
        return definition;
    }

    @Test
    void failedSaveDoesNotPublishKit() {
        var storage = new DelayedStorage();
        var config = new KitConfig();
        config.createStarterKitWhenEmpty = false;
        var service = new DefaultKitService(
                storage,
                new NoopUsers(),
                new NoopPlatform(),
                Optional.empty(),
                config,
                Path.of("kits")
        );

        var save = service.save(kit("daily", "Daily"));
        storage.failSave();

        assertThrows(Exception.class, save::join);
        assertTrue(service.kit("daily").isEmpty());
    }

    private static final class DelayedStorage implements StorageService {

        private CompletableFuture<Void> pendingSave;

        @Override
        public <T> CompletableFuture<T> load(
                Path path,
                Class<T> type,
                Supplier<T> defaultSupplier
        ) {
            return CompletableFuture.completedFuture(defaultSupplier.get());
        }

        @Override
        public <T> CompletableFuture<Void> save(Path path, T value) {
            if (pendingSave != null) throw new IllegalStateException("test save already pending");
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

    private static final class NoopUsers implements UserService {

        @Override
        public CompletableFuture<CellUser> load(UUID uuid) {
            return CompletableFuture.failedFuture(new UnsupportedOperationException());
        }

        @Override
        public CompletableFuture<CellUser> loadFromPlayer(Object player) {
            return CompletableFuture.failedFuture(new UnsupportedOperationException());
        }

        @Override
        public Optional<CellUser> cached(UUID uuid) {
            return Optional.empty();
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
        public <T> CompletableFuture<T> update(UUID uuid, Function<CellUser, T> mutation) {
            return CompletableFuture.failedFuture(new UnsupportedOperationException());
        }

        @Override
        public void markDirty(UUID uuid) {
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

    private static final class NoopPlatform implements PlatformService {

        @Override
        public Optional<CellPlayer> player(CommandInvocation invocation) {
            return Optional.empty();
        }

        @Override
        public Optional<CellPlayer> player(Object nativeHandle) {
            return Optional.empty();
        }

        @Override
        public Optional<CellPlayer> onlinePlayer(String name) {
            return Optional.empty();
        }

        @Override
        public List<CellPlayer> onlinePlayers() {
            return List.of();
        }

        @Override
        public List<String> worlds() {
            return List.of();
        }

        @Override
        public String defaultWorld() {
            return "minecraft:overworld";
        }

        @Override
        public CellLocation location(CellPlayer player) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CompletableFuture<Boolean> teleport(CellPlayer player, CellLocation location) {
            return CompletableFuture.completedFuture(false);
        }

        @Override
        public Optional<CellLocation> safeLocation(CellLocation location) {
            return Optional.empty();
        }

        @Override
        public Optional<CellLocation> highestLocation(String world, double x, double z) {
            return Optional.empty();
        }

        @Override
        public Optional<CellLocation> targetLocation(CellPlayer player, int maxDistance) {
            return Optional.empty();
        }

    }

}
