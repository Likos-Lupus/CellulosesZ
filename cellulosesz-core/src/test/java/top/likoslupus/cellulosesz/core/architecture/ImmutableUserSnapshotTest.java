package top.likoslupus.cellulosesz.core.architecture;

import org.junit.jupiter.api.Test;
import top.likoslupus.cellulosesz.api.user.CellUser;
import top.likoslupus.cellulosesz.api.user.UserRelations;
import top.likoslupus.cellulosesz.api.user.UserState;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

final class ImmutableUserSnapshotTest {

    @Test
    void construct_withNestedMutableValues_defensivelyCopies() {
        var commands = new ArrayList<>(List.of("home"));
        var commandMap = new HashMap<String, List<String>>();
        commandMap.put("minecraft:stick", commands);
        var unlimited = new HashSet<>(Set.of("minecraft:stone"));

        var state = new UserState(
                false,
                false,
                false,
                false,
                null,
                null,
                null,
                commandMap,
                unlimited
        );
        commands.add("spawn");
        commandMap.put("minecraft:blaze_rod", List.of("warp"));
        unlimited.add("minecraft:diamond");

        assertEquals(List.of("home"), state.powerToolCommands().get("minecraft:stick"));
        assertFalse(state.powerToolCommands().containsKey("minecraft:blaze_rod"));
        assertEquals(Set.of("minecraft:stone"), state.unlimitedItems());
        assertThrows(
                UnsupportedOperationException.class,
                () -> state.powerToolCommands().put("minecraft:book", List.of("help"))
        );
        assertThrows(
                UnsupportedOperationException.class,
                () -> state.powerToolCommands().get("minecraft:stick").add("help")
        );
    }

    @Test
    void publish_userAndRelations_returnsDefensiveSnapshots() {
        var uuid = UUID.randomUUID();
        var cooldowns = new HashMap<String, Long>();
        cooldowns.put("home", 5L);
        var ignored = new HashSet<UUID>();
        var ignoredPlayer = UUID.randomUUID();
        ignored.add(ignoredPlayer);

        var user = CellUser.create(uuid)
                .withCooldowns(cooldowns)
                .withRelations(new UserRelations(ignored));
        cooldowns.put("warp", 10L);
        ignored.clear();

        assertEquals(Map.of("home", 5L), user.cooldowns());
        assertEquals(Set.of(ignoredPlayer), user.relations().ignored());
        assertThrows(UnsupportedOperationException.class, () -> user.cooldowns().put("spawn", 1L));
        assertThrows(UnsupportedOperationException.class, () -> user.relations().ignored().clear());
    }

    @Test
    void withers_whenInvoked_doNotMutateOriginal() {
        var original = CellUser.create(UUID.randomUUID());
        var changed = original.withPreferences(original.preferences().withPowerToolsEnabled(false));

        assertNotSame(original, changed);
        assertTrue(original.preferences().powerToolsEnabled());
        assertFalse(changed.preferences().powerToolsEnabled());
        assertEquals(original.uuid(), changed.uuid());
        assertEquals(original.state(), changed.state());
    }

}
