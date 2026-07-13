package top.likoslupus.cellulosesz.core.storage;

import top.likoslupus.cellulosesz.api.logging.CellulosesZLogger;
import top.likoslupus.cellulosesz.api.storage.StorageService;
import top.likoslupus.cellulosesz.core.config.JacksonCodecs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import java.util.stream.Stream;

public final class JacksonStorageService implements StorageService {

    private final Path root;
    private final Executor executor;
    private final CellulosesZLogger logger;

    public JacksonStorageService(
            Path root,
            Executor executor,
            CellulosesZLogger logger
    ) {
        this.root = root;
        this.executor = executor;
        this.logger = logger;
    }

    @Override
    public <T> CompletableFuture<T> load(
            Path path,
            Class<T> type,
            Supplier<T> defaultSupplier
    ) {
        return CompletableFuture.supplyAsync(() -> {
            var resolved = resolve(path);
            try {
                Files.createDirectories(resolved.getParent());
                if (Files.notExists(resolved)) {
                    var value = defaultSupplier.get();
                    write(resolved, value);
                    return value;
                }
                return read(resolved, type);
            } catch (IOException exception) {
                logger.error("Failed to load document at " + resolved, exception);
                throw new CompletionException(exception);
            }
        }, executor);
    }

    @Override
    public <T> CompletableFuture<Void> save(Path path, T value) {
        return CompletableFuture.runAsync(() -> {
            var resolved = resolve(path);
            try {
                Files.createDirectories(resolved.getParent());
                write(resolved, value);
            } catch (IOException exception) {
                logger.error("Failed to save document at " + resolved, exception);
                throw new CompletionException(exception);
            }
        }, executor);
    }

    @Override
    public <T> CompletableFuture<List<T>> loadDirectory(Path directory, Class<T> type) {
        return CompletableFuture.supplyAsync(() -> {
            var resolved = resolve(directory);

            try {
                Files.createDirectories(resolved);
                try (var stream = Files.list(resolved)) {
                    return stream
                            .filter(Files::isRegularFile)
                            .filter(path -> path.getFileName().toString().endsWith(".json")
                                    || path.getFileName().toString().endsWith(".yml")
                                    || path.getFileName().toString().endsWith(".yaml"))
                            .flatMap(path -> {
                                try {
                                    return Stream.of(read(path, type));
                                } catch (IOException exception) {
                                    logger.error("Failed to load document at " + path, exception);
                                    throw new CompletionException(exception);
                                }
                            })
                            .toList();
                }
            } catch (IOException exception) {
                logger.error("Failed to load document directory at " + resolved, exception);
                throw new CompletionException(exception);
            }
        }, executor);
    }

    private Path resolve(Path path) {
        return path.isAbsolute()
                ? path
                : root.resolve(path).normalize();
    }

    private void write(Path path, Object value) throws IOException {
        if (json(path)) {
            JacksonCodecs.writeJson(path, value);
        } else {
            JacksonCodecs.writeYaml(path, value);
        }
    }

    private <T> T read(Path path, Class<T> type) throws IOException {
        if (json(path)) {
            return JacksonCodecs.readJson(path, type);
        }
        return JacksonCodecs.readYaml(path, type);
    }

    private boolean json(Path path) {
        return path.getFileName().toString().endsWith(".json");
    }

}
