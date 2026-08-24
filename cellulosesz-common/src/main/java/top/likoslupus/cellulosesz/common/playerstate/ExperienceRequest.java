package top.likoslupus.cellulosesz.common.playerstate;

import static top.likoslupus.cellulosesz.api.validation.Checks.requireNonNegative;

import static java.util.Objects.requireNonNull;

public record ExperienceRequest(
        ExperienceAction action,
        ExperienceUnit unit,
        int amount
) {

    public ExperienceRequest {
        requireNonNull(action, "action");
        requireNonNull(unit, "unit");
        requireNonNegative(amount, "amount");
        if (action == ExperienceAction.RESET && amount != 0) {
            throw new IllegalArgumentException("reset amount must be zero");
        }
    }

}
