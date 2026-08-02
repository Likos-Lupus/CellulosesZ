package top.likoslupus.cellulosesz.common.player;

import java.util.UUID;

/** Raised only when a stable player identity cannot be resolved to a live server player. */
public final class MinecraftPlayerUnavailableException extends IllegalStateException {

    public MinecraftPlayerUnavailableException(UUID playerId) {
        super("Player is no longer online: " + playerId);
    }

}
