package top.likoslupus.cellulosesz.common.player;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.text.ClientLocaleService;

public final class MinecraftClientLocaleService implements ClientLocaleService {

    @Override
    public String clientLocale(CellPlayer player) {
        return MinecraftPlayers.requireOnline(player).clientInformation().language();
    }

}
