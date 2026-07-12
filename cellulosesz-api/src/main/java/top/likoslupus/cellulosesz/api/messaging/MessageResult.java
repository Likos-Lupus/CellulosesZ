package top.likoslupus.cellulosesz.api.messaging;

import top.likoslupus.cellulosesz.api.text.LocalizedMessage;

import java.util.Map;

public record MessageResult(
        boolean success,
        LocalizedMessage message
) {

    public static MessageResult success(LocalizedMessage message) {
        return new MessageResult(true, message);
    }

    public static MessageResult success(String key) {
        return new MessageResult(true, LocalizedMessage.of(key));
    }

    public static MessageResult success(String key, Map<String, ?> placeholders) {
        return new MessageResult(true, LocalizedMessage.of(key, placeholders));
    }

    public static MessageResult failure(LocalizedMessage message) {
        return new MessageResult(false, message);
    }

    public static MessageResult failure(String key) {
        return new MessageResult(false, LocalizedMessage.of(key));
    }

    public static MessageResult failure(String key, Map<String, ?> placeholders) {
        return new MessageResult(false, LocalizedMessage.of(key, placeholders));
    }

}
