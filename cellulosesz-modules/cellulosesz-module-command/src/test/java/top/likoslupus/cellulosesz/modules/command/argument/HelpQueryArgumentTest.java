package top.likoslupus.cellulosesz.modules.command.argument;

import com.mojang.brigadier.StringReader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class HelpQueryArgumentTest {

    @Test
    void acceptsWordsAndRejectsIntegerLookingPages() throws Exception {
        assertEquals("economy", HelpQueryArgument.query().parse(new StringReader("economy")));
        assertThrows(Exception.class, () -> HelpQueryArgument.query().parse(new StringReader("0")));
        assertThrows(Exception.class, () -> HelpQueryArgument.query().parse(new StringReader("2")));
    }

}
