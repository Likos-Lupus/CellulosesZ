package top.likoslupus.cellulosesz.common.player;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.common.lifecycle.MinecraftServerHandle;
import top.likoslupus.cellulosesz.core.text.ClientLocaleService;

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
