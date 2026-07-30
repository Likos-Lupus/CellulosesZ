package top.likoslupus.cellulosesz.common.player;

import net.minecraft.server.level.ServerPlayer;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;

public final class MinecraftPlayers {

    private MinecraftPlayers() {
    }

    public static ServerPlayer requireOnline(CellPlayer player) {
        if (!(player.nativeHandle() instanceof ServerPlayer nativePlayer)
                || nativePlayer.hasDisconnected()
        ) {
            throw new IllegalStateException("Player is no longer online");
        }
        return nativePlayer;
    }

    public static CellPlayer wrap(ServerPlayer player) {
        return new CellPlayer(player.getUUID(), player.getGameProfile().name(), player);
    }

}
