package top.likoslupus.cellulosesz.api.admin;

public record AdminResult(
        boolean success,
        String message
) {

    public static AdminResult success(String message) {
        return new AdminResult(true, message);
    }

    public static AdminResult failure(String message) {
        return new AdminResult(false, message);
    }

}
