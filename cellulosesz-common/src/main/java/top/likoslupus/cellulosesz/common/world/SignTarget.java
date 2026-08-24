package top.likoslupus.cellulosesz.common.world;

import top.likoslupus.cellulosesz.api.teleport.CellLocation;

import java.util.List;

import static java.util.Objects.requireNonNull;

public record SignTarget(
        CellLocation location,
        boolean front,
        List<String> lines,
        boolean waxed
) {

    public SignTarget {
        requireNonNull(location, "location");
        lines = List.copyOf(requireNonNull(lines, "lines"));
        if (lines.size() != 4) {
            throw new IllegalArgumentException("sign must contain exactly four lines");
        }
    }

}
