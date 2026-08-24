package top.likoslupus.cellulosesz.modules.messaging.domain;

import top.likoslupus.cellulosesz.api.command.execution.CommandOutcome;
import top.likoslupus.cellulosesz.api.text.LocalizedMessage;
import top.likoslupus.cellulosesz.api.text.MessageArguments;

import static java.util.Objects.requireNonNull;

public record MessageResult(
        CommandOutcome.Status status,
        LocalizedMessage message
) {

    public MessageResult {
        requireNonNull(status, "status");
        requireNonNull(message, "message");
    }

    public static MessageResult success(String key) {
        return success(LocalizedMessage.of(key));
    }

    public static MessageResult success(LocalizedMessage message) {
        return new MessageResult(CommandOutcome.Status.SUCCESS, message);
    }

    public static MessageResult success(
            String key,
            MessageArguments arguments
    ) {
        return success(LocalizedMessage.of(key, arguments));
    }

    public static MessageResult failure(String key) {
        return failure(LocalizedMessage.of(key));
    }

    public static MessageResult failure(LocalizedMessage message) {
        return new MessageResult(CommandOutcome.Status.REJECTED, message);
    }

    public static MessageResult failure(
            String key,
            MessageArguments arguments
    ) {
        return failure(LocalizedMessage.of(key, arguments));
    }

    public static MessageResult failed(String key) {
        return failed(LocalizedMessage.of(key));
    }

    public static MessageResult failed(LocalizedMessage message) {
        return new MessageResult(CommandOutcome.Status.FAILED, message);
    }

    public static MessageResult failed(
            String key,
            MessageArguments arguments
    ) {
        return failed(LocalizedMessage.of(key, arguments));
    }

    public boolean success() {
        return status == CommandOutcome.Status.SUCCESS;
    }

}
