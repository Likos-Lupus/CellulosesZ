package top.likoslupus.cellulosesz.api.command;

import top.likoslupus.cellulosesz.api.platform.operation.PlatformOperationStatus;
import top.likoslupus.cellulosesz.api.player.ResolvedPlayer;
import top.likoslupus.cellulosesz.api.text.LocalizedMessage;
import top.likoslupus.cellulosesz.api.text.RichText;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface CommandInvocation {

    Object nativeSource();

    String label();

    boolean player();

    Optional<String> playerName();

    default Optional<UUID> playerUuid() {
        return Optional.empty();
    }

    boolean hasPermission(String permission);

    default String auditSummary() {
        return "arguments=" + args().length;
    }

    String[] args();

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

    default void platformError(PlatformOperationStatus status) {
        var key = switch (status) {
            case INVALID_ARGUMENT -> "commands.common.platform.invalid-argument";
            case TARGET_NOT_FOUND, WORLD_NOT_FOUND -> "commands.common.platform.target-not-found";
            case STATE_NOT_ALLOWED, WRONG_THREAD, NOT_READY, UNSAFE_DESTINATION ->
                    "commands.common.platform.state-not-allowed";
            case EXEMPT -> "commands.common.platform.exempt";
            case UNSUPPORTED -> "commands.common.platform.unsupported";
            case CONFLICT -> "commands.common.platform.conflict";
            case PARTIAL_SUCCESS -> "commands.common.platform.partial-success";
            case ROLLBACK_FAILED -> "commands.common.platform.rollback-failed";
            case SUCCESS, INTERNAL_ERROR -> "commands.common.platform.internal-error";
        };
        errorKey(key);
    }

    default void errorKey(String key) {
        errorKey(key, Map.of());
    }

}
