package top.likoslupus.cellulosesz.core.command.service;

import java.util.Collection;
import java.util.List;

public interface CommandAliasRegistry {

    void register(String command, Collection<String> aliases);

    List<String> aliases(String command);

}
