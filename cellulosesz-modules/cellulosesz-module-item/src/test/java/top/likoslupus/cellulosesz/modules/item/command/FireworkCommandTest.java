package top.likoslupus.cellulosesz.modules.item.command;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class FireworkCommandTest {

    @Test
    void acceptsOnlyExactSupportedFlags() {
        assertEquals(new FireworkCommand.FireworkFlags(true, false), FireworkCommand.parseFlags("trail"));
        assertEquals(new FireworkCommand.FireworkFlags(false, true), FireworkCommand.parseFlags("twinkle"));
        assertEquals(new FireworkCommand.FireworkFlags(true, true), FireworkCommand.parseFlags("trail,twinkle"));
        assertEquals(new FireworkCommand.FireworkFlags(true, true), FireworkCommand.parseFlags("TWINKLE+TRAIL"));
    }

    @Test
    void rejectsSubstringAndMalformedFlags() {
        assertThrows(IllegalArgumentException.class, () -> FireworkCommand.parseFlags("notrail"));
        assertThrows(IllegalArgumentException.class, () -> FireworkCommand.parseFlags("trail,"));
        assertThrows(IllegalArgumentException.class, () -> FireworkCommand.parseFlags("sparkle"));
        assertThrows(IllegalArgumentException.class, () -> FireworkCommand.parseFlags(""));
    }

}
