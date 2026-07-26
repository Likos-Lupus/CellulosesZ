package top.likoslupus.cellulosesz.core.storage;

import top.likoslupus.cellulosesz.api.logging.CellulosesZLogger;
import top.likoslupus.cellulosesz.api.storage.StorageService;
import top.likoslupus.cellulosesz.core.config.JacksonCodecs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import java.util.stream.Stream;

public final class JacksonStorageService implements StorageService {

    private final Path root;
    private final Executor executor;
    private final CellulosesZLogger logger;
    private final ConcurrentHashMap<Path, CompletableFuture<Boolean>> fileTails = new ConcurrentHashMap<>();

    public JacksonStorageService(
            Path root,
            Executor executor,
            CellulosesZLogger logger
    ) {
        this.root = root.toAbsolutePath().normalize();
        this.executor = executor;
        this.logger = logger;
    }

    @Override
    public <T> CompletableFuture<T> load(
            Path path,
            Class<T> type,
            Supplier<T> defaultSupplier
    ) {
        var resolved = resolve(path);
        return enqueue(resolved, "load", () -> {
            ensureTargetInsideRoot(resolved);
            if (Files.notExists(resolved, LinkOption.NOFOLLOW_LINKS)) {
                var value = defaultSupplier.get();
                write(resolved, value);
                return value;
            }
            return read(resolved, type);
        });
    }

    @Override
    public <T> CompletableFuture<Void> save(Path path, T value) {
        var resolved = resolve(path);
        return enqueue(resolved, "save", () -> {
            ensureTargetInsideRoot(resolved);
            write(resolved, value);
            return Boolean.TRUE;
        }).thenAccept(_ -> {
        });
    }

    @Override
    public CompletableFuture<Boolean> exists(Path path) {
        var resolved = resolve(path);
        return enqueue(resolved, "inspect", () -> {
            ensureParentInsideRoot(resolved);
            return Files.isRegularFile(resolved, LinkOption.NOFOLLOW_LINKS);
        });
    }

    @Override
    public CompletableFuture<Boolean> delete(Path path) {
        var resolved = resolve(path);
        return enqueue(resolved, "delete", () -> {
            ensureTargetInsideRoot(resolved);
            return Files.deleteIfExists(resolved);
        });
    }

    @Override
    public <T> CompletableFuture<List<T>> loadDirectory(Path directory, Class<T> type) {
        var resolved = resolve(directory);
        return CompletableFuture.supplyAsync(() -> {
            try {
                ensureDirectoryInsideRoot(resolved);
                try (var stream = Files.list(resolved)) {
                    return stream
                            .filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
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
        if (path.isAbsolute()) {
            throw new IllegalArgumentException("Absolute storage paths are not allowed: " + path);
        }
        var resolved = root.resolve(path).normalize();
        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException("Storage path escapes the configured data root: " + path);
        }
        return resolved;
    }

    private <T> CompletableFuture<T> enqueue(
            Path resolved,
            String operation,
            IoSupplier<T> task
    ) {
        var result = new CompletableFuture<T>();
        fileTails.compute(resolved, (ignored, previous) -> {
            var tail = previous == null
                    ? CompletableFuture.completedFuture(Boolean.TRUE)
                    : previous;
            var next = tail.handle((_, _) -> Boolean.TRUE)
                    .thenApplyAsync(_ -> {
                        try {
                            result.complete(task.get());
                            return Boolean.TRUE;
                        } catch (Throwable exception) {
                            logger.error("Failed to " + operation + " document at " + resolved, exception);
                            result.completeExceptionally(exception);
                            throw exception instanceof RuntimeException runtime
                                    ? runtime
                                    : new CompletionException(exception);
                        }
                    }, executor);
            next.whenComplete((_, _) -> fileTails.remove(resolved, next));
            return next;
        });
        return result;
    }

    private void ensureTargetInsideRoot(Path path) throws IOException {
        ensureParentInsideRoot(path);
        if (Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
            if (Files.isSymbolicLink(path)) {
                throw new IOException("Storage document must not be a symbolic link: " + path);
            }
            var realRoot = root.toRealPath();
            var realTarget = path.toRealPath();
            if (!realTarget.startsWith(realRoot)) {
                throw new IOException("Storage document resolves outside the configured data root: " + path);
            }
        }
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

    private void ensureParentInsideRoot(Path path) throws IOException {
        var parent = path.getParent();
        if (parent == null) throw new IOException("Storage path has no parent: " + path);
        ensureDirectoryInsideRoot(parent);
    }

    private boolean json(Path path) {
        return path.getFileName().toString().endsWith(".json");
    }

    private void ensureDirectoryInsideRoot(Path directory) throws IOException {
        var normalized = directory.normalize();
        if (!normalized.startsWith(root)) {
            throw new IOException("Storage path escapes the configured data root: " + directory);
        }
        Files.createDirectories(root);
        if (Files.isSymbolicLink(root)) {
            throw new IOException("Storage root must not be a symbolic link: " + root);
        }

        var realRoot = root.toRealPath();
        var current = root;
        for (var segment : root.relativize(normalized)) {
            current = current.resolve(segment);
            if (Files.exists(current, LinkOption.NOFOLLOW_LINKS)) {
                if (Files.isSymbolicLink(current)) {
                    throw new IOException("Storage directory must not contain symbolic links: " + current);
                }
                if (!Files.isDirectory(current, LinkOption.NOFOLLOW_LINKS)) {
                    throw new IOException("Storage directory component is not a directory: " + current);
                }
            } else {
                Files.createDirectory(current);
            }
            if (!current.toRealPath().startsWith(realRoot)) {
                throw new IOException("Storage path resolves outside the configured data root: " + current);
            }
        }
    }

    @FunctionalInterface
    private interface IoSupplier<T> {

        T get() throws IOException;

    }

}
