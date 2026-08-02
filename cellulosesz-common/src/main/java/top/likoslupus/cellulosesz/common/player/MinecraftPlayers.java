package top.likoslupus.cellulosesz.common.player;

import net.minecraft.server.level.ServerPlayer;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.common.lifecycle.MinecraftServerHandle;

import static java.util.Objects.requireNonNull;

/**
 * Converts stable API identities at the Minecraft boundary without retaining native player
 * instances.
 */
public final class MinecraftPlayers {

    private MinecraftPlayers() {
    }

    public static ServerPlayer requireOnline(
            MinecraftServerHandle server,
            CellPlayer player
    ) {
        var current = requireNonNull(server, "server").requireRunning();
        var identity = requireNonNull(player, "player");
        var nativePlayer = current.getPlayerList().getPlayer(identity.uuid());

        if (nativePlayer == null || nativePlayer.hasDisconnected()) {
            throw new MinecraftPlayerUnavailableException(identity.uuid());
        }

        return nativePlayer;
    }

    public static CellPlayer wrap(ServerPlayer player) {
        var value = requireNonNull(player, "player");
        return new CellPlayer(value.getUUID(), value.getGameProfile().name());
    }

}
