package top.likoslupus.cellulosesz.common.player;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.text.ClientLocaleService;
import top.likoslupus.cellulosesz.common.lifecycle.MinecraftServerHandle;

import static java.util.Objects.requireNonNull;

public final class MinecraftClientLocaleService implements ClientLocaleService {

    private final MinecraftServerHandle server;

    public MinecraftClientLocaleService(MinecraftServerHandle server) {
        this.server = requireNonNull(server, "server");
    }

    @Override
    public String clientLocale(CellPlayer player) {
        return MinecraftPlayers.requireOnline(server, player).clientInformation().language();
    }

}
