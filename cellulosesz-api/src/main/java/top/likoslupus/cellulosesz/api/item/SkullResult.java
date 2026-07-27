package top.likoslupus.cellulosesz.api.item;

import static java.util.Objects.requireNonNull;

public record SkullResult(
        String owner,
        String recipient,
        boolean spawned
) {

    public SkullResult {
        owner = requireNonNull(owner, "owner");
        recipient = requireNonNull(recipient, "recipient");
    }

}
