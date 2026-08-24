package top.likoslupus.cellulosesz.core.command.service;

import static top.likoslupus.cellulosesz.api.validation.Checks.requireNonBlank;

import static java.util.Objects.requireNonNull;

/** Stable typed key for a confirmation payload. */
public record ConfirmationKey<T>(
        String id,
        Class<T> payloadType
) {

    public ConfirmationKey {
        id = requireNonBlank(requireNonNull(id, "id").trim(), "id");
        requireNonNull(payloadType, "payloadType");
    }

}
