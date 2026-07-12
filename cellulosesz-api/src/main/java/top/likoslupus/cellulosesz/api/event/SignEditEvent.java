package top.likoslupus.cellulosesz.api.event;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.teleport.CellLocation;

import java.util.List;
import java.util.Objects;

public final class SignEditEvent extends AbstractCancellableEvent {

    private final CellPlayer player;
    private final CellLocation location;
    private final boolean front;
    private final List<String> previousLines;
    private List<String> lines;

    public SignEditEvent(
            CellPlayer player,
            CellLocation location,
            boolean front,
            List<String> previousLines,
            List<String> lines
    ) {
        this.player = Objects.requireNonNull(player, "player");
        this.location = Objects.requireNonNull(location, "location");
        this.front = front;
        this.previousLines = List.copyOf(Objects.requireNonNull(previousLines, "previousLines"));
        lines(lines);
    }

    public void lines(List<String> lines) {
        this.lines = List.copyOf(Objects.requireNonNull(lines, "lines"));
    }

    public CellPlayer player() {
        return player;
    }

    public CellLocation location() {
        return location;
    }

    public boolean front() {
        return front;
    }

    public List<String> previousLines() {
        return previousLines;
    }

    public List<String> lines() {
        return lines;
    }

}
