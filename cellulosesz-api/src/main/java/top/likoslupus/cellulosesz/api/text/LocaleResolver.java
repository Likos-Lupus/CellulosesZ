package top.likoslupus.cellulosesz.api.text;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;

public interface LocaleResolver {

    String locale(CellPlayer player);

    String consoleLocale();

}
