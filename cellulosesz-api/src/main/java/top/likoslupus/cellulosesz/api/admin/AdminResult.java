package top.likoslupus.cellulosesz.api.admin;

import top.likoslupus.cellulosesz.api.text.LocalizedMessage;

import java.util.Map;

public record AdminResult(
        boolean success,
        LocalizedMessage message
) {

    public static AdminResult success(LocalizedMessage message) {
        return new AdminResult(true, message);
    }

    public static AdminResult success(String key) {
        return new AdminResult(true, LocalizedMessage.of(key));
    }

    public static AdminResult success(String key, Map<String, ?> placeholders) {
        return new AdminResult(true, LocalizedMessage.of(key, placeholders));
    }

    public static AdminResult failure(LocalizedMessage message) {
        return new AdminResult(false, message);
    }

    public static AdminResult failure(String key) {
        return new AdminResult(false, LocalizedMessage.of(key));
    }

    public static AdminResult failure(String key, Map<String, ?> placeholders) {
        return new AdminResult(false, LocalizedMessage.of(key, placeholders));
    }

}
