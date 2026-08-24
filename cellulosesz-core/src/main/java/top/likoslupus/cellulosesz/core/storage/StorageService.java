package top.likoslupus.cellulosesz.core.storage;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public interface StorageService {

    /**
     * Reads a document or returns a new default without writing it.
     */
    <T> CompletableFuture<T> loadOrDefault(
            Path path,
            Class<T> type,
            Supplier<T> defaultSupplier
    );

    /**
     * Reads a document, creating and persisting the default when it does not exist.
     */
    <T> CompletableFuture<T> createIfMissing(
            Path path,
            Class<T> type,
            Supplier<T> defaultSupplier
    );

    <T> CompletableFuture<Void> save(Path path, T value);

    CompletableFuture<Boolean> exists(Path path);

    CompletableFuture<Boolean> delete(Path path);

    <T> CompletableFuture<List<T>> loadDirectory(Path directory, Class<T> type);

}
