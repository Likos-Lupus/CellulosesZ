package top.likoslupus.cellulosesz.api.event;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.teleport.CellLocation;

import java.util.List;
import java.util.Objects;

public final class SignBreakEvent extends AbstractCancellableEvent {

    private final CellPlayer player;
    private final CellLocation location;
    private final List<String> frontLines;
    private final List<String> backLines;

    public SignBreakEvent(
            CellPlayer player,
            CellLocation location,
            List<String> frontLines,
            List<String> backLines
    ) {
        this.player = Objects.requireNonNull(player, "player");
        this.location = Objects.requireNonNull(location, "location");
        this.frontLines = List.copyOf(Objects.requireNonNull(frontLines, "frontLines"));
        this.backLines = List.copyOf(Objects.requireNonNull(backLines, "backLines"));
    }

    public CellPlayer player() {
        return player;
    }

    public CellLocation location() {
        return location;
    }

    public List<String> frontLines() {
        return frontLines;
    }

    public List<String> backLines() {
        return backLines;
    }

}
