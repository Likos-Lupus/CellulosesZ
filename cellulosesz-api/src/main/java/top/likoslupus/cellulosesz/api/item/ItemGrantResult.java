package top.likoslupus.cellulosesz.api.item;

import static top.likoslupus.cellulosesz.api.validation.Checks.requireInRange;
import static top.likoslupus.cellulosesz.api.validation.Checks.requirePositive;

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
