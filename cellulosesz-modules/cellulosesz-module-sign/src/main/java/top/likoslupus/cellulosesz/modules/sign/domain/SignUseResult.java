package top.likoslupus.cellulosesz.modules.sign.domain;

import top.likoslupus.cellulosesz.api.text.LocalizedMessage;
import top.likoslupus.cellulosesz.api.text.MessageArguments;

import java.util.Optional;
import org.jspecify.annotations.Nullable;

public record SignUseResult(
        boolean handled,
        boolean success,
        @Nullable LocalizedMessage message
) {

    public static SignUseResult pass() {
        return new SignUseResult(false, true, null);
    }

    public static SignUseResult success(LocalizedMessage message) {
        return new SignUseResult(true, true, message);
    }

    public static SignUseResult success(String key) {
        return new SignUseResult(true, true, LocalizedMessage.of(key));
    }

    public static SignUseResult success(String key, MessageArguments arguments) {
        return new SignUseResult(true, true, LocalizedMessage.of(key, arguments));
    }

    public static SignUseResult failure(LocalizedMessage message) {
        return new SignUseResult(true, false, message);
    }

    public static SignUseResult failure(String key) {
        return new SignUseResult(true, false, LocalizedMessage.of(key));
    }

    public static SignUseResult failure(String key, MessageArguments arguments) {
        return new SignUseResult(true, false, LocalizedMessage.of(key, arguments));
    }

    public Optional<LocalizedMessage> optionalMessage() {
        return Optional.ofNullable(message);
    }

}
