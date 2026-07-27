package top.likoslupus.cellulosesz.api.world;

import java.util.List;
import java.util.OptionalDouble;

import static java.util.Objects.requireNonNull;

public record ServerDiagnosticsSnapshot(
        long uptimeMillis,
        long usedMemoryBytes,
        long allocatedMemoryBytes,
        long maximumMemoryBytes,
        long availableMemoryBytes,
        double ticksPerSecond,
        OptionalDouble averageTickMillis,
        List<WorldDiagnostics> worlds
) {

    public ServerDiagnosticsSnapshot {
        if (uptimeMillis < 0L
                || usedMemoryBytes < 0L
                || allocatedMemoryBytes < 0L
                || maximumMemoryBytes < 0L
                || availableMemoryBytes < 0L
        ) {
            throw new IllegalArgumentException("diagnostic values must not be negative");
        }
        if (!Double.isFinite(ticksPerSecond) || ticksPerSecond < 0.0D) {
            throw new IllegalArgumentException("ticksPerSecond must be finite and non-negative");
        }
        requireNonNull(averageTickMillis, "averageTickMillis");
        worlds = List.copyOf(requireNonNull(worlds, "worlds"));
    }

}
