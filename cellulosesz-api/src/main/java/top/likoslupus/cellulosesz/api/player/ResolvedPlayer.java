package top.likoslupus.cellulosesz.api.player;

import org.jspecify.annotations.Nullable;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;

import java.util.Optional;
import java.util.UUID;

public record ResolvedPlayer(
        ResolvedPlayerState state,
        @Nullable UUID uuid,
        String name,
        @Nullable CellPlayer onlinePlayer,
        boolean vanished
) {

    public Optional<UUID> optionalUuid() {
        return Optional.ofNullable(uuid);
    }

    public Optional<CellPlayer> online() {
        return Optional.ofNullable(onlinePlayer);
    }

}
