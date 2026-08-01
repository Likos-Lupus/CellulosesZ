package top.likoslupus.cellulosesz.api.sign;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.teleport.CellLocation;

import java.util.List;

import static java.util.Objects.requireNonNull;
import static top.likoslupus.cellulosesz.api.validation.Checks.requireInRange;

public record SignBreakRequest(
        CellPlayer actor,
        CellLocation location,
        List<String> expectedFrontLines,
        List<String> expectedBackLines
) {

    public SignBreakRequest {
        requireNonNull(actor, "actor");
        requireNonNull(location, "location");
        expectedFrontLines = List.copyOf(expectedFrontLines);
        expectedBackLines = List.copyOf(expectedBackLines);
        requireInRange(expectedFrontLines.size(), 4, 4, "expectedFrontLines.size");
        requireInRange(expectedBackLines.size(), 4, 4, "expectedBackLines.size");
    }

}
