package top.likoslupus.cellulosesz.api.admin;

import top.likoslupus.cellulosesz.api.text.LocalizedMessage;

import java.util.Map;

import static java.util.Objects.requireNonNull;

public record AdminResult(
        AdminStatus status,
        LocalizedMessage message
) {

    public AdminResult {
        requireNonNull(status, "status");
        requireNonNull(message, "message");
    }

    public static AdminResult success(LocalizedMessage message) {
        return new AdminResult(AdminStatus.SUCCESS, message);
    }

    public static AdminResult success(String key) {
        return new AdminResult(AdminStatus.SUCCESS, LocalizedMessage.of(key));
    }

    public static AdminResult success(String key, Map<String, ?> placeholders) {
        return new AdminResult(AdminStatus.SUCCESS, LocalizedMessage.of(key, placeholders));
    }

    public static AdminResult partial(String key, Map<String, ?> placeholders) {
        return new AdminResult(AdminStatus.PARTIAL_SUCCESS, LocalizedMessage.of(key, placeholders));
    }

    public static AdminResult failure(LocalizedMessage message) {
        return new AdminResult(AdminStatus.FAILURE, message);
    }

    public static AdminResult failure(String key) {
        return new AdminResult(AdminStatus.FAILURE, LocalizedMessage.of(key));
    }

    public static AdminResult failure(String key, Map<String, ?> placeholders) {
        return new AdminResult(AdminStatus.FAILURE, LocalizedMessage.of(key, placeholders));
    }

    public static AdminResult failure(AdminStatus status, String key) {
        if (status.successful()) throw new IllegalArgumentException("Failure status must not be successful");
        return new AdminResult(status, LocalizedMessage.of(key));
    }

    public static AdminResult failure(
            AdminStatus status,
            String key,
            Map<String, ?> placeholders
    ) {
        if (status.successful()) throw new IllegalArgumentException("Failure status must not be successful");
        return new AdminResult(status, LocalizedMessage.of(key, placeholders));
    }

    public boolean success() {
        return status.successful();
    }

}
