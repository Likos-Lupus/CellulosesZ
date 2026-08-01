package top.likoslupus.cellulosesz.core.architecture;

import org.junit.jupiter.api.Test;
import top.likoslupus.cellulosesz.api.entity.ProjectileRequest;
import top.likoslupus.cellulosesz.api.entity.ProjectileType;
import top.likoslupus.cellulosesz.api.entity.TntBurstRequest;
import top.likoslupus.cellulosesz.api.item.FireworkItemRequest;
import top.likoslupus.cellulosesz.api.item.FireworkShape;
import top.likoslupus.cellulosesz.api.item.InventoryClearFilter;
import top.likoslupus.cellulosesz.api.item.PotionItemRequest;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.teleport.CellLocation;
import top.likoslupus.cellulosesz.api.world.EntityRemovalRequest;
import top.likoslupus.cellulosesz.api.world.EntityRemoveSelector;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class StructuredRequestChecksTest {

    private static final CellPlayer PLAYER = new CellPlayer(UUID.randomUUID(), "tester", new Object());
    private static final CellLocation LOCATION = new CellLocation("minecraft:overworld", 0, 64, 0, 0, 0);

    @Test
    void fireworkRequestsEnforcePowerColorAndEffectInvariants() {
        assertEquals(3, FireworkItemRequest.power(3).power());
        assertThrows(IllegalArgumentException.class, () -> FireworkItemRequest.power(0));
        assertThrows(IllegalArgumentException.class, () -> FireworkItemRequest.power(4));
        assertThrows(
                IllegalArgumentException.class,
                () -> FireworkItemRequest.effect(FireworkShape.SMALL_BALL, List.of(), List.of(), false, false)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> FireworkItemRequest.effect(FireworkShape.SMALL_BALL, List.of(0x1000000), List.of(), false, false)
        );
    }

    @Test
    void potionAndInventoryFilterRequestsRejectInvalidCrossFieldStates() {
        assertEquals("minecraft:speed", PotionItemRequest.apply("minecraft:speed", 30, 1).effectId().orElseThrow());
        assertThrows(IllegalArgumentException.class, () -> PotionItemRequest.apply("minecraft:speed", 0, 0));
        assertThrows(
                IllegalArgumentException.class,
                () -> new PotionItemRequest(Optional.empty(), 1, 0)
        );
        assertEquals(InventoryClearFilter.Kind.ITEM, InventoryClearFilter.item("minecraft:stone").kind());
        assertThrows(
                IllegalArgumentException.class,
                () -> new InventoryClearFilter(InventoryClearFilter.Kind.ITEM, Optional.empty())
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new InventoryClearFilter(
                        InventoryClearFilter.Kind.ALL_INVENTORY,
                        Optional.of("minecraft:stone")
                )
        );
    }

    @Test
    void projectileRemovalAndTntRequestsUseFinitePositiveBounds() {
        assertEquals(2.0, new ProjectileRequest(PLAYER, ProjectileType.FIREBALL, 2.0, 40).speed());
        assertThrows(
                IllegalArgumentException.class,
                () -> new ProjectileRequest(PLAYER, ProjectileType.FIREBALL, Double.NaN, 40)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new ProjectileRequest(PLAYER, ProjectileType.FIREBALL, 1.0, 0)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new EntityRemovalRequest(EntityRemoveSelector.of(EntityRemoveSelector.Kind.ALL), Optional.of(PLAYER), 0)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new TntBurstRequest(LOCATION, 1, 20, Double.POSITIVE_INFINITY, false, 0, 0)
        );
    }

}
