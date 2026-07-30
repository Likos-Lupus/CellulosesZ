package top.likoslupus.cellulosesz.modules.playerstate.command.argument;

import com.mojang.brigadier.StringReader;
import org.junit.jupiter.api.Test;
import top.likoslupus.cellulosesz.api.playerstate.ExperienceUnit;
import top.likoslupus.cellulosesz.api.playerstate.PersonalTimeSetting;

import static org.junit.jupiter.api.Assertions.*;

final class PlayerStateArgumentTest {

    @Test
    void experienceDistinguishesPointsAndLevels() throws Exception {
        var argument = ExperienceAmountArgument.amount();
        assertEquals(ExperienceUnit.POINTS, argument.parse(new StringReader("100")).unit());
        assertEquals(ExperienceUnit.LEVELS, argument.parse(new StringReader("30L")).unit());
        assertEquals(ExperienceUnit.LEVELS, argument.parse(new StringReader("30l")).unit());
        assertThrows(Exception.class, () -> argument.parse(new StringReader("-1")));
    }

    @Test
    void personalTimeUsesTypedResetAndStablePresets() throws Exception {
        var argument = PersonalTimeArgument.time();
        assertInstanceOf(PersonalTimeSetting.Reset.class, argument.parse(new StringReader("reset")));
        assertEquals(
                23000L,
                ((PersonalTimeSetting.Fixed) argument.parse(new StringReader("dawn"))).ticks()
        );
        assertEquals(
                6000L,
                ((PersonalTimeSetting.Fixed) argument.parse(new StringReader("30000"))).ticks()
        );
        assertThrows(Exception.class, () -> argument.parse(new StringReader("-1")));
    }

}
