package top.likoslupus.cellulosesz.modules.teleport;

import static java.util.Objects.requireNonNull;

/** Atomically published immutable settings for long-lived teleport services. */
public final class TeleportRuntimeSettings {

    private volatile Snapshot snapshot;

    public TeleportRuntimeSettings(TeleportConfig config) {
        snapshot = Snapshot.from(config);
    }

    public void configure(TeleportConfig config) {
        snapshot = Snapshot.from(config);
    }

    public int warmupSeconds() {
        return snapshot.warmupSeconds();
    }

    public int requestTimeoutSeconds() {
        return snapshot.requestTimeoutSeconds();
    }

    public int requestMaximumBulkTargets() {
        return snapshot.requestMaximumBulkTargets();
    }

    public int randomTeleportAttempts() {
        return snapshot.randomTeleportAttempts();
    }

    public int maximumBulkTargets() {
        return snapshot.maximumBulkTargets();
    }

    public int maximumJumpDistance() {
        return snapshot.maximumJumpDistance();
    }

    private record Snapshot(
            int warmupSeconds,
            int requestTimeoutSeconds,
            int requestMaximumBulkTargets,
            int randomTeleportAttempts,
            int maximumBulkTargets,
            int maximumJumpDistance
    ) {

        private static Snapshot from(TeleportConfig source) {
            var config = requireNonNull(source, "config").validatedCopy();
            return new Snapshot(
                    config.warmup.defaultSeconds,
                    config.requests.timeoutSeconds,
                    config.requests.maximumBulkTargets,
                    config.randomTeleport.attempts,
                    config.maximumBulkTargets,
                    config.maximumJumpDistance
            );
        }

    }

}
