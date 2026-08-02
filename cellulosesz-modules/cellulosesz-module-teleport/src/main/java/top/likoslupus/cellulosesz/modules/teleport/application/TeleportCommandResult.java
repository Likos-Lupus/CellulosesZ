package top.likoslupus.cellulosesz.modules.teleport.application;

import top.likoslupus.cellulosesz.api.text.LocalizedMessage;
import top.likoslupus.cellulosesz.api.text.MessageArguments;


import static java.util.Objects.requireNonNull;

public record TeleportCommandResult(
        TeleportCommandStatus status,
        LocalizedMessage message
) {

    public TeleportCommandResult {
        requireNonNull(status, "status");
        requireNonNull(message, "message");
    }

    public static TeleportCommandResult success(String key) {
        return new TeleportCommandResult(
                TeleportCommandStatus.SUCCESS,
                LocalizedMessage.of(key)
        );
    }

    public static TeleportCommandResult success(
            String key,
            MessageArguments values
    ) {
        return new TeleportCommandResult(
                TeleportCommandStatus.SUCCESS,
                LocalizedMessage.of(key, values)
        );
    }

    public static TeleportCommandResult partial(
            String key,
            MessageArguments values
    ) {
        return new TeleportCommandResult(
                TeleportCommandStatus.PARTIAL_SUCCESS,
                LocalizedMessage.of(key, values)
        );
    }

    public static TeleportCommandResult failure(
            TeleportCommandStatus status,
            String key
    ) {
        return new TeleportCommandResult(
                status,
                LocalizedMessage.of(key)
        );
    }

    public static TeleportCommandResult failure(
            TeleportCommandStatus status,
            String key,
            MessageArguments values
    ) {
        return new TeleportCommandResult(
                status,
                LocalizedMessage.of(key, values)
        );
    }

    public boolean success() {
        return status == TeleportCommandStatus.SUCCESS
                || status == TeleportCommandStatus.PARTIAL_SUCCESS;
    }

}
