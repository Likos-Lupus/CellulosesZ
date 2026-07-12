package top.likoslupus.cellulosesz.api.text;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;

public interface LocaleResolver {

    String locale(CommandInvocation invocation);

    String locale(CellPlayer player);

    String consoleLocale();

}
