package top.likoslupus.cellulosesz.common.world;

import java.util.List;
import java.util.OptionalDouble;

import static top.likoslupus.cellulosesz.api.validation.Checks.requireNonNegative;

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
        requireNonNegative(uptimeMillis, "uptimeMillis");
        requireNonNegative(usedMemoryBytes, "usedMemoryBytes");
        requireNonNegative(allocatedMemoryBytes, "allocatedMemoryBytes");
        requireNonNegative(maximumMemoryBytes, "maximumMemoryBytes");
        requireNonNegative(availableMemoryBytes, "availableMemoryBytes");
        requireNonNegative(ticksPerSecond, "ticksPerSecond");
        requireNonNull(averageTickMillis, "averageTickMillis");
        worlds = List.copyOf(requireNonNull(worlds, "worlds"));
    }

}
