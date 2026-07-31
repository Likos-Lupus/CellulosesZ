package top.likoslupus.cellulosesz.modules.playerstate.application;

import top.likoslupus.cellulosesz.modules.playerstate.config.PlayerStateConfig;

import static java.util.Objects.requireNonNull;
import static top.likoslupus.cellulosesz.api.validation.Checks.*;

public record PlayerStateCommandSettings(
        int defaultNearRadius,
        int maximumNearRadius,
        int maximumNearResults,
        double minimumSpeed,
        double maximumSpeed,
        int maximumPingLength,
        int maximumRealNameResults,
        long autoAfkMillis,
        long afkKickMillis,
        long activityCheckTicks,
        boolean persistAfk,
        boolean persistFlyGod,
        boolean persistVanish,
        boolean persistPersonalTimeWeather
) {

    public PlayerStateCommandSettings {
        requireFalse(defaultNearRadius <= 0 || maximumNearRadius < defaultNearRadius, "near radius configuration is invalid");
        requirePositive(maximumNearResults, "maximumNearResults");
        requireFinite(minimumSpeed, "minimumSpeed");
        requireFinite(maximumSpeed, "maximumSpeed");
        requirePositive(minimumSpeed, "minimumSpeed");
        requireLessThan(minimumSpeed, maximumSpeed, "minimumSpeed");
        requirePositive(maximumPingLength, "maximumPingLength");
        requirePositive(maximumRealNameResults, "maximumRealNameResults");
        requireNonNegative(autoAfkMillis, "autoAfkMillis");
        requireNonNegative(afkKickMillis, "afkKickMillis");
        requirePositive(activityCheckTicks, "activityCheckTicks");
    }

    public static PlayerStateCommandSettings from(PlayerStateConfig config) {
        requireNonNull(config, "config");
        return new PlayerStateCommandSettings(
                config.nearRadius,
                config.maximumNearRadius,
                config.maximumNearResults,
                config.minimumSpeed,
                config.maximumSpeed,
                config.maximumPingLength,
                config.maximumRealNameResults,
                Math.multiplyExact(config.autoAfkSeconds, 1000L),
                Math.multiplyExact(config.afkKickSeconds, 1000L),
                Math.multiplyExact(config.activityCheckSeconds, 20L),
                config.persistAfk,
                config.persistFlyGod,
                config.persistVanish,
                config.persistPersonalTimeWeather
        );
    }

}
