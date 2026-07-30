package top.likoslupus.cellulosesz.modules.economy.command.argument;

import com.mojang.brigadier.StringReader;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class PaymentTargetsArgumentTest {

    @Test
    void normalizesCaseInsensitiveDuplicatesWithoutResolvingPlayers() throws Exception {
        assertEquals(
                List.of("Alice", "Bob"),
                PaymentTargetsArgument.targets(3)
                        .parse(new StringReader("Alice,bob,ALICE"))
        );
    }

    @Test
    void rejectsEmptySegmentsAndTooManyTokens() {
        assertThrows(
                Exception.class,
                () -> PaymentTargetsArgument.targets(3)
                        .parse(new StringReader("Alice,,Bob"))
        );
        assertThrows(
                Exception.class,
                () -> PaymentTargetsArgument.targets(2)
                        .parse(new StringReader("Alice,Bob,Charlie"))
        );
    }

}
