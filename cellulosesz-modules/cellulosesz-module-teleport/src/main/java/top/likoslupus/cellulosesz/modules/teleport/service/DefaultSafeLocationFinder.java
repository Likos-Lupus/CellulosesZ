package top.likoslupus.cellulosesz.modules.teleport.service;

import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.teleport.CellLocation;
import top.likoslupus.cellulosesz.api.teleport.SafeLocationFinder;

import java.util.Optional;

public final class DefaultSafeLocationFinder implements SafeLocationFinder {

    private final PlatformService platform;

    public DefaultSafeLocationFinder(PlatformService platform) {
        this.platform = platform;
    }

    @Override
    public Optional<CellLocation> safeLocation(CellLocation location) {
        return platform.safeLocation(location);
    }

}
