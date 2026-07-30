package top.likoslupus.cellulosesz.modules.economy.application;

import top.likoslupus.cellulosesz.api.text.LocalizedMessage;

import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;

public record EconomyCommandResult(
        boolean success,
        List<LocalizedMessage> messages
) {

    public EconomyCommandResult {
        messages = List.copyOf(requireNonNull(messages, "messages"));
        if (messages.isEmpty()) {
            throw new IllegalArgumentException("messages must not be empty");
        }
    }

    public static EconomyCommandResult success(String key) {
        return new EconomyCommandResult(true, List.of(LocalizedMessage.of(key)));
    }

    public static EconomyCommandResult success(String key, Map<String, ?> values) {
        return new EconomyCommandResult(true, List.of(LocalizedMessage.of(key, values)));
    }

    public static EconomyCommandResult success(List<LocalizedMessage> values) {
        return new EconomyCommandResult(true, values);
    }

    public static EconomyCommandResult failure(String key) {
        return new EconomyCommandResult(false, List.of(LocalizedMessage.of(key)));
    }

    public static EconomyCommandResult failure(String key, Map<String, ?> values) {
        return new EconomyCommandResult(false, List.of(LocalizedMessage.of(key, values)));
    }

    public static EconomyCommandResult failure(LocalizedMessage value) {
        return new EconomyCommandResult(false, List.of(value));
    }

}
