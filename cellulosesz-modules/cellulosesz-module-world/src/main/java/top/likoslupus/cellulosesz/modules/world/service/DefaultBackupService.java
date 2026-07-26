package top.likoslupus.cellulosesz.modules.world.service;

import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.world.BackupService;
import top.likoslupus.cellulosesz.modules.world.config.WorldConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Objects.requireNonNull;

public final class DefaultBackupService implements BackupService {

    private final PlatformService platform;
    private final Path dataRoot;
    private final AtomicBoolean running = new AtomicBoolean();
    private volatile BackupSnapshot config;

    public DefaultBackupService(
            PlatformService platform,
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
        var directory = requireNonNull(backup.directory, "backup.directory").trim();
        if (directory.isBlank()) throw new IllegalArgumentException("backup.directory must not be blank");
        if (backup.retain < 1) throw new IllegalArgumentException("backup.retain must be positive");
        var resolved = dataRoot.resolve(directory).normalize();
        if (!resolved.startsWith(dataRoot))
            throw new IllegalArgumentException("backup.directory escapes data directory");
        this.config = new BackupSnapshot(backup.enabled, directory, backup.retain);
    }

    @Override
    public CompletableFuture<Path> createBackup() {
        var current = config;
        if (!current.enabled()) {
            return CompletableFuture.failedFuture(new IllegalStateException("Backups are disabled"));
        }
        if (!running.compareAndSet(false, true)) {
            return CompletableFuture.failedFuture(new IllegalStateException("A backup is already running"));
        }
        var directory = dataRoot.resolve(current.directory()).normalize();
        return platform.backup(directory)
                .thenApply(created -> {
                    prune(directory, current.retain());
                    return created;
                })
                .whenComplete((ignored, failure) -> running.set(false));
    }

    @Override
    public boolean running() {
        return running.get();
    }

    private static void prune(Path directory, int retain) {
        try (var files = Files.list(directory)) {
            var backups = files.filter(path -> path.getFileName().toString().endsWith(".zip"))
                    .sorted(Comparator.comparingLong(DefaultBackupService::modified).reversed())
                    .toList();
            for (var index = retain; index < backups.size(); index++) Files.delete(backups.get(index));
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

    private record BackupSnapshot(
            boolean enabled,
            String directory,
            int retain
    ) {

        private BackupSnapshot {
            requireNonNull(directory, "directory");
            if (directory.isBlank()) throw new IllegalArgumentException("directory must not be blank");
            if (retain < 1) throw new IllegalArgumentException("retain must be positive");
        }

    }

}
