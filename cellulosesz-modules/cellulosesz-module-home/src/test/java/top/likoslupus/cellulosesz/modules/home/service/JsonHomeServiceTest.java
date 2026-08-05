package top.likoslupus.cellulosesz.modules.home.service;

import org.junit.jupiter.api.Test;
import top.likoslupus.cellulosesz.api.home.HomeRenameStatus;
import top.likoslupus.cellulosesz.api.storage.StorageService;
import top.likoslupus.cellulosesz.api.teleport.CellLocation;
import top.likoslupus.cellulosesz.modules.home.persistence.HomeDocument;
import top.likoslupus.cellulosesz.modules.home.persistence.HomeMapper;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import org.jspecify.annotations.NullMarked;

import static org.junit.jupiter.api.Assertions.*;

final class JsonHomeServiceTest {

    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000099");
    private static final CellLocation OLD_LOCATION = new CellLocation(
            "minecraft:overworld",
            1, 64, 1,
            0, 0
    );
    private static final CellLocation NEW_LOCATION = new CellLocation(
            "minecraft:overworld",
            2, 70, 2,
            0, 0
    );

    @Test
    void rename_whenSourceMissingOrTargetExists_isAtomicNoOp() {
        var storage = new FakeStorage(documentWith("old", OLD_LOCATION, "new", NEW_LOCATION));
        var service = new JsonHomeService(storage, Path.of("homes"));

        assertEquals(
                HomeRenameStatus.SOURCE_MISSING,
                service.renameHomeDetailed(PLAYER, "missing", "fresh").join()
        );
        assertEquals(
                HomeRenameStatus.TARGET_EXISTS,
                service.renameHomeDetailed(PLAYER, "old", "new").join()
        );

        assertEquals(0, storage.saves.get());
        assertEquals(OLD_LOCATION, service.cachedHomes(PLAYER).get("old"));
        assertEquals(NEW_LOCATION, service.cachedHomes(PLAYER).get("new"));
    }

    private static HomeDocument documentWith(Object... entries) {
        var document = HomeMapper.empty(PLAYER);
        IntStream.iterate(
                        0,
                        index -> index < entries.length,
                        index -> index + 2
                )
                .forEach(index -> document.homes.put(
                                (String) entries[index],
                                HomeMapper.fromDomain((CellLocation) entries[index + 1])
                        )
                );
        return document;
    }

    @Test
    void rename_whenSaveSucceeds_publishesAfterPersistence() {
        var storage = new FakeStorage(documentWith("old", OLD_LOCATION));
        var service = new JsonHomeService(storage, Path.of("homes"));

        assertEquals(
                HomeRenameStatus.RENAMED,
                service.renameHomeDetailed(PLAYER, "old", "fresh").join()
        );

        assertEquals(1, storage.saves.get());
        assertFalse(service.cachedHomes(PLAYER).containsKey("old"));
        assertEquals(OLD_LOCATION, service.cachedHomes(PLAYER).get("fresh"));
    }

    @Test
    void rename_whenSaveFails_doesNotPublishPartialState() {
        var storage = new FakeStorage(documentWith("old", OLD_LOCATION));
        storage.failSave = true;
        var service = new JsonHomeService(storage, Path.of("homes"));

        assertThrows(
                CompletionException.class,
                () -> service.renameHomeDetailed(PLAYER, "old", "fresh").join()
        );

        assertEquals(1, storage.saves.get());
        assertEquals(OLD_LOCATION, service.cachedHomes(PLAYER).get("old"));
        assertFalse(service.cachedHomes(PLAYER).containsKey("fresh"));
    }

    @NullMarked
    private static final class FakeStorage implements StorageService {

        private final AtomicInteger saves = new AtomicInteger();
        private HomeDocument document;
        private boolean failSave;

        private FakeStorage(HomeDocument document) {
            this.document = document;
        }

        @Override
        public <T> CompletableFuture<T> loadOrDefault(
                Path path,
                Class<T> type,
                Supplier<T> defaultSupplier
        ) {
            return createIfMissing(path, type, defaultSupplier);
        }

        @Override
        public <T> CompletableFuture<T> createIfMissing(
                Path path,
                Class<T> type,
                Supplier<T> defaultSupplier
        ) {
            return CompletableFuture.completedFuture(type.cast(document));
        }

        @Override
        public <T> CompletableFuture<Void> save(Path path, T value) {
            saves.incrementAndGet();
            if (failSave) {
                return CompletableFuture.failedFuture(new IllegalStateException("save failed"));
            }

            document = (HomeDocument) value;
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Boolean> exists(Path path) {
            return CompletableFuture.completedFuture(true);
        }

        @Override
        public CompletableFuture<Boolean> delete(Path path) {
            return CompletableFuture.completedFuture(false);
        }

        @Override
        public <T> CompletableFuture<List<T>> loadDirectory(Path directory, Class<T> type) {
            return CompletableFuture.completedFuture(List.of());
        }

    }

}
