package top.likoslupus.cellulosesz.modules.warp.service;

import top.likoslupus.cellulosesz.api.storage.StorageService;
import top.likoslupus.cellulosesz.api.teleport.CellLocation;
import top.likoslupus.cellulosesz.api.warp.Warp;
import top.likoslupus.cellulosesz.api.warp.WarpService;
import top.likoslupus.cellulosesz.modules.warp.WarpConfig;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class JsonWarpService implements WarpService {

    private final StorageService storage;
    private final Path warpsDirectory;
    private final WarpConfig config;
    private final ConcurrentHashMap<String, Warp> warps = new ConcurrentHashMap<>();

    public JsonWarpService(
            StorageService storage,
            Path warpsDirectory,
            WarpConfig config
    ) {
        this.storage = storage;
        this.warpsDirectory = warpsDirectory;
        this.config = config;
    }

    @Override
    public CompletableFuture<List<Warp>> warps() {
        if (warps.isEmpty()) {
            return reload().thenApply(_ -> sorted());
        }
        return CompletableFuture.completedFuture(sorted());
    }

    @Override
    public CompletableFuture<Optional<Warp>> warp(String name) {
        var key = normalize(name);
        var cached = warps.get(key);

        if (cached != null) {
            return CompletableFuture.completedFuture(Optional.of(cached));
        }
        if (Files.notExists(path(key))) {
            return CompletableFuture.completedFuture(Optional.empty());
        }

        return storage.load(path(key), Warp.class, Warp::new)
                .thenApply(warp -> {
                    if (warp.name.isBlank()) return Optional.empty();

                    warps.put(normalize(warp.name), warp);
                    return Optional.of(warp);
                });
    }

    @Override
    public CompletableFuture<Warp> setWarp(
            String name,
            CellLocation location,
            UUID creator
    ) {
        var key = normalize(name);
        var warp = new Warp(key, location);

        warp.createdBy = creator;
        var previous = warps.put(key, warp);

        return storage.save(path(key), warp)
                .thenApply(_ -> warp)
                .whenComplete((_, exception) -> {
                    if (exception == null) return;
                    if (previous == null) {
                        warps.remove(key, warp);
                    } else {
                        warps.replace(key, warp, previous);
                    }
                });
    }

    @Override
    public CompletableFuture<Boolean> deleteWarp(String name) {
        var key = normalize(name);
        var previous = warps.remove(key);

        return CompletableFuture.supplyAsync(() -> {
            try {
                var deleted = Files.deleteIfExists(path(key));
                return deleted || previous != null;
            } catch (Exception exception) {
                if (previous != null) warps.putIfAbsent(key, previous);
                throw new java.util.concurrent.CompletionException(exception);
            }
        });
    }

    @Override
    public Optional<String> requiredPermission(Warp warp) {
        if (!config.perWarpPermission) return Optional.empty();
        return Optional.of("cellulosesz.warp." + normalize(warp.name));
    }

    @Override
    public CompletableFuture<Void> reload() {
        return storage.loadDirectory(warpsDirectory, Warp.class)
                .thenAccept(loaded -> {
                    var replacement = new LinkedHashMap<String, Warp>();
                    loaded.stream()
                            .filter(warp -> !warp.name.isBlank())
                            .forEach(warp -> replacement.put(normalize(warp.name), warp));
                    warps.clear();
                    warps.putAll(replacement);
                });
    }

    private Path path(String name) {
        return warpsDirectory.resolve(normalize(name) + ".json");
    }

    private List<Warp> sorted() {
        return warps.values().stream()
                .sorted(Comparator.comparing(warp -> warp.name))
                .toList();
    }

    private String normalize(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }

}
