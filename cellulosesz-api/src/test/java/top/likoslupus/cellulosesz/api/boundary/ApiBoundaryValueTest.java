package top.likoslupus.cellulosesz.api.boundary;

import org.junit.jupiter.api.Test;
import top.likoslupus.cellulosesz.api.item.ItemDescriptor;
import top.likoslupus.cellulosesz.api.kit.KitDefinition;
import top.likoslupus.cellulosesz.api.kit.KitItem;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformOperationStatus;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.api.teleport.CellLocation;
import top.likoslupus.cellulosesz.api.text.LocalizedMessage;
import top.likoslupus.cellulosesz.api.text.MessageArgument;
import top.likoslupus.cellulosesz.api.text.MessageArguments;
import top.likoslupus.cellulosesz.api.text.RichText;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

final class ApiBoundaryValueTest {

    @Test
    void immutableDomainValuesNormalizeAndDefensivelyCopy() {
        var descriptor = new ItemDescriptor(" Stone ", 2, "  minecraft:stone  ");
        assertEquals("minecraft:stone", descriptor.item());
        assertEquals("minecraft:stone", descriptor.argument());

        var mutable = new ArrayList<KitItem>();
        mutable.add(new KitItem(0, " stack "));
        var kit = new KitDefinition(
                " starter ",
                " Starter ",
                Optional.of("  "),
                Duration.ofSeconds(30),
                new BigDecimal("1.50"),
                mutable
        );
        mutable.clear();

        assertEquals("starter", kit.id());
        assertEquals(Optional.empty(), kit.permission());
        assertEquals(1, kit.items().size());
        assertThrows(UnsupportedOperationException.class, () -> kit.items().clear());
    }

    @Test
    void invalidValuesFailAtConstruction() {
        assertThrows(IllegalArgumentException.class, () -> new ItemDescriptor("stone", 0));
        assertThrows(
                IllegalArgumentException.class,
                () -> new CellLocation("world", Double.NaN, 0, 0, 0, 0)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new KitDefinition(
                        "starter",
                        "Starter",
                        Optional.empty(),
                        Duration.ofMillis(1),
                        BigDecimal.ZERO,
                        java.util.List.of(new KitItem(0, "stack"))
                )
        );
    }

    @Test
    void typedMessageArgumentsCoverEveryPublicVariant() {
        var uuid = UUID.fromString("00000000-0000-0000-0000-000000000502");
        var nested = LocalizedMessage.of("nested.key");
        var rich = RichText.plain("rich");
        var values = MessageArguments.builder()
                .put("text", "value")
                .put("number", new BigDecimal("12.50"))
                .put("boolean", true)
                .put("uuid", uuid)
                .put("rich", rich)
                .put("nested", nested)
                .build()
                .values();

        assertEquals("value", ((MessageArgument.Text) values.get("text")).value());
        assertEquals(
                new BigDecimal("12.50"),
                ((MessageArgument.Number) values.get("number")).value()
        );
        assertTrue(((MessageArgument.BooleanValue) values.get("boolean")).value());
        assertEquals(uuid, ((MessageArgument.UuidValue) values.get("uuid")).value());
        assertEquals(rich, ((MessageArgument.RichTextValue) values.get("rich")).value());
        assertEquals(nested, ((MessageArgument.NestedMessage) values.get("nested")).value());
    }

    @Test
    void zeroAndFalseRemainSuccessfulValuesDistinctFromFailure() {
        var zero = PlatformResult.success(0);
        var falseValue = PlatformResult.success(false);
        var failure = PlatformResult.<Integer>failure(
                PlatformOperationStatus.NOT_READY,
                "server unavailable"
        );

        assertTrue(zero.successful());
        assertEquals(0, zero.value().orElseThrow().intValue());
        assertTrue(falseValue.successful());
        assertFalse(falseValue.value().orElseThrow());
        assertFalse(failure.successful());
        assertTrue(failure.value().isEmpty());
    }

}
