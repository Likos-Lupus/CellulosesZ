package top.likoslupus.cellulosesz.api.teleport;

public record RandomTeleportSettings(
        double centerX,
        double centerZ,
        int minRadius,
        int maxRadius
) {

    public RandomTeleportSettings {
        if (!Double.isFinite(centerX) || !Double.isFinite(centerZ)) {
            throw new IllegalArgumentException("Random teleport center must be finite");
        }
        if (minRadius < 0 || maxRadius <= minRadius) {
            throw new IllegalArgumentException("Random teleport radii must satisfy 0 <= min < max");
        }
    }

}
