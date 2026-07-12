package top.likoslupus.cellulosesz.api.teleport;

import top.likoslupus.cellulosesz.api.text.LocalizedMessage;

import java.util.Map;

public record TeleportResult(
        boolean success,
        LocalizedMessage message,
        CellLocation location
) {

    public static TeleportResult success(CellLocation location) {
        return new TeleportResult(true, LocalizedMessage.of("service.teleport.success"), location);
    }

    public static TeleportResult failed(String key, CellLocation location) {
        return new TeleportResult(false, LocalizedMessage.of(key), location);
    }

    public static TeleportResult failed(String key, Map<String, ?> placeholders, CellLocation location) {
        return new TeleportResult(false, LocalizedMessage.of(key, placeholders), location);
    }

}
