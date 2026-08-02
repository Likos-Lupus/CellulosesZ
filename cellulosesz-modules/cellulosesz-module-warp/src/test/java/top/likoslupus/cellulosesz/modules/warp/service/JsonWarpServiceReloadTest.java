package top.likoslupus.cellulosesz.modules.warp.service;

import org.junit.jupiter.api.Test;
import top.likoslupus.cellulosesz.api.storage.StorageService;
import top.likoslupus.cellulosesz.api.teleport.CellLocation;
import top.likoslupus.cellulosesz.api.warp.Warp;
import top.likoslupus.cellulosesz.modules.warp.WarpConfig;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

final class JsonWarpServiceReloadTest {

    @Test
    void stagedReloadPublishesOnlyOnCommitAndRollbackRestoresPreviousSnapshot() {
        var storage = new ReloadStorage();
        storage.loaded = List.of(warp("old", 1.0));
        var service = service(storage);
        service.initialize().join();

        storage.loaded = List.of(warp("next", 2.0));
        var prepared = service.prepareReload(true).join();

        assertEquals(List.of("old"), names(service));
        prepared.commit().toCompletableFuture().join();
        assertEquals(List.of("next"), names(service));
        assertTrue(
                service
                        .requiredPermission(service.cachedWarp("next").orElseThrow())
                        .isPresent()
        );

        prepared.rollback().toCompletableFuture().join();
        assertEquals(List.of("old"), names(service));
        assertTrue(service.requiredPermission(service.cachedWarp("old").orElseThrow()).isEmpty());
    }

    private static Warp warp(String name, double x) {
        return new Warp(
                name,
                new CellLocation("minecraft:overworld", x, 64.0, x, 0.0F, 0.0F)
        );
    }

    private static JsonWarpService service(StorageService storage) {
        return new JsonWarpService(storage, Path.of("warps"), new WarpConfig());
    }

    private static List<String> names(JsonWarpService service) {
        return service.cachedWarps().stream().map(warp -> warp.name).toList();
    }

    @Test
    void stalePreparedReloadCannotOverwriteNewerMutation() {
        var storage = new ReloadStorage();
        storage.loaded = List.of(warp("old", 1.0));
        var service = service(storage);
        service.initialize().join();

        storage.loaded = List.of(warp("next", 2.0));
        var prepared = service.prepareReload(false).join();
        service.setWarp(
                "live",
                new CellLocation(
                        "minecraft:overworld",
                        3.0, 64.0, 3.0,
                        0.0F, 0.0F
                ),
                UUID.fromString("00000000-0000-0000-0000-000000000001")
        ).join();

        assertThrows(Exception.class, () -> prepared.commit().toCompletableFuture().join());
        assertTrue(service.cachedWarp("live").isPresent());
        assertTrue(service.cachedWarp("next").isEmpty());
    }

    @Test
    void failedPreparationLeavesLiveSnapshotUntouched() {
        var storage = new ReloadStorage();
        storage.loaded = List.of(warp("old", 1.0));
        var service = service(storage);
        service.initialize().join();

        storage.loaded = List.of(warp("duplicate", 1.0), warp("duplicate", 2.0));

        assertThrows(Exception.class, () -> service.prepareReload(false).join());
        assertEquals(List.of("old"), names(service));
    }

    private static final class ReloadStorage implements StorageService {

        private final Map<Path, Object> saved = new LinkedHashMap<>();
        private List<Warp> loaded = List.of();

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
            return CompletableFuture.completedFuture(loaded.stream().map(type::cast).toList());
        }

    }

}
