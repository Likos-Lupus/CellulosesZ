package top.likoslupus.cellulosesz.modules.sign;

import static java.util.Objects.requireNonNull;

/** Atomically published immutable edit-sign command settings. */
public final class SignRuntimeSettings {

    private volatile Snapshot snapshot;

    public SignRuntimeSettings(SignConfig config) {
        snapshot = Snapshot.from(config);
    }

    public void configure(SignConfig config) {
        snapshot = Snapshot.from(config);
    }

    public int editTargetDistance() {
        return snapshot.editTargetDistance();
    }

    public int editMaximumLineLength() {
        return snapshot.editMaximumLineLength();
    }

    private record Snapshot(
            int editTargetDistance,
            int editMaximumLineLength
    ) {

        private static Snapshot from(SignConfig source) {
            var config = requireNonNull(source, "config").validatedCopy();
            return new Snapshot(config.editTargetDistance, config.editMaximumLineLength);
        }

    }

}
