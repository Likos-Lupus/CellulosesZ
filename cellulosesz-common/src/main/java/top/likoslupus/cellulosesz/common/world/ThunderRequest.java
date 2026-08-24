package top.likoslupus.cellulosesz.common.world;

import static top.likoslupus.cellulosesz.api.validation.Checks.requirePositive;

public record ThunderRequest(
        boolean enabled,
        int durationTicks
) {

    public ThunderRequest {
        requirePositive(durationTicks, "durationTicks");
    }

}
