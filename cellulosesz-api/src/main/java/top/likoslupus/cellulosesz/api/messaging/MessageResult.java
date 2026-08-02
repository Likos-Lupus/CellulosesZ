package top.likoslupus.cellulosesz.api.messaging;

import top.likoslupus.cellulosesz.api.command.execution.CommandOutcome;
import top.likoslupus.cellulosesz.api.text.LocalizedMessage;

import java.util.Map;

import static java.util.Objects.requireNonNull;

public record MessageResult(
        CommandOutcome.Status status,
        LocalizedMessage message
) {

    public MessageResult(
            boolean success,
            LocalizedMessage message
    ) {
        this(
                success
                        ? CommandOutcome.Status.SUCCESS
                        : CommandOutcome.Status.REJECTED,
                message
        );
    }

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
            Map<String, ?> placeholders
    ) {
        return success(LocalizedMessage.of(key, placeholders));
    }

    public static MessageResult failure(String key) {
        return failure(LocalizedMessage.of(key));
    }

    public static MessageResult failure(LocalizedMessage message) {
        return new MessageResult(CommandOutcome.Status.REJECTED, message);
    }

    public static MessageResult failure(
            String key,
            Map<String, ?> placeholders
    ) {
        return failure(LocalizedMessage.of(key, placeholders));
    }

    public static MessageResult failed(String key) {
        return failed(LocalizedMessage.of(key));
    }

    public static MessageResult failed(LocalizedMessage message) {
        return new MessageResult(CommandOutcome.Status.FAILED, message);
    }

    public static MessageResult failed(
            String key,
            Map<String, ?> placeholders
    ) {
        return failed(LocalizedMessage.of(key, placeholders));
    }

    public boolean success() {
        return status == CommandOutcome.Status.SUCCESS;
    }

}
