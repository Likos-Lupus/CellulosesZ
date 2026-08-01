package top.likoslupus.cellulosesz.api.user;

import java.util.Set;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

public record UserRelations(Set<UUID> ignored) {

    public UserRelations {
        ignored = Set.copyOf(requireNonNull(ignored, "ignored"));
    }

    public static UserRelations defaults() {
        return new UserRelations(Set.of());
    }

    public UserRelations withIgnored(Set<UUID> value) {
        return new UserRelations(value);
    }

}
