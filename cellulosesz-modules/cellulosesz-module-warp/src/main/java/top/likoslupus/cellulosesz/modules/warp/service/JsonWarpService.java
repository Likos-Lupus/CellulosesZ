package top.likoslupus.cellulosesz.modules.warp.service;

import top.likoslupus.cellulosesz.api.storage.StorageService;
import top.likoslupus.cellulosesz.api.teleport.CellLocation;
import top.likoslupus.cellulosesz.api.warp.Warp;
import top.likoslupus.cellulosesz.api.warp.WarpService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class JsonWarpService implements WarpService {

    private final StorageService storage;
    private final Path warpsDirectory;
    private final ConcurrentHashMap<String, Warp> warps = new ConcurrentHashMap<>();

    public JsonWarpService(
            StorageService storage,
            Path warpsDirectory
    ) {
        this.storage = storage;
        this.warpsDirectory = warpsDirectory;
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

        if (cached != null) return CompletableFuture.completedFuture(Optional.of(cached));
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
    public CompletableFuture<Warp> setWarp(String name, CellLocation location, UUID creator) {
        var key = normalize(name);
        var warp = new Warp(key, location);

        warp.createdBy = creator;
        warp.permission = "cellulosesz.warp." + key;
        warps.put(key, warp);

        return storage.save(path(key), warp).thenApply(_ -> warp);
    }

    @Override
    public CompletableFuture<Boolean> deleteWarp(String name) {
        var key = normalize(name);
        var removed = warps.remove(key) != null;

        return CompletableFuture.supplyAsync(() -> {
            try {
                Files.deleteIfExists(path(key));
                return removed;
            } catch (Exception exception) {
                return false;
            }
        });
    }

    @Override
    public CompletableFuture<Void> reload() {
        return storage.loadDirectory(warpsDirectory, Warp.class)
                .thenAccept(loaded -> {
                    warps.clear();
                    loaded.stream()
                            .filter(warp -> !warp.name.isBlank())
                            .forEach(warp -> warps.put(normalize(warp.name), warp));
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
