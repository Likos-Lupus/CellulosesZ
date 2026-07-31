package top.likoslupus.cellulosesz.core.command.spec;

import org.junit.jupiter.api.Test;
import top.likoslupus.cellulosesz.core.command.DirectCommandMigrationIndex;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.*;

final class Segment2CommandsAreAbsentFromLegacySpecTest {

    private static final Set<String> ROOTS = Set.of(
            "cellulosesz", "help", "broadcast", "broadcastworld", "helpop", "ignore", "list", "mail",
            "me", "msg", "msgtoggle", "r", "rtoggle", "socialspy", "balance", "balancetop", "eco",
            "pay", "payconfirmtoggle", "paytoggle", "sell", "setworth", "worth", "afk", "compass",
            "depth", "exp", "feed", "fly", "gamemode", "getpos", "god", "heal", "near", "nick",
            "ping", "playtime", "ptime", "pweather", "realname", "rest", "seen", "speed", "vanish",
            "whois"
    );

    @Test
    void migrationIndexContainsTheFullDirectSet() {
        assertEquals(45, ROOTS.size());
        assertEquals(104, DirectCommandMigrationIndex.roots().size());
        assertTrue(DirectCommandMigrationIndex.roots().containsAll(ROOTS));
    }

    @Test
    void explicitSwitchAndLegacyBridgeHaveNoSegmentTwoRegistrationCase() throws Exception {
        var root = projectRoot();
        var factory = Files.readString(root.resolve("cellulosesz-core/src/main/java/top/likoslupus/cellulosesz/core/command/spec/DefaultCommandSpecFactory.java"));
        var bridge = Files.readString(root.resolve("cellulosesz-common/src/main/java/top/likoslupus/cellulosesz/common/command/legacy/LegacyCommandBridge.java"));
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

}
