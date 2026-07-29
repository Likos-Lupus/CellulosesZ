package top.likoslupus.cellulosesz.modules.kit.application;

import top.likoslupus.cellulosesz.api.validation.Checks;

public sealed interface KitCooldown permits
        KitCooldown.Once,
        KitCooldown.Seconds {

    record Once() implements KitCooldown {

    }

    record Seconds(long value) implements KitCooldown {

        public Seconds {
            value = Checks.requireNonNegative(value, "value");
        }

    }

}
