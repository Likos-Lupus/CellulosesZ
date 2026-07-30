package top.likoslupus.cellulosesz.modules.playerstate.application;

import top.likoslupus.cellulosesz.api.admin.AdminResult;
import top.likoslupus.cellulosesz.api.text.LocalizedMessage;

import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;
import static top.likoslupus.cellulosesz.api.validation.Checks.requireNonEmpty;

public record PlayerStateCommandResult(
        boolean success,
        List<LocalizedMessage> messages
) {

    public PlayerStateCommandResult {
        messages = List.copyOf(requireNonNull(messages, "messages"));
        requireNonEmpty(messages, "messages");
    }

    public static PlayerStateCommandResult success(String key) {
        return new PlayerStateCommandResult(true, List.of(LocalizedMessage.of(key)));
    }

    public static PlayerStateCommandResult success(String key, Map<String, ?> values) {
        return new PlayerStateCommandResult(true, List.of(LocalizedMessage.of(key, values)));
    }

    public static PlayerStateCommandResult success(List<LocalizedMessage> values) {
        return new PlayerStateCommandResult(true, values);
    }

    public static PlayerStateCommandResult failure(String key) {
        return new PlayerStateCommandResult(false, List.of(LocalizedMessage.of(key)));
    }

    public static PlayerStateCommandResult failure(String key, Map<String, ?> values) {
        return new PlayerStateCommandResult(false, List.of(LocalizedMessage.of(key, values)));
    }

    public static PlayerStateCommandResult from(AdminResult result) {
        return new PlayerStateCommandResult(result.success(), List.of(result.message()));
    }

}
