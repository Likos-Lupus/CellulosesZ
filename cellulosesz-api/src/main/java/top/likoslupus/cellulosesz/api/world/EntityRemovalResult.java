package top.likoslupus.cellulosesz.api.world;

import static top.likoslupus.cellulosesz.api.validation.Checks.*;

public record EntityRemovalResult(
        int matched,
        int removed,
        int failed
) {

    public EntityRemovalResult {
        requireNonNegative(matched, "matched");
        requireInRange(removed, 0, matched, "removed");
        requireInRange(failed, 0, matched, "failed");
        requireFalse(removed + failed > matched, "removed and failed must not exceed matched");
    }

}
