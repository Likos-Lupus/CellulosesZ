package top.likoslupus.cellulosesz.api.kit;

public record KitClaimResult(
        boolean success,
        String message
) {

    public static KitClaimResult success(String message) {
        return new KitClaimResult(true, message);
    }

    public static KitClaimResult failure(String message) {
        return new KitClaimResult(false, message);
    }

}
