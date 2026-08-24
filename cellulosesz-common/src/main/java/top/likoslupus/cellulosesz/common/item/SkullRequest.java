package top.likoslupus.cellulosesz.common.item;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;

import java.util.Optional;

import static java.util.Objects.requireNonNull;

public record SkullRequest(
        String owner,
        CellPlayer recipient,
        boolean spawn,
        Optional<InventoryItemSnapshot> expectedHeld
) {

    public SkullRequest {
        owner = requireNonNull(owner, "owner").trim();
        if (owner.isEmpty()) {
            throw new IllegalArgumentException("owner must not be blank");
        }
        requireNonNull(recipient, "recipient");
        requireNonNull(expectedHeld, "expectedHeld");
        if (spawn && expectedHeld.isPresent()) {
            throw new IllegalArgumentException("spawn request must not carry a held snapshot");
        }
        if (!spawn && expectedHeld.isEmpty()) {
            throw new IllegalArgumentException("modify request requires a held snapshot");
        }
    }

}
