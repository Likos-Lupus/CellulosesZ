package top.likoslupus.cellulosesz.core.command.service;

import static top.likoslupus.cellulosesz.api.validation.Checks.requireNonBlank;

import static java.util.Objects.requireNonNull;

public record ConfirmationToken(String value) {

    public ConfirmationToken {
        value = requireNonBlank(requireNonNull(value, "value").trim(), "value");
    }

}
