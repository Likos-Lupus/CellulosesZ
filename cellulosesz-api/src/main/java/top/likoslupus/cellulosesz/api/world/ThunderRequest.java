package top.likoslupus.cellulosesz.api.world;

public record ThunderRequest(
        boolean enabled,
        int durationTicks
) {

    public ThunderRequest {
        if (durationTicks < 1) {
            throw new IllegalArgumentException("durationTicks must be positive");
        }
    }

}
