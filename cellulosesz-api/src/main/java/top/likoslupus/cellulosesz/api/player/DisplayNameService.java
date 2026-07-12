package top.likoslupus.cellulosesz.api.player;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.text.RichText;

import java.util.UUID;

public interface DisplayNameService {

    RichText displayName(CellPlayer player);

    RichText displayName(UUID uuid, String fallbackName);

    String plainDisplayName(CellPlayer player);

    boolean validNickname(CellPlayer player, String nickname);

    String sanitizeNickname(CellPlayer player, String nickname);

    void refresh(CellPlayer player);

    void refreshAll();

}
