package top.likoslupus.cellulosesz.api.world;

import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public record WorldResolution(
        Status status,
        Optional<String> worldId,
        List<String> candidates
) {

    public WorldResolution {
        requireNonNull(status, "status");
        requireNonNull(worldId, "worldId");
        candidates = List.copyOf(requireNonNull(candidates, "candidates"));
        if (status == Status.RESOLVED && worldId.isEmpty()) {
            throw new IllegalArgumentException("resolved world is required");
        }
        if (status != Status.RESOLVED && worldId.isPresent()) {
            throw new IllegalArgumentException("unresolved world must be empty");
        }
        if (status == Status.AMBIGUOUS && candidates.size() < 2) {
            throw new IllegalArgumentException("ambiguous resolution needs candidates");
        }
    }

    public static WorldResolution resolved(String id) {
        return new WorldResolution(Status.RESOLVED, Optional.of(id), List.of(id));
    }

    public static WorldResolution notFound() {
        return new WorldResolution(Status.NOT_FOUND, Optional.empty(), List.of());
    }

    public static WorldResolution ambiguous(List<String> candidates) {
        return new WorldResolution(Status.AMBIGUOUS, Optional.empty(), candidates);
    }

    public enum Status {
        RESOLVED,
        NOT_FOUND,
        AMBIGUOUS
    }

}
