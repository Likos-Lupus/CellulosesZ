package top.likoslupus.cellulosesz.common.playerstate;

import top.likoslupus.cellulosesz.api.playerstate.GameModeKind;

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
