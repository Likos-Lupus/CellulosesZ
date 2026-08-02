package top.likoslupus.cellulosesz.api.platform;

import java.util.UUID;

import static top.likoslupus.cellulosesz.api.validation.TextChecks.requireNonBlank;

import static java.util.Objects.requireNonNull;

/** Stable platform-neutral player identity. */
public record CellPlayer(
        UUID uuid,
        String name
) {

    public CellPlayer {
        requireNonNull(uuid, "uuid");
        name = requireNonBlank(requireNonNull(name, "name").trim(), "name");
    }

}
