package top.likoslupus.cellulosesz.api.storage;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public interface StorageService {

    <T> CompletableFuture<T> load(
            Path path,
            Class<T> type,
            Supplier<T> defaultSupplier
    );

    <T> CompletableFuture<Void> save(
            Path path,
            T value
    );

    <T> CompletableFuture<List<T>> loadDirectory(
            Path directory,
            Class<T> type
    );

}
