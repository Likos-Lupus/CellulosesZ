package top.likoslupus.cellulosesz.modules.kit.persistence;

import org.junit.jupiter.api.Test;
import top.likoslupus.cellulosesz.api.kit.KitDefinition;
import top.likoslupus.cellulosesz.api.kit.KitItem;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class KitMapperTest {

    @Test
    void roundTripsImmutableKit() {
        var kit = new KitDefinition(
                "daily",
                "Daily Kit",
                Optional.empty(),
                Duration.ofSeconds(30),
                new BigDecimal("12.50"),
                List.of(new KitItem(4, "{id:\"minecraft:stone\",count:1}"))
        );

        assertEquals(kit, KitMapper.toDomain(KitMapper.fromDomain(kit)));
    }

    @Test
    void rejectsInvalidPersistedCostWithContext() {
        var document = KitMapper.fromDomain(new KitDefinition(
                "daily",
                "Daily Kit",
                Optional.empty(),
                Duration.ZERO,
                BigDecimal.ZERO,
                List.of(new KitItem(0, "{id:\"minecraft:stone\",count:1}"))
        ));
        document.cost = "not-a-decimal";

        var failure = assertThrows(
                IllegalArgumentException.class,
                () -> KitMapper.toDomain(document)
        );
        assertEquals("Invalid persisted kit document daily", failure.getMessage());
    }

}
