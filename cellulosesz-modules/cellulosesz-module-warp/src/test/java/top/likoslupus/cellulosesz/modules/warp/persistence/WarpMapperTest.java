package top.likoslupus.cellulosesz.modules.warp.persistence;

import org.junit.jupiter.api.Test;
import top.likoslupus.cellulosesz.api.teleport.CellLocation;
import top.likoslupus.cellulosesz.api.warp.Warp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class WarpMapperTest {

    @Test
    void roundTripsImmutableWarp() {
        var warp = new Warp(
                "spawn",
                "Spawn",
                new BigDecimal("3.25"),
                new CellLocation("minecraft:overworld", 1.5, 64.0, -2.5, 90.0F, 0.0F),
                Optional.of(UUID.fromString("00000000-0000-0000-0000-000000000456")),
                Instant.ofEpochMilli(1_700_000_000_000L)
        );

        assertEquals(warp, WarpMapper.toDomain(WarpMapper.fromDomain(warp)));
    }

    @Test
    void rejectsInvalidPersistedCreatorWithWarpContext() {
        var document = WarpMapper.fromDomain(new Warp(
                "spawn",
                new CellLocation("minecraft:overworld", 0.0, 64.0, 0.0, 0.0F, 0.0F)
        ));
        document.createdBy = "invalid";

        var failure = assertThrows(
                IllegalArgumentException.class,
                () -> WarpMapper.toDomain(document)
        );
        assertEquals("Invalid persisted warp document spawn", failure.getMessage());
    }

}
