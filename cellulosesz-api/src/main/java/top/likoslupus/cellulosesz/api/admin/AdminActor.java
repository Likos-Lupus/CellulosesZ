package top.likoslupus.cellulosesz.api.admin;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;

import java.util.Optional;
import java.util.UUID;

import static java.util.Objects.requireNonNull;
import static top.likoslupus.cellulosesz.api.validation.Checks.*;

/**
 * Stable audit identity for a player or non-player command source.
 */
public record AdminActor(
        Optional<UUID> uuid,
        String name
) {

    public AdminActor {
        requireNonNull(uuid, "uuid");
        name = requireNonBlank(name, "name").trim();
        requireMaxLength(name, 64, "name");
        requireNoControlCharacters(name, "name");
    }

    public static AdminActor player(CellPlayer player) {
        requireNonNull(player, "player");
        return new AdminActor(Optional.of(player.uuid()), player.name());
    }

    public static AdminActor console(String name) {
        return new AdminActor(Optional.empty(), name);
    }

}
