package top.likoslupus.cellulosesz.modules.playerstate.application;

import top.likoslupus.cellulosesz.api.admin.AdminResult;
import top.likoslupus.cellulosesz.api.command.execution.CommandOutcome;
import top.likoslupus.cellulosesz.api.text.LocalizedMessage;

import java.util.List;
import java.util.Map;

import static top.likoslupus.cellulosesz.api.validation.Checks.requireNonEmpty;

import static java.util.Objects.requireNonNull;

public record PlayerStateCommandResult(
        CommandOutcome.Status status,
        List<LocalizedMessage> messages
) {

    public PlayerStateCommandResult(
            boolean success,
            List<LocalizedMessage> messages
    ) {
        this(
                success
                        ? CommandOutcome.Status.SUCCESS
                        : CommandOutcome.Status.REJECTED, messages
        );
    }

    public PlayerStateCommandResult {
        requireNonNull(status, "status");
        messages = List.copyOf(requireNonNull(messages, "messages"));
        requireNonEmpty(messages, "messages");
    }

    public static PlayerStateCommandResult success(String key) {
        return new PlayerStateCommandResult(
                CommandOutcome.Status.SUCCESS,
                List.of(LocalizedMessage.of(key))
        );
    }

    public static PlayerStateCommandResult success(String key, Map<String, ?> values) {
        return new PlayerStateCommandResult(
                CommandOutcome.Status.SUCCESS,
                List.of(LocalizedMessage.of(key, values))
        );
    }

    public static PlayerStateCommandResult success(List<LocalizedMessage> values) {
        return new PlayerStateCommandResult(CommandOutcome.Status.SUCCESS, values);
    }

    public static PlayerStateCommandResult failure(String key) {
        return new PlayerStateCommandResult(
                CommandOutcome.Status.REJECTED,
                List.of(LocalizedMessage.of(key))
        );
    }

    public static PlayerStateCommandResult failure(String key, Map<String, ?> values) {
        return new PlayerStateCommandResult(
                CommandOutcome.Status.REJECTED,
                List.of(LocalizedMessage.of(key, values))
        );
    }

    public static PlayerStateCommandResult failed(String key) {
        return new PlayerStateCommandResult(
                CommandOutcome.Status.FAILED,
                List.of(LocalizedMessage.of(key))
        );
    }

    public static PlayerStateCommandResult failed(String key, Map<String, ?> values) {
        return new PlayerStateCommandResult(
                CommandOutcome.Status.FAILED,
                List.of(LocalizedMessage.of(key, values))
        );
    }

    public static PlayerStateCommandResult from(AdminResult result) {
        var status = switch (result.status()) {
            case SUCCESS -> CommandOutcome.Status.SUCCESS;
            case PARTIAL_SUCCESS -> CommandOutcome.Status.PARTIAL;
            case PERSISTENCE_FAILURE,
                 NATIVE_COMMAND_FAILURE,
                 PLATFORM_FAILURE,
                 ROLLBACK_FAILURE,
                 FAILURE -> CommandOutcome.Status.FAILED;
            default -> CommandOutcome.Status.REJECTED;
        };
        return new PlayerStateCommandResult(status, List.of(result.message()));
    }

    public boolean success() {
        return status == CommandOutcome.Status.SUCCESS;
    }

}
