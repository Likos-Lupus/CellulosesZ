package top.likoslupus.cellulosesz.api.playerstate;

import static java.util.Objects.requireNonNull;

public record ExperienceRequest(
        ExperienceAction action,
        ExperienceUnit unit,
        int amount
) {

    public ExperienceRequest {
        requireNonNull(action, "action");
        requireNonNull(unit, "unit");
        if (amount < 0) {
            throw new IllegalArgumentException("amount must not be negative");
        }
        if (action == ExperienceAction.RESET && amount != 0) {
            throw new IllegalArgumentException("reset amount must be zero");
        }
    }

}
