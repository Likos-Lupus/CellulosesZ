package top.likoslupus.cellulosesz.api.command;

import java.util.Optional;

public interface CommandInvocation {

    Object nativeSource();

    String label();

    String[] args();

    boolean player();

    Optional<String> playerName();

    boolean hasPermission(String permission);

    void reply(String message);

    void error(String message);

}
