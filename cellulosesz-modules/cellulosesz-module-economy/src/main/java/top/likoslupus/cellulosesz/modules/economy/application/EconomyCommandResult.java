package top.likoslupus.cellulosesz.modules.economy.application;

import top.likoslupus.cellulosesz.api.command.execution.CommandOutcome;
import top.likoslupus.cellulosesz.api.text.LocalizedMessage;

import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;

public record EconomyCommandResult(
        CommandOutcome.Status status,
        List<LocalizedMessage> messages
) {

    public EconomyCommandResult(boolean success, List<LocalizedMessage> messages) {
        this(
                success
                        ? CommandOutcome.Status.SUCCESS
                        : CommandOutcome.Status.REJECTED, messages
        );
    }

    public EconomyCommandResult {
        requireNonNull(status, "status");
        messages = List.copyOf(requireNonNull(messages, "messages"));
        if (messages.isEmpty()) {
            throw new IllegalArgumentException("messages must not be empty");
        }
    }

    public static EconomyCommandResult success(String key) {
        return new EconomyCommandResult(
                CommandOutcome.Status.SUCCESS,
                List.of(LocalizedMessage.of(key))
        );
    }

    public static EconomyCommandResult success(String key, Map<String, ?> values) {
        return new EconomyCommandResult(
                CommandOutcome.Status.SUCCESS,
                List.of(LocalizedMessage.of(key, values))
        );
    }

    public static EconomyCommandResult success(List<LocalizedMessage> values) {
        return new EconomyCommandResult(CommandOutcome.Status.SUCCESS, values);
    }

    public static EconomyCommandResult failure(String key) {
        return new EconomyCommandResult(
                CommandOutcome.Status.REJECTED,
                List.of(LocalizedMessage.of(key))
        );
    }

    public static EconomyCommandResult failure(String key, Map<String, ?> values) {
        return new EconomyCommandResult(
                CommandOutcome.Status.REJECTED,
                List.of(LocalizedMessage.of(key, values))
        );
    }

    public static EconomyCommandResult failure(LocalizedMessage value) {
        return new EconomyCommandResult(CommandOutcome.Status.REJECTED, List.of(value));
    }

    public static EconomyCommandResult failed(String key) {
        return new EconomyCommandResult(
                CommandOutcome.Status.FAILED,
                List.of(LocalizedMessage.of(key))
        );
    }

    public static EconomyCommandResult failed(String key, Map<String, ?> values) {
        return new EconomyCommandResult(
                CommandOutcome.Status.FAILED,
                List.of(LocalizedMessage.of(key, values))
        );
    }

    public boolean success() {
        return status == CommandOutcome.Status.SUCCESS;
    }

}
