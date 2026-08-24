package top.likoslupus.cellulosesz.common.admin;

import java.util.UUID;

import static top.likoslupus.cellulosesz.api.validation.Checks.*;

import static java.util.Objects.requireNonNull;

public record PlayerProfileId(
        UUID uuid,
        String name
) {

    public PlayerProfileId {
        requireNonNull(uuid, "uuid");
        name = requireNonBlank(name, "name");
        requireMaxLength(name, 16, "name");
        requireNoControlCharacters(name, "name");
    }

}
