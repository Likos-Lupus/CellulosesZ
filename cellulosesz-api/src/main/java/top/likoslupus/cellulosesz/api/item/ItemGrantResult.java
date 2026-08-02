package top.likoslupus.cellulosesz.api.item;

import static top.likoslupus.cellulosesz.api.validation.NumericChecks.requirePositive;
import static top.likoslupus.cellulosesz.api.validation.RangeChecks.requireInRange;

public record ItemGrantResult(
        int requested,
        int granted
) {

    public ItemGrantResult {
        requirePositive(requested, "requested");
        requireInRange(granted, 0, requested, "granted");
    }

    public boolean complete() {
        return requested == granted;
    }

}
