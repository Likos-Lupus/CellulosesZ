package top.likoslupus.cellulosesz.common.text;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.api.text.RichText;

/**
 * Locale lookup and rich-text delivery for an online player.
 */
public interface PlayerAudienceService {

    String locale(CellPlayer player);

    PlatformResult<Void> send(CellPlayer player, RichText message);

}
