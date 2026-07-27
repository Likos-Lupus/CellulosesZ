package top.likoslupus.cellulosesz.api.item;

import static java.util.Objects.requireNonNull;

public record BookMutationResult(
        BookAction action,
        BookDetails details
) {

    public BookMutationResult {
        requireNonNull(action, "action");
        requireNonNull(details, "details");
    }

}
