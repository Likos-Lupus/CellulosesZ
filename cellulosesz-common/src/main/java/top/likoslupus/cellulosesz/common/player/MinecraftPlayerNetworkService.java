package top.likoslupus.cellulosesz.common.player;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.player.PlayerNetworkService;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Optional;

public final class MinecraftPlayerNetworkService implements PlayerNetworkService {

    @Override
    public Optional<InetAddress> address(CellPlayer player) {
        var remote = MinecraftPlayers.requireOnline(player).connection.getRemoteAddress();
        if (remote instanceof InetSocketAddress socket) {
            return Optional.ofNullable(socket.getAddress());
        }
        return Optional.empty();
    }

}
