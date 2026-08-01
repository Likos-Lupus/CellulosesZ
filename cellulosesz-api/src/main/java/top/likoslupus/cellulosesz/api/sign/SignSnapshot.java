package top.likoslupus.cellulosesz.api.sign;

import top.likoslupus.cellulosesz.api.teleport.CellLocation;

import java.util.List;

import static java.util.Objects.requireNonNull;
import static top.likoslupus.cellulosesz.api.validation.Checks.requireInRange;

public record SignSnapshot(
        CellLocation location,
        boolean front,
        boolean waxed,
        List<String> lines
) {

    public SignSnapshot {
        requireNonNull(location, "location");
        lines = List.copyOf(lines);
        requireInRange(lines.size(), 4, 4, "lines.size");
    }

}
