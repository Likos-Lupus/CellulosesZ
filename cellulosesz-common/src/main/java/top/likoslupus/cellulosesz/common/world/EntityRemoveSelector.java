package top.likoslupus.cellulosesz.common.world;

import top.likoslupus.cellulosesz.api.validation.Checks;

import java.util.Optional;

import static java.util.Objects.requireNonNull;

public record EntityRemoveSelector(
        Kind kind,
        Optional<String> entityId
) {

    public EntityRemoveSelector {
        requireNonNull(kind, "kind");
        entityId = requireNonNull(entityId, "entityId")
                .map(value -> Checks.requireNonBlank(value, "entityId").trim());
        if (kind == Kind.ENTITY_TYPE && entityId.isEmpty()) {
            throw new IllegalArgumentException("entityId is required for ENTITY_TYPE selector");
        }
        if (kind != Kind.ENTITY_TYPE && entityId.isPresent()) {
            throw new IllegalArgumentException("entityId is only valid for ENTITY_TYPE selector");
        }
    }

    public static EntityRemoveSelector of(Kind kind) {
        return new EntityRemoveSelector(kind, Optional.empty());
    }

    public static EntityRemoveSelector entity(String entityId) {
        return new EntityRemoveSelector(Kind.ENTITY_TYPE, Optional.of(entityId));
    }

    public enum Kind {

        ALL,
        ANIMALS,
        MONSTERS,
        ITEMS,
        PROJECTILES,
        BOATS,
        MINECARTS,
        ENTITY_TYPE

    }

}
