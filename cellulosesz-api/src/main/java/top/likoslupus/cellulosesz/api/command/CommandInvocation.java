package top.likoslupus.cellulosesz.api.command;

import top.likoslupus.cellulosesz.api.player.ResolvedPlayer;
import top.likoslupus.cellulosesz.api.text.LocalizedMessage;
import top.likoslupus.cellulosesz.api.text.RichText;

import java.util.Map;
import java.util.Optional;

public interface CommandInvocation {

    Object nativeSource();

    String label();

    String[] args();

    boolean player();

    Optional<String> playerName();

    boolean hasPermission(String permission);

    ResolvedPlayer resolvePlayer(String input);

    String locale();

    void reply(String message);

    void reply(RichText message);

    default void reply(LocalizedMessage message) {
        replyKey(message.key(), message.placeholders());
    }

    void replyKey(String key, Map<String, ?> placeholders);

    default void replyKey(String key) {
        replyKey(key, Map.of());
    }

    void error(String message);

    void error(RichText message);

    default void error(LocalizedMessage message) {
        errorKey(message.key(), message.placeholders());
    }

    void errorKey(String key, Map<String, ?> placeholders);

    default void errorKey(String key) {
        errorKey(key, Map.of());
    }

}
