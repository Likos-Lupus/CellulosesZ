package top.likoslupus.cellulosesz.api.sign;

public record SignUseResult(
        boolean handled,
        boolean success,
        String message
) {

    public static SignUseResult pass() {
        return new SignUseResult(false, true, "");
    }

    public static SignUseResult success(String message) {
        return new SignUseResult(true, true, message);
    }

    public static SignUseResult failure(String message) {
        return new SignUseResult(true, false, message);
    }

}
