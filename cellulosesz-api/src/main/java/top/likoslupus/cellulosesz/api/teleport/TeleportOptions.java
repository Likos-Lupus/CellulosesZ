package top.likoslupus.cellulosesz.api.teleport;

import static top.likoslupus.cellulosesz.api.validation.NumericChecks.requireNonNegative;

public record TeleportOptions(
        boolean safe,
        boolean rememberBack,
        boolean allowCrossWorld,
        boolean keepVehicle,
        int warmupSeconds
) {

    public TeleportOptions {
        requireNonNegative(warmupSeconds, "warmupSeconds");
    }

    public static TeleportOptions defaults() {
        return new TeleportOptions(true, true, true, false, 0);
    }

    public TeleportOptions withoutBackMemory() {
        return new TeleportOptions(safe, false, allowCrossWorld, keepVehicle, warmupSeconds);
    }

    public TeleportOptions withWarmup(int seconds) {
        return new TeleportOptions(safe, rememberBack, allowCrossWorld, keepVehicle, seconds);
    }

    public TeleportOptions withSafe(boolean value) {
        return new TeleportOptions(
                value,
                rememberBack,
                allowCrossWorld,
                keepVehicle,
                warmupSeconds
        );
    }

}
