package top.likoslupus.cellulosesz.common.playerstate;

/**
 * Exact vanilla total-experience curve helpers with overflow checks.
 */
public final class ExperienceMath {

    private ExperienceMath() {
    }

    public static int pointsToNextLevel(int level) {
        if (level < 0) {
            throw new IllegalArgumentException("level must not be negative");
        }
        if (level >= 30) {
            return Math.addExact(Math.multiplyExact(9, level), -158);
        }
        if (level >= 15) {
            return Math.addExact(Math.multiplyExact(5, level), -38);
        }
        return Math.addExact(Math.multiplyExact(2, level), 7);
    }

    public static int levelForTotal(int total) {
        if (total < 0) {
            throw new IllegalArgumentException("total must not be negative");
        }
        var low = 0;
        var high = maximumLevel();
        while (low < high) {
            var middle = low + (high - low + 1) / 2;
            if (totalForLevel(middle) <= total) {
                low = middle;
            } else {
                high = middle - 1;
            }
        }
        return low;
    }

    public static int maximumLevel() {
        var low = 0;
        var high = 1_000_000;
        while (low < high) {
            var middle = low + (high - low + 1) / 2;
            try {
                totalForLevel(middle);
                low = middle;
            } catch (IllegalArgumentException _) {
                high = middle - 1;
            }
        }
        return low;
    }

    public static int totalForLevel(int level) {
        if (level < 0) {
            throw new IllegalArgumentException("level must not be negative");
        }
        long result;
        if (level <= 16) {
            result = (long) level * level + 6L * level;
        } else if (level <= 31) {
            result = (5L * level * level - 81L * level + 720L) / 2L;
        } else {
            result = (9L * level * level - 325L * level + 4440L) / 2L;
        }
        if (result > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("level is too large");
        }
        return (int) result;
    }

}
