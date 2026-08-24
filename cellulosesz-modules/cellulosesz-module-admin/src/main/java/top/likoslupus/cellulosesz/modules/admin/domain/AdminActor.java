package top.likoslupus.cellulosesz.modules.admin.domain;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;

import java.util.Optional;
import java.util.UUID;

import static top.likoslupus.cellulosesz.api.validation.Checks.*;

import static java.util.Objects.requireNonNull;

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
