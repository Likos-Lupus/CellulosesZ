package top.likoslupus.cellulosesz.common.item;

import static java.util.Objects.requireNonNull;

public record BookRequest(
        BookAction action,
        String value,
        String actingPlayerName
) {

    public BookRequest {
        requireNonNull(action, "action");
        value = requireNonNull(value, "value");
        actingPlayerName = requireNonNull(actingPlayerName, "actingPlayerName");
    }

}
