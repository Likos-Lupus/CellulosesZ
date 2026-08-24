package top.likoslupus.cellulosesz.core.text;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;

/**
 * Reads the locale advertised by an online Minecraft client.
 */
public interface ClientLocaleService {

    String clientLocale(CellPlayer player);

}
