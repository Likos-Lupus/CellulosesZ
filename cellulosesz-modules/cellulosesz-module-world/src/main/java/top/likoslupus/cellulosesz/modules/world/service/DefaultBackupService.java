package top.likoslupus.cellulosesz.modules.world.service;

import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.world.BackupService;
import top.likoslupus.cellulosesz.modules.world.config.WorldConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public final class DefaultBackupService implements BackupService {

    private final PlatformService platform;
    private final Path dataRoot;
    private final AtomicBoolean running = new AtomicBoolean();
    private volatile WorldConfig config;

    public DefaultBackupService(PlatformService platform, Path dataRoot, WorldConfig config) {
        this.platform = Objects.requireNonNull(platform, "platform");
        this.dataRoot = Objects.requireNonNull(dataRoot, "dataRoot").toAbsolutePath().normalize();
        configure(config);
    }

    public void configure(WorldConfig config) {
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(config.backup, "backup");
        if (config.backup.directory.isBlank()) throw new IllegalArgumentException("backup.directory must not be blank");
        if (config.backup.retain < 1) throw new IllegalArgumentException("backup.retain must be positive");
        var resolved = dataRoot.resolve(config.backup.directory).normalize();
        if (!resolved.startsWith(dataRoot))
            throw new IllegalArgumentException("backup.directory escapes data directory");
        this.config = config;
    }

    @Override
    public CompletableFuture<Path> createBackup() {
        var current = config;
        if (!current.backup.enabled) {
            return CompletableFuture.failedFuture(new IllegalStateException("Backups are disabled"));
        }
        if (!running.compareAndSet(false, true)) {
            return CompletableFuture.failedFuture(new IllegalStateException("A backup is already running"));
        }
        var directory = dataRoot.resolve(current.backup.directory).normalize();
        return platform.backup(directory)
                .thenApply(created -> {
                    prune(directory, current.backup.retain);
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

}
