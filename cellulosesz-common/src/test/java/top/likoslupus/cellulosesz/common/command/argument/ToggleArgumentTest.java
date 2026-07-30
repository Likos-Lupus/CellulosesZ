package top.likoslupus.cellulosesz.common.command.argument;

import com.mojang.brigadier.StringReader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class ToggleArgumentTest {

    @Test
    void acceptsCanonicalAndCompatibilityTokens() throws Exception {
        var argument = ToggleArgument.toggle();
        assertEquals(ToggleMode.ON, argument.parse(new StringReader("on")));
        assertEquals(ToggleMode.ON, argument.parse(new StringReader("enabled")));
        assertEquals(ToggleMode.OFF, argument.parse(new StringReader("false")));
        assertEquals(ToggleMode.OFF, argument.parse(new StringReader("disable")));
    }

    @Test
    void rejectsUnknownTokensAndSuggestsOnlyCanonicalValues() {
        var argument = ToggleArgument.toggle();
        assertThrows(Exception.class, () -> argument.parse(new StringReader("maybe")));
        assertEquals(java.util.List.of("on", "off"), argument.getExamples());
    }

}
