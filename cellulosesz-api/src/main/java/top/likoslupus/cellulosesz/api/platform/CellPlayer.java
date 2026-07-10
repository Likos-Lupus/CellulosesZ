package top.likoslupus.cellulosesz.api.platform;

import java.util.UUID;

public record CellPlayer(
        UUID uuid,
        String name,
        Object nativeHandle
) {

}
