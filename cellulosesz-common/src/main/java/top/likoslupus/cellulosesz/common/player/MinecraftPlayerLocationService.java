package top.likoslupus.cellulosesz.common.player;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.player.PlayerLocationPlatformService;
import top.likoslupus.cellulosesz.api.teleport.CellLocation;

public final class MinecraftPlayerLocationService implements PlayerLocationPlatformService {

    @Override
    public CellLocation currentLocation(CellPlayer player) {
        var nativePlayer = MinecraftPlayers.requireOnline(player);
        var level = nativePlayer.level();

        return new CellLocation(
                level.dimension().identifier().toString(),
                nativePlayer.getX(), nativePlayer.getY(), nativePlayer.getZ(),
                nativePlayer.getYRot(), nativePlayer.getXRot()
        );
    }

}
