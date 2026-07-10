package top.likoslupus.cellulosesz.modules.teleport.service;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.teleport.BackLocationService;
import top.likoslupus.cellulosesz.api.teleport.CellLocation;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DefaultBackLocationService implements BackLocationService {

    private final PlatformService platform;
    private final ConcurrentHashMap<UUID, CellLocation> locations = new ConcurrentHashMap<>();

    public DefaultBackLocationService(
            PlatformService platform
    ) {
        this.platform = platform;
    }

    @Override
    public void remember(CellPlayer player) {
        locations.put(player.uuid(), platform.location(player));
    }

    @Override
    public void remember(UUID uuid, CellLocation location) {
        locations.put(uuid, location);
    }

    @Override
    public Optional<CellLocation> location(UUID uuid) {
        return Optional.ofNullable(locations.get(uuid));
    }

}
