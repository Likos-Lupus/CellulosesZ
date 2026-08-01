package top.likoslupus.cellulosesz.core.storage;

import top.likoslupus.cellulosesz.api.lifecycle.AsyncCloseable;
import top.likoslupus.cellulosesz.api.logging.CellulosesZLogger;
import top.likoslupus.cellulosesz.api.storage.StorageService;
import top.likoslupus.cellulosesz.core.concurrent.SerialAsyncQueue;
import top.likoslupus.cellulosesz.core.config.JacksonCodecs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

public final class JacksonStorageService implements StorageService, AsyncCloseable {

    private static final int MAXIMUM_PENDING_OPERATIONS = 4_096;

    private final Path root;
    private final CellulosesZLogger logger;
    private final SerialAsyncQueue operations;

    public JacksonStorageService(
            Path root,
            Executor executor,
            CellulosesZLogger logger
    ) {
        this.root = root.toAbsolutePath().normalize();
        this.logger = logger;
        this.operations = new SerialAsyncQueue(executor, MAXIMUM_PENDING_OPERATIONS);
    }

    @Override
    public <T> CompletableFuture<T> loadOrDefault(
            Path path,
            Class<T> type,
            Supplier<T> defaultSupplier
    ) {
        var resolved = resolve(path);
        return enqueue(
                resolved,
                "load",
                () -> {
                    ensureTargetInsideRoot(resolved);
                    return Files.notExists(resolved, LinkOption.NOFOLLOW_LINKS)
                            ? defaultSupplier.get()
                            : read(resolved, type);
                }
        );
    }

    @Override
    public <T> CompletableFuture<T> createIfMissing(
            Path path,
            Class<T> type,
            Supplier<T> defaultSupplier
    ) {
        var resolved = resolve(path);
        return enqueue(
                resolved,
                "create or load",
                () -> {
                    ensureTargetInsideRoot(resolved);
                    if (Files.notExists(resolved, LinkOption.NOFOLLOW_LINKS)) {
                        var value = defaultSupplier.get();
                        write(resolved, value);

                        return value;
                    }

                    return read(resolved, type);
                }
        );
    }

    @Override
    public <T> CompletableFuture<Void> save(Path path, T value) {
        var resolved = resolve(path);
        return enqueue(
                resolved,
                "save",
                () -> {
                    ensureTargetInsideRoot(resolved);
                    write(resolved, value);

                    return Boolean.TRUE;
                }
        ).thenAccept(_ -> {
        });
    }

    @Override
    public CompletableFuture<Boolean> exists(Path path) {
        var resolved = resolve(path);
        return enqueue(
                resolved,
                "inspect",
                () -> {
                    ensureParentInsideRoot(resolved);
                    return Files.isRegularFile(resolved, LinkOption.NOFOLLOW_LINKS);
                }
        );
    }

    @Override
    public CompletableFuture<Boolean> delete(Path path) {
        var resolved = resolve(path);
        return enqueue(
                resolved,
                "delete",
                () -> {
                    ensureTargetInsideRoot(resolved);
                    return Files.deleteIfExists(resolved);
                }
        );
    }

    @Override
    public <T> CompletableFuture<List<T>> loadDirectory(Path directory, Class<T> type) {
        var resolved = resolve(directory);
        return enqueue(
                resolved,
                "load directory",
                () -> {
                    ensureDirectoryInsideRoot(resolved);
                    try (var stream = Files.list(resolved)) {
                        var paths = stream
                                .filter(path -> Files.isRegularFile(
                                        path,
                                        LinkOption.NOFOLLOW_LINKS
                                ))
                                .filter(JacksonStorageService::supportedDocument)
                                .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                                .toList();
                        var documents = new ArrayList<T>(paths.size());

                        for (var path : paths) {
                            documents.add(read(path, type));
                        }
                        return List.copyOf(documents);
                    }
                }
        );
    }

    private static boolean supportedDocument(Path path) {
        var fileName = path.getFileName().toString();
        return fileName.endsWith(".json")
                || fileName.endsWith(".yml")
                || fileName.endsWith(".yaml");
    }

    private void write(Path path, Object value) throws IOException {
        if (json(path)) {
            JacksonCodecs.writeJson(path, value);
        } else {
            JacksonCodecs.writeYaml(path, value);
        }
    }

    private Path resolve(Path path) {
        if (path.isAbsolute()) {
            throw new IllegalArgumentException(
                    "Absolute storage paths are not allowed: " + path
            );
        }

        var resolved = root.resolve(path).normalize();
        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException(
                    "Storage path escapes the configured data root: " + path
            );
        }

        return resolved;
    }

    private <T> CompletableFuture<T> enqueue(
            Path resolved,
            String operation,
            IoSupplier<T> task
    ) {
        return operations
                .submit(() -> {
                    try {
                        return CompletableFuture.completedFuture(task.get());
                    } catch (Throwable exception) {
                        logger.error(
                                "Failed to %s document at %s".formatted(operation, resolved),
                                exception
                        );

                        return CompletableFuture.failedFuture(exception instanceof CompletionException
                                ? exception
                                : new CompletionException(exception)
                        );
                    }
                });
    }

    private void ensureTargetInsideRoot(Path path) throws IOException {
        ensureParentInsideRoot(path);
        if (Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
            if (Files.isSymbolicLink(path)) {
                throw new IOException(
                        "Storage document must not be a symbolic link: " + path
                );
            }

            var realRoot = root.toRealPath();
            var realTarget = path.toRealPath();
            if (!realTarget.startsWith(realRoot)) {
                throw new IOException(
                        "Storage document resolves outside the configured data root: " + path
                );
            }
        }
    }

    private <T> T read(Path path, Class<T> type) throws IOException {
        return json(path)
                ? JacksonCodecs.readJson(path, type)
                : JacksonCodecs.readYaml(path, type);
    }

    private void ensureParentInsideRoot(Path path) throws IOException {
        var parent = path.getParent();
        if (parent == null) {
            throw new IOException("Storage path has no parent: " + path);
        }

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
                    throw new IOException(
                            "Storage directory must not contain symbolic links: " + current
                    );
                }

                if (!Files.isDirectory(current, LinkOption.NOFOLLOW_LINKS)) {
                    throw new IOException(
                            "Storage directory component is not a directory: " + current
                    );
                }
            } else {
                Files.createDirectory(current);
            }

            if (!current.toRealPath().startsWith(realRoot)) {
                throw new IOException(
                        "Storage path resolves outside the configured data root: " + current
                );
            }
        }
    }

    @Override
    public void stopAccepting() {
        operations.stopAccepting();
    }

    @Override
    public CompletableFuture<Void> drain() {
        return operations.drain();
    }

    @FunctionalInterface
    private interface IoSupplier<T> {

        T get() throws Exception;

    }

}
