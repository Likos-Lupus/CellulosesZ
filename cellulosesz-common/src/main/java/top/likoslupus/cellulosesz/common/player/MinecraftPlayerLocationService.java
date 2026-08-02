package top.likoslupus.cellulosesz.common.player;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.player.PlayerLocationPlatformService;
import top.likoslupus.cellulosesz.api.teleport.CellLocation;
import top.likoslupus.cellulosesz.common.lifecycle.MinecraftServerHandle;

import static java.util.Objects.requireNonNull;

public final class MinecraftPlayerLocationService implements PlayerLocationPlatformService {

    private final MinecraftServerHandle server;

    public MinecraftPlayerLocationService(MinecraftServerHandle server) {
        this.server = requireNonNull(server, "server");
    }

    @Override
    public CellLocation currentLocation(CellPlayer player) {
        var nativePlayer = MinecraftPlayers.requireOnline(server, player);
        var level = nativePlayer.level();

        return new CellLocation(
                level.dimension().identifier().toString(),
                nativePlayer.getX(), nativePlayer.getY(), nativePlayer.getZ(),
                nativePlayer.getYRot(), nativePlayer.getXRot()
        );
    }

}
