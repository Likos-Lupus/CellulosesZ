package top.likoslupus.cellulosesz.fabric.player;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.player.PlayerLocationPlatformService;
import top.likoslupus.cellulosesz.api.teleport.CellLocation;
import top.likoslupus.cellulosesz.fabric.FabricPlatformService;

import static java.util.Objects.requireNonNull;

public final class FabricPlayerLocationService implements PlayerLocationPlatformService {

    private final FabricPlatformService platform;

    public FabricPlayerLocationService(FabricPlatformService platform) {
        this.platform = requireNonNull(platform, "platform");
    }

    @Override
    public CellLocation currentLocation(CellPlayer player) {
        return platform.location(player);
    }

}
