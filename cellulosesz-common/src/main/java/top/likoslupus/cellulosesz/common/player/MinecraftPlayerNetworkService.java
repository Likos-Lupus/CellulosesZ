package top.likoslupus.cellulosesz.common.player;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.player.PlayerNetworkService;
import top.likoslupus.cellulosesz.common.lifecycle.MinecraftServerHandle;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public final class MinecraftPlayerNetworkService implements PlayerNetworkService {

    private final MinecraftServerHandle server;

    public MinecraftPlayerNetworkService(MinecraftServerHandle server) {
        this.server = requireNonNull(server, "server");
    }

    @Override
    public Optional<InetAddress> address(CellPlayer player) {
        var remote = MinecraftPlayers.requireOnline(server, player).connection.getRemoteAddress();
        if (remote instanceof InetSocketAddress socket) {
            return Optional.ofNullable(socket.getAddress());
        }
        return Optional.empty();
    }

}
