package top.likoslupus.cellulosesz.api.playerstate;

public record ExperienceSnapshot(
        int totalPoints,
        int level,
        double progress,
        int pointsToNextLevel
) {

    public ExperienceSnapshot {
        if (totalPoints < 0) {
            throw new IllegalArgumentException("totalPoints must not be negative");
        }
        if (level < 0) {
            throw new IllegalArgumentException("level must not be negative");
        }
        if (!Double.isFinite(progress) || progress < 0.0D || progress >= 1.000001D) {
            throw new IllegalArgumentException("progress must be finite and between zero and one");
        }
        if (pointsToNextLevel < 0) {
            throw new IllegalArgumentException("pointsToNextLevel must not be negative");
        }
    }

}
