package top.likoslupus.cellulosesz.api.playerstate;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class ExperienceMathTest {

    @Test
    void totalExperienceCurveMatchesVanillaBreakpoints() {
        assertEquals(0, ExperienceMath.totalForLevel(0));
        assertEquals(7, ExperienceMath.totalForLevel(1));
        assertEquals(352, ExperienceMath.totalForLevel(16));
        assertEquals(394, ExperienceMath.totalForLevel(17));
        assertEquals(1507, ExperienceMath.totalForLevel(31));
        assertEquals(1628, ExperienceMath.totalForLevel(32));
    }

    @Test
    void inverseCurveReturnsHighestCompletedLevel() {
        assertEquals(0, ExperienceMath.levelForTotal(0));
        assertEquals(0, ExperienceMath.levelForTotal(6));
        assertEquals(1, ExperienceMath.levelForTotal(7));
        assertEquals(30, ExperienceMath.levelForTotal(ExperienceMath.totalForLevel(30)));
        assertEquals(30, ExperienceMath.levelForTotal(ExperienceMath.totalForLevel(31) - 1));
    }

    @Test
    void curveRejectsNegativeAndOverflowingValues() {
        assertThrows(IllegalArgumentException.class, () -> ExperienceMath.totalForLevel(-1));
        assertThrows(IllegalArgumentException.class, () -> ExperienceMath.levelForTotal(-1));
        assertThrows(IllegalArgumentException.class, () -> ExperienceMath.totalForLevel(ExperienceMath.maximumLevel() + 1));
    }

}
