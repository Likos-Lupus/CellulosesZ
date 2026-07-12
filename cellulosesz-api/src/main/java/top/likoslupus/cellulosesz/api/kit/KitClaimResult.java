package top.likoslupus.cellulosesz.api.kit;

import top.likoslupus.cellulosesz.api.text.LocalizedMessage;

import java.util.Map;

public record KitClaimResult(
        boolean success,
        LocalizedMessage message
) {

    public static KitClaimResult success(LocalizedMessage message) {
        return new KitClaimResult(true, message);
    }

    public static KitClaimResult success(String key) {
        return new KitClaimResult(true, LocalizedMessage.of(key));
    }

    public static KitClaimResult success(String key, Map<String, ?> placeholders) {
        return new KitClaimResult(true, LocalizedMessage.of(key, placeholders));
    }

    public static KitClaimResult failure(LocalizedMessage message) {
        return new KitClaimResult(false, message);
    }

    public static KitClaimResult failure(String key) {
        return new KitClaimResult(false, LocalizedMessage.of(key));
    }

    public static KitClaimResult failure(String key, Map<String, ?> placeholders) {
        return new KitClaimResult(false, LocalizedMessage.of(key, placeholders));
    }

}
