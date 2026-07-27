package top.likoslupus.cellulosesz.api.item;

import java.util.Optional;

import static java.util.Objects.requireNonNull;

public record HatResult(
        Optional<String> previousHelmet,
        Optional<String> currentHelmet
) {

    public HatResult {
        requireNonNull(previousHelmet, "previousHelmet");
        requireNonNull(currentHelmet, "currentHelmet");
    }

}
