package top.likoslupus.cellulosesz.core.i18n;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Strict composition root for the built-in message catalog.
 *
 * <p>Messages live in stable business-domain sources. Duplicate keys fail immediately instead of being silently
 * overwritten by {@link Map#putAll(Map)}.</p>
 */
final class BuiltInCommandMessages {

    private BuiltInCommandMessages() {
    }

    static Map<String, String> english() {
        var messages = new LinkedHashMap<String, String>();
        addAll(messages, CommonMessages.english());
        addAll(messages, CommandMessages.english());
        addAll(messages, AdminMessages.english());
        addAll(messages, EconomyMessages.english());
        addAll(messages, HomeMessages.english());
        addAll(messages, WarpMessages.english());
        addAll(messages, KitMessages.english());
        addAll(messages, ItemMessages.english());
        addAll(messages, MessagingMessages.english());
        addAll(messages, PlayerStateMessages.english());
        addAll(messages, TeleportMessages.english());
        addAll(messages, TextMessages.english());
        addAll(messages, WorldMessages.english());
        addAll(messages, SignMessages.english());
        return Map.copyOf(messages);
    }

    private static void addAll(Map<String, String> target, Map<String, String> source) {
        source.forEach((key, value) -> {
            if (target.putIfAbsent(key, value) != null) {
                throw new IllegalStateException("Duplicate built-in message key: " + key);
            }
        });
    }

    static Map<String, String> chinese() {
        var messages = new LinkedHashMap<String, String>();
        addAll(messages, CommonMessages.chinese());
        addAll(messages, CommandMessages.chinese());
        addAll(messages, AdminMessages.chinese());
        addAll(messages, EconomyMessages.chinese());
        addAll(messages, HomeMessages.chinese());
        addAll(messages, WarpMessages.chinese());
        addAll(messages, KitMessages.chinese());
        addAll(messages, ItemMessages.chinese());
        addAll(messages, MessagingMessages.chinese());
        addAll(messages, PlayerStateMessages.chinese());
        addAll(messages, TeleportMessages.chinese());
        addAll(messages, TextMessages.chinese());
        addAll(messages, WorldMessages.chinese());
        addAll(messages, SignMessages.chinese());
        return Map.copyOf(messages);
    }

}
