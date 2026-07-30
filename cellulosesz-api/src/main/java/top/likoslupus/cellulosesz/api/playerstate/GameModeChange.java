package top.likoslupus.cellulosesz.api.playerstate;

import static java.util.Objects.requireNonNull;

public record GameModeChange(
        GameModeKind previous,
        GameModeKind current
) {

    public GameModeChange {
        requireNonNull(previous, "previous");
        requireNonNull(current, "current");
    }

}
