package top.likoslupus.cellulosesz.api.item;

import top.likoslupus.cellulosesz.api.validation.Checks;

import java.util.Optional;

import static top.likoslupus.cellulosesz.api.validation.Checks.requireNonNegative;
import static top.likoslupus.cellulosesz.api.validation.Checks.requirePositive;

public record PotionItemRequest(
        Optional<String> effectId,
        int durationSeconds,
        int amplifier
) {

    public PotionItemRequest {
        effectId = effectId.map(value -> Checks.requireNonBlank(value, "effectId").trim());
        requireNonNegative(durationSeconds, "durationSeconds");
        requireNonNegative(amplifier, "amplifier");
        if (effectId.isEmpty() && (durationSeconds != 0 || amplifier != 0)) {
            throw new IllegalArgumentException("clear potion request must use zero duration and amplifier");
        }
    }

    public static PotionItemRequest clear() {
        return new PotionItemRequest(Optional.empty(), 0, 0);
    }

    public static PotionItemRequest apply(
            String effectId,
            int durationSeconds,
            int amplifier
    ) {
        return new PotionItemRequest(
                Optional.of(Checks.requireNonBlank(effectId, "effectId")),
                requirePositive(durationSeconds, "durationSeconds"),
                amplifier
        );
    }

}
