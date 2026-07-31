package top.likoslupus.cellulosesz.core.command.spec;

import org.junit.jupiter.api.Test;
import top.likoslupus.cellulosesz.core.command.DirectCommandMigrationIndex;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.*;

final class Segment3CommandsAreAbsentFromLegacySpecTest {

    private static final Set<String> ROOTS = Set.of(
            "ban", "banip", "burn", "deljail", "ext", "ice", "jail", "jailedplayers", "jails",
            "kick", "kickall", "kill", "mute", "setjail", "sudo", "suicide", "tempban",
            "tempbanip", "unban", "unbanip", "back", "bottom", "jump", "settpr", "top", "tp",
            "tpa", "tpaall", "tpacancel", "tpaccept", "tpahere", "tpall", "tpauto", "tpdeny",
            "tphere", "tpo", "tpoffline", "tpohere", "tppos", "tpr", "tptoggle", "world"
    );

    @Test
    void migrationIndexContainsExactlyOneHundredFourDirectRoots() {
        assertEquals(42, ROOTS.size());
        assertEquals(104, DirectCommandMigrationIndex.roots().size());
        assertTrue(DirectCommandMigrationIndex.roots().containsAll(ROOTS));
    }

    @Test
    void segmentThreeRootsHaveNoExplicitOrFallbackLegacyRoute() throws Exception {
        var root = projectRoot();
        var factory = Files.readString(root.resolve(
                "cellulosesz-core/src/main/java/top/likoslupus/cellulosesz/core/command/spec/DefaultCommandSpecFactory.java"));
        var bridge = Files.readString(root.resolve(
                "cellulosesz-common/src/main/java/top/likoslupus/cellulosesz/common/command/legacy/LegacyCommandBridge.java"));
        ROOTS.forEach(command -> {
            assertFalse(factory.contains("case \"" + command + "\""), command);
            assertFalse(bridge.contains("Commands.literal(\"" + command + "\")"), command);
        });
        assertTrue(factory.contains("DirectCommandMigrationIndex.contains"));
        assertTrue(bridge.contains("DirectCommandMigrationIndex.contains"));
    }

    private static Path projectRoot() {
        var current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (current != null && !Files.exists(current.resolve("settings.gradle.kts"))) {
            current = current.getParent();
        }
        return requireNonNull(current, "project root");
    }

    @Test
    void toggleJailIsOnlyAnAliasAndNotACanonicalDirectRoot() {
        assertFalse(DirectCommandMigrationIndex.roots().contains("togglejail"));
        assertTrue(DirectCommandMigrationIndex.roots().contains("jail"));
    }

}
