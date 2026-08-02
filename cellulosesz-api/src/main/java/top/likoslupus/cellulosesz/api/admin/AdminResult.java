package top.likoslupus.cellulosesz.api.admin;

import top.likoslupus.cellulosesz.api.text.LocalizedMessage;
import top.likoslupus.cellulosesz.api.text.MessageArguments;

import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Machine-readable administration outcome with an optional list of component results for compound
 * operations.
 */
public record AdminResult(
        AdminStatus status,
        LocalizedMessage message,
        List<AdminResult> components
) {

    public AdminResult(
            AdminStatus status,
            LocalizedMessage message
    ) {
        this(status, message, List.of());
    }

    public AdminResult {
        requireNonNull(status, "status");
        requireNonNull(message, "message");
        components = List.copyOf(requireNonNull(components, "components"));
    }

    public static AdminResult success(LocalizedMessage message) {
        return new AdminResult(AdminStatus.SUCCESS, message);
    }

    public static AdminResult success(String key) {
        return new AdminResult(AdminStatus.SUCCESS, LocalizedMessage.of(key));
    }

    public static AdminResult success(String key, MessageArguments placeholders) {
        return new AdminResult(AdminStatus.SUCCESS, LocalizedMessage.of(key, placeholders));
    }

    public static AdminResult success(
            String key,
            MessageArguments placeholders,
            List<AdminResult> components
    ) {
        return new AdminResult(
                AdminStatus.SUCCESS,
                LocalizedMessage.of(key, placeholders),
                components
        );
    }

    public static AdminResult partial(String key, MessageArguments placeholders) {
        return new AdminResult(AdminStatus.PARTIAL_SUCCESS, LocalizedMessage.of(key, placeholders));
    }

    public static AdminResult partial(
            String key,
            MessageArguments placeholders,
            List<AdminResult> components
    ) {
        if (components.isEmpty()) {
            throw new IllegalArgumentException(
                    "Compound partial result must contain component results"
            );
        }
        return new AdminResult(
                AdminStatus.PARTIAL_SUCCESS,
                LocalizedMessage.of(key, placeholders),
                components
        );
    }

    public static AdminResult failure(LocalizedMessage message) {
        return new AdminResult(AdminStatus.FAILURE, message);
    }

    public static AdminResult failure(String key) {
        return new AdminResult(AdminStatus.FAILURE, LocalizedMessage.of(key));
    }

    public static AdminResult failure(String key, MessageArguments placeholders) {
        return new AdminResult(AdminStatus.FAILURE, LocalizedMessage.of(key, placeholders));
    }

    public static AdminResult failure(AdminStatus status, String key) {
        if (status.successful()) {
            throw new IllegalArgumentException("Failure status must not be successful");
        }
        return new AdminResult(status, LocalizedMessage.of(key));
    }

    public static AdminResult failure(
            AdminStatus status,
            String key,
            MessageArguments placeholders
    ) {
        if (status.successful()) {
            throw new IllegalArgumentException("Failure status must not be successful");
        }
        return new AdminResult(status, LocalizedMessage.of(key, placeholders));
    }

    public static AdminResult failure(
            AdminStatus status,
            String key,
            MessageArguments placeholders,
            List<AdminResult> components
    ) {
        if (status.successful()) {
            throw new IllegalArgumentException("Failure status must not be successful");
        }
        if (components.isEmpty()) {
            throw new IllegalArgumentException("Compound failure must contain component results");
        }
        return new AdminResult(status, LocalizedMessage.of(key, placeholders), components);
    }

    public boolean success() {
        return status.successful();
    }

}
