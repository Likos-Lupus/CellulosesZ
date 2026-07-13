package top.likoslupus.cellulosesz.modules.teleport.service;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.storage.StorageService;
import top.likoslupus.cellulosesz.api.teleport.BackLocationService;
import top.likoslupus.cellulosesz.api.teleport.CellLocation;
import top.likoslupus.cellulosesz.modules.teleport.data.BackLocationDocument;

import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

public final class DefaultBackLocationService implements BackLocationService {

    private final PlatformService platform;
    private final StorageService storage;
    private final Path path;
    private final BackLocationDocument document;

    public DefaultBackLocationService(
            PlatformService platform,
            StorageService storage,
            Path path
    ) {
        this.platform = platform;
        this.storage = storage;
        this.path = path;
        this.document = storage.load(path, BackLocationDocument.class, BackLocationDocument::new).join();
    }

    @Override
    public synchronized void remember(CellPlayer player) {
        remember(player.uuid(), platform.location(player));
    }

    @Override
    public synchronized void remember(UUID uuid, CellLocation location) {
        var key = uuid.toString();
        var previous = document.locations.put(key, location);
        try {
            storage.save(path, document).join();
        } catch (RuntimeException exception) {
            if (previous == null) {
                document.locations.remove(key);
            } else {
                document.locations.put(key, previous);
            }
            throw exception;
        }
    }

    @Override
    public synchronized void forget(UUID uuid) {
        var key = uuid.toString();
        var previous = document.locations.remove(key);
        if (previous == null) return;

        try {
            storage.save(path, document).join();
        } catch (RuntimeException exception) {
            document.locations.put(key, previous);
            throw exception;
        }
    }

    @Override
    public synchronized Optional<CellLocation> location(UUID uuid) {
        return Optional.ofNullable(document.locations.get(uuid.toString()));
    }

}
