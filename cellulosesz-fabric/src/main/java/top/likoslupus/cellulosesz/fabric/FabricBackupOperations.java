package top.likoslupus.cellulosesz.fabric;

import net.minecraft.world.level.storage.LevelResource;
import top.likoslupus.cellulosesz.api.world.BackupPlatformService;
import top.likoslupus.cellulosesz.api.world.BackupResult;

import java.io.IOException;
import java.nio.file.*;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static java.util.Objects.requireNonNull;

public final class FabricBackupOperations implements BackupPlatformService {

    private final FabricServerAccess access;
    private final Clock clock;
    private final ExecutorService executor;
    private final AtomicBoolean running = new AtomicBoolean();

    public FabricBackupOperations(FabricServerAccess access) {
        this(access, Clock.systemUTC());
    }

    FabricBackupOperations(FabricServerAccess access, Clock clock) {
        this.access = requireNonNull(access, "access");
        this.clock = requireNonNull(clock, "clock");
        this.executor = Executors.newSingleThreadExecutor(
                Thread.ofVirtual().name("cellulosesz-backup-", 0).factory()
        );
    }

    @Override
    public CompletableFuture<BackupResult> create(Path destinationDirectory) {
        requireNonNull(destinationDirectory, "destinationDirectory");
        if (!running.compareAndSet(false, true)) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("A backup is already running")
            );
        }

        var barrier = new CompletableFuture<Path>();
        try (var server = access.requireServer()) {
            server.execute(() -> {
                try {
                    if (!server.saveEverything(false, true, true)) {
                        throw new IllegalStateException(
                                "Minecraft reported that the world save failed"
                        );
                    }
                    barrier.complete(server
                            .getWorldPath(LevelResource.ROOT)
                            .toAbsolutePath()
                            .normalize());
                } catch (Throwable failure) {
                    barrier.completeExceptionally(failure);
                }
            });
        }

        return barrier
                .thenCompose(worldRoot -> CompletableFuture.supplyAsync(
                        () -> createArchive(worldRoot, destinationDirectory),
                        executor
                ))
                .whenComplete((ignored, _) -> running.set(false));
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }

    private BackupResult createArchive(Path worldRoot, Path destinationDirectory) {
        var destinationRoot = destinationDirectory.toAbsolutePath().normalize();
        Path temporary = null;

        try {
            Files.createDirectories(destinationRoot);

            var realWorldRoot = worldRoot.toRealPath();
            var realDestinationRoot = destinationRoot.toRealPath();
            var filename = "backup-%s-%s.zip".formatted(
                    DateTimeFormatter
                            .ofPattern("yyyyMMdd-HHmmss-SSS")
                            .format(LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC)),
                    UUID.randomUUID().toString().substring(0, 8)
            );
            var destination = destinationRoot.resolve(filename).normalize();

            if (!destination.startsWith(destinationRoot)) {
                throw new IOException("Backup destination escaped its configured directory");
            }

            temporary = Files.createTempFile(destinationRoot, ".backup-", ".zip.tmp");
            var excludedRoot = realDestinationRoot.startsWith(realWorldRoot)
                    ? realDestinationRoot
                    : null;
            var counters = new long[2];

            try (
                    var output = Files.newOutputStream(
                            temporary,
                            StandardOpenOption.WRITE,
                            StandardOpenOption.TRUNCATE_EXISTING
                    );
                    var zip = new ZipOutputStream(output);
                    var paths = Files.walk(realWorldRoot)
            ) {
                paths.filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
                        .filter(path -> excludedRoot == null || !path.startsWith(excludedRoot))
                        .forEach(path -> addEntry(realWorldRoot, path, zip, counters));
            }

            try {
                Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, destination);
            }
            temporary = null;

            return new BackupResult(destination, counters[0], counters[1]);
        } catch (BackupFailure exception) {
            throw new IllegalStateException("Unable to create server backup", exception.getCause());
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to create server backup", exception);
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException _) {
                    // Best-effort cleanup after a failed archive.
                }
            }
        }
    }

    private static void addEntry(
            Path worldRoot,
            Path path,
            ZipOutputStream zip,
            long[] counters
    ) {
        var relative = worldRoot.relativize(path).normalize();
        if (relative.isAbsolute() || relative.startsWith("..")) {
            throw new BackupFailure(new IOException("Unsafe backup entry: " + path));
        }

        var entryName = relative.toString().replace('\\', '/');
        if (entryName.isBlank() || entryName.startsWith("/") || entryName.contains("../")) {
            throw new BackupFailure(new IOException("Unsafe backup entry: " + entryName));
        }

        try {
            zip.putNextEntry(new ZipEntry(entryName));
            counters[1] += Files.copy(path, zip);
            counters[0]++;
            zip.closeEntry();
        } catch (IOException exception) {
            throw new BackupFailure(exception);
        }
    }

    private static final class BackupFailure extends RuntimeException {

        private BackupFailure(IOException cause) {
            super(cause);
        }

    }

}
