package top.likoslupus.cellulosesz.api.text

import top.likoslupus.cellulosesz.api.platform.CellPlayer

public interface LocaleResolver {

    public fun locale(player: CellPlayer): String

    public fun consoleLocale(): String

}
