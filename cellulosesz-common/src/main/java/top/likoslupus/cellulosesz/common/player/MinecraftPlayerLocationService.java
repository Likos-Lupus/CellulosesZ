package top.likoslupus.cellulosesz.common.player;

import net.minecraft.server.level.ServerPlayer;
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
        return snapshot(MinecraftPlayers.requireOnline(server, player));
    }

    public static CellLocation snapshot(ServerPlayer player) {
        var nativePlayer = requireNonNull(player, "player");
        var level = nativePlayer.level();

        return new CellLocation(
                level.dimension().identifier().toString(),
                nativePlayer.getX(), nativePlayer.getY(), nativePlayer.getZ(),
                nativePlayer.getYRot(), nativePlayer.getXRot()
        );
    }

}
