package top.likoslupus.cellulosesz.api.messaging;

public record MessageResult(
        boolean success,
        String message
) {

    public static MessageResult success(String message) {
        return new MessageResult(true, message);
    }

    public static MessageResult failure(String message) {
        return new MessageResult(false, message);
    }

}
