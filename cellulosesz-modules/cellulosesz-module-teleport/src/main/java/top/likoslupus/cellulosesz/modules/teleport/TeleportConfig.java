package top.likoslupus.cellulosesz.modules.teleport;

import static top.likoslupus.cellulosesz.api.validation.NumericChecks.requireNonNegative;
import static top.likoslupus.cellulosesz.api.validation.NumericChecks.requirePositive;
import static top.likoslupus.cellulosesz.api.validation.RangeChecks.requireGreaterThan;


public final class TeleportConfig {

    public Warmup warmup = new Warmup();
    public Requests requests = new Requests();
    public RandomTeleport randomTeleport = new RandomTeleport();
    public int maximumBulkTargets = 200;
    public int maximumJumpDistance = 100;

    public TeleportConfig validatedCopy() {
        var copy = new TeleportConfig();

        copy.copyFrom(this);
        copy.validate();

        return copy;
    }

    public void copyFrom(TeleportConfig source) {
        source.validate();
        warmup.defaultSeconds = source.warmup.defaultSeconds;
        requests.timeoutSeconds = source.requests.timeoutSeconds;
        requests.maximumBulkTargets = source.requests.maximumBulkTargets;
        randomTeleport.minRadius = source.randomTeleport.minRadius;
        randomTeleport.maxRadius = source.randomTeleport.maxRadius;
        randomTeleport.attempts = source.randomTeleport.attempts;
        maximumBulkTargets = source.maximumBulkTargets;
        maximumJumpDistance = source.maximumJumpDistance;
    }

    private void validate() {
        requireNonNegative(warmup.defaultSeconds, "warmup.defaultSeconds");
        requirePositive(requests.timeoutSeconds, "requests.timeoutSeconds");
        requirePositive(requests.maximumBulkTargets, "requests.maximumBulkTargets");
        requireNonNegative(randomTeleport.minRadius, "randomTeleport.minRadius");
        requireGreaterThan(
                randomTeleport.maxRadius, "randomTeleport.maxRadius",
                randomTeleport.minRadius, "randomTeleport.minRadius"
        );
        requirePositive(randomTeleport.attempts, "randomTeleport.attempts");
        requirePositive(maximumBulkTargets, "maximumBulkTargets");
        requirePositive(maximumJumpDistance, "maximumJumpDistance");
    }

    public static final class Warmup {

        public int defaultSeconds = 0;

    }

    public static final class Requests {

        public int timeoutSeconds = 120;
        public int maximumBulkTargets = 200;

    }

    public static final class RandomTeleport {

        public int minRadius = 0;
        public int maxRadius = 1000;
        public int attempts = 24;

    }

}
