package top.likoslupus.cellulosesz.api.command.service;

import static top.likoslupus.cellulosesz.api.validation.TextChecks.requireNonBlank;

import static java.util.Objects.requireNonNull;

public record ConfirmationToken(String value) {

    public ConfirmationToken {
        value = requireNonBlank(requireNonNull(value, "value").trim(), "value");
    }

}
