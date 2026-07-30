package top.likoslupus.cellulosesz.core.command.spec;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

final class RemainingCommandSpecTest {

    private static final List<String> ROOTS = List.of(
            "antioch", "beezooka", "book", "break", "bigtree", "burn", "clearinventory",
            "clearinventoryconfirmtoggle", "condense", "disposal", "ext", "fireball", "gc", "hat",
            "ice", "itemdb", "kill", "kittycannon", "lightning", "more", "nuke", "powertoollist",
            "powertooltoggle", "recipe", "editsign", "skull", "spawner", "spawnmob", "stonecutter",
            "sudo", "suicide", "thunder", "tree"
    );

    private static final Set<String> SEGMENT_2 = Set.of(
            "cellulosesz", "help", "broadcast", "broadcastworld", "helpop", "ignore", "list", "mail",
            "me", "msg", "msgtoggle", "r", "rtoggle", "socialspy", "balance", "balancetop", "eco",
            "pay", "payconfirmtoggle", "paytoggle", "sell", "setworth", "worth", "afk", "compass",
            "depth", "exp", "feed", "fly", "gamemode", "getpos", "god", "heal", "near", "nick",
            "ping", "playtime", "ptime", "pweather", "realname", "rest", "seen", "speed", "vanish",
            "whois"
    );

    private final DefaultCommandSpecFactory factory = new DefaultCommandSpecFactory();

    @Test
    void everyRemainingImplementedRootHasAnExplicitSpec() {
        assertEquals(33, ROOTS.size());
        ROOTS.forEach(root -> {
            var spec = factory.spec(command(root));
            assertFalse(spec.automatic(), root);
            assertFalse(spec.routes().isEmpty(), root);
        });
    }

    @NullMarked
    private static CellCommand command(String name) {
        return new CellCommand() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public int execute(CommandInvocation invocation) {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Test
    void segmentTwoRootsCannotUseExplicitOrGeneratedFallbackSpecs() {
        assertEquals(45, SEGMENT_2.size());
        SEGMENT_2.forEach(root -> assertThrows(
                IllegalArgumentException.class,
                () -> factory.spec(command(root)),
                root
        ));
    }

    @Test
    void representativeRemainingShapesStayExplicit() {
        var expectedRoutes = Map.of(
                "burn", 1,
                "clearinventory", 11,
                "recipe", 2,
                "spawnmob", 3,
                "sudo", 1,
                "thunder", 2
        );
        expectedRoutes.forEach((root, count) ->
                assertEquals(count, factory.spec(command(root)).routes().size(), root)
        );
    }

}
