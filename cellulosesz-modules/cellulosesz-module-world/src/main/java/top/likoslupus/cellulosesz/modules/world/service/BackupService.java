package top.likoslupus.cellulosesz.modules.world.service;

import top.likoslupus.cellulosesz.api.world.BackupPlatformService;
import top.likoslupus.cellulosesz.modules.world.config.WorldConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import static top.likoslupus.cellulosesz.api.validation.Checks.requireInRange;
import static top.likoslupus.cellulosesz.api.validation.Checks.requireNonBlank;

import static java.util.Objects.requireNonNull;

public final class BackupService {

    private final BackupPlatformService platform;
    private final Path dataRoot;
    private final AtomicBoolean running = new AtomicBoolean();
    private volatile BackupSnapshot config;

    public BackupService(
            BackupPlatformService platform,
            Path dataRoot,
            WorldConfig config
    ) {
        this.platform = requireNonNull(platform, "platform");
        this.dataRoot = requireNonNull(dataRoot, "dataRoot").toAbsolutePath().normalize();
        configure(config);
    }

    public void configure(WorldConfig config) {
        var source = requireNonNull(config, "config");
        var backup = requireNonNull(source.backup, "backup");
        var directory = requireNonBlank(backup.directory, "backup.directory").trim();
        var retain = requireInRange(backup.retain, 1, 10_000, "backup.retain");
        var resolved = dataRoot.resolve(directory).normalize();

        if (!resolved.startsWith(dataRoot)) {
            throw new IllegalArgumentException("backup.directory escapes data directory");
        }

        this.config = new BackupSnapshot(backup.enabled, directory, retain);
    }

    public CompletableFuture<Path> createBackup() {
        var current = config;
        if (!current.enabled()) {
            return CompletableFuture.failedFuture(new IllegalStateException("Backups are disabled"));
        }

        if (!running.compareAndSet(false, true)) {
            return CompletableFuture.failedFuture(new IllegalStateException(
                    "A backup is already running"
            ));
        }

        var directory = dataRoot
                .resolve(current.directory())
                .normalize();
        return platform
                .create(directory)
                .thenApply(result -> {
                    prune(directory, current.retain());
                    return result.archive();
                })
                .whenComplete((_, _) -> running.set(false));
    }

    private static void prune(Path directory, int retain) {
        try (var files = Files.list(directory)) {
            var backups = files
                    .filter(path -> path.getFileName().toString().endsWith(".zip"))
                    .sorted(Comparator.comparingLong(BackupService::modified).reversed())
                    .toList();

            for (var index = retain; index < backups.size(); index++) {
                Files.delete(backups.get(index));
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to prune old backups", exception);
        }
    }

    private static long modified(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to inspect backup", exception);
        }
    }

    public boolean running() {
        return running.get();
    }

    private record BackupSnapshot(
            boolean enabled,
            String directory,
            int retain
    ) {

        private BackupSnapshot {
            directory = requireNonBlank(directory, "directory");
            requireInRange(retain, 1, 10_000, "retain");
        }

    }

}
