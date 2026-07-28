package top.likoslupus.cellulosesz.api.platform.admin;

import java.util.UUID;

import static java.util.Objects.requireNonNull;
import static top.likoslupus.cellulosesz.api.validation.Checks.*;

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
