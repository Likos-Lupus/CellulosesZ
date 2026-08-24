package top.likoslupus.cellulosesz.common.playerstate;

import static top.likoslupus.cellulosesz.api.validation.Checks.requireNonNegative;

public record ExperienceSnapshot(
        int totalPoints,
        int level,
        double progress,
        int pointsToNextLevel
) {

    public ExperienceSnapshot {
        requireNonNegative(totalPoints, "totalPoints");
        requireNonNegative(level, "level");
        if (!Double.isFinite(progress) || progress < 0.0D || progress >= 1.000001D) {
            throw new IllegalArgumentException("progress must be finite and between zero and one");
        }
        requireNonNegative(pointsToNextLevel, "pointsToNextLevel");
    }

}
