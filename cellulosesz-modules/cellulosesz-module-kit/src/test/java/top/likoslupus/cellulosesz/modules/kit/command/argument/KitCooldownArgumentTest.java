package top.likoslupus.cellulosesz.modules.kit.command.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.junit.jupiter.api.Test;
import top.likoslupus.cellulosesz.modules.kit.application.KitCooldown;

import static org.junit.jupiter.api.Assertions.*;

final class KitCooldownArgumentTest {

    private final KitCooldownArgument argument = KitCooldownArgument.cooldown();

    @Test
    void parsesTypedValues() throws CommandSyntaxException {
        assertEquals(new KitCooldown.Seconds(0), argument.parse(new StringReader("0")));
        assertEquals(new KitCooldown.Seconds(60), argument.parse(new StringReader("60")));
        assertInstanceOf(KitCooldown.Once.class, argument.parse(new StringReader("once")));
    }

    @Test
    void rejectsNegativeOverflowAndInvalid() {
        assertThrows(
                CommandSyntaxException.class,
                () -> argument.parse(new StringReader("-1"))
        );
        assertThrows(
                CommandSyntaxException.class,
                () -> argument.parse(new StringReader("999999999999999999999999"))
        );
        assertThrows(
                CommandSyntaxException.class,
                () -> argument.parse(new StringReader("later"))
        );
    }

}
