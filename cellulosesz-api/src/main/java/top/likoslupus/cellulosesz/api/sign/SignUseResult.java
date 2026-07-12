package top.likoslupus.cellulosesz.api.sign;

import org.jspecify.annotations.Nullable;
import top.likoslupus.cellulosesz.api.text.LocalizedMessage;

import java.util.Map;
import java.util.Optional;

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

    public static SignUseResult success(String key, Map<String, ?> placeholders) {
        return new SignUseResult(true, true, LocalizedMessage.of(key, placeholders));
    }

    public static SignUseResult failure(LocalizedMessage message) {
        return new SignUseResult(true, false, message);
    }

    public static SignUseResult failure(String key) {
        return new SignUseResult(true, false, LocalizedMessage.of(key));
    }

    public static SignUseResult failure(String key, Map<String, ?> placeholders) {
        return new SignUseResult(true, false, LocalizedMessage.of(key, placeholders));
    }

    public Optional<LocalizedMessage> optionalMessage() {
        return Optional.ofNullable(message);
    }

}
