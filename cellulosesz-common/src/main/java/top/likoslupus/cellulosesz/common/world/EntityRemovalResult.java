package top.likoslupus.cellulosesz.common.world;

import static top.likoslupus.cellulosesz.api.validation.Checks.requireInRange;
import static top.likoslupus.cellulosesz.api.validation.Checks.requireNonNegative;

public record EntityRemovalResult(
        int matched,
        int removed,
        int failed
) {

    public EntityRemovalResult {
        requireNonNegative(matched, "matched");
        requireInRange(removed, 0, matched, "removed");
        requireInRange(failed, 0, matched, "failed");
        if (removed + failed > matched) {
            throw new IllegalStateException("removed and failed must not exceed matched");
        }
    }

}
