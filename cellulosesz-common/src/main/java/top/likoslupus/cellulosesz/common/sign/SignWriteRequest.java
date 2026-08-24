package top.likoslupus.cellulosesz.common.sign;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.teleport.CellLocation;

import java.util.List;

import static top.likoslupus.cellulosesz.api.validation.Checks.requireInRange;

import static java.util.Objects.requireNonNull;

public record SignWriteRequest(
        CellPlayer actor,
        CellLocation location,
        boolean front,
        List<String> expectedLines,
        List<String> replacementLines,
        boolean allowWaxed
) {

    public SignWriteRequest {
        requireNonNull(actor, "actor");
        requireNonNull(location, "location");
        expectedLines = List.copyOf(expectedLines);
        replacementLines = List.copyOf(replacementLines);
        requireInRange(expectedLines.size(), 4, 4, "expectedLines.size");
        requireInRange(replacementLines.size(), 4, 4, "replacementLines.size");
    }

}
