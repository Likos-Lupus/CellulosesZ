package top.likoslupus.cellulosesz.api.teleport;

public record TeleportResult(
        boolean success,
        String message,
        CellLocation location
) {

    public static TeleportResult success(CellLocation location) {
        return new TeleportResult(true, "teleported", location);
    }

    public static TeleportResult failed(String message, CellLocation location) {
        return new TeleportResult(false, message, location);
    }

}
