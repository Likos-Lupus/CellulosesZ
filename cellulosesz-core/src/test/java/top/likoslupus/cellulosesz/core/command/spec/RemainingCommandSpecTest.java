package top.likoslupus.cellulosesz.core.command.spec;

import org.junit.jupiter.api.Test;
import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.spec.CommandParameter;
import top.likoslupus.cellulosesz.api.command.spec.CommandParameterType;
import top.likoslupus.cellulosesz.api.command.spec.CommandRoute;
import top.likoslupus.cellulosesz.api.command.spec.CommandSpec;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

final class RemainingCommandSpecTest {

    private static final List<String> ROOTS = List.of(
            "antioch", "beezooka", "book", "break", "bigtree", "burn", "clearinventory",
            "clearinventoryconfirmtoggle", "condense", "compass", "depth", "disposal", "exp", "ext",
            "fireball", "gc", "getpos", "hat", "ice", "itemdb", "kill", "kittycannon", "lightning",
            "more", "nuke", "ping", "powertoollist", "powertooltoggle", "realname", "recipe", "rest",
            "editsign", "skull", "spawner", "spawnmob", "stonecutter", "sudo", "suicide", "thunder", "tree"
    );

    private final DefaultCommandSpecFactory factory = new DefaultCommandSpecFactory();

    @Test
    void everyImplementedRootHasAnExplicitSpec() {
        assertEquals(40, ROOTS.size());
        ROOTS.forEach(root -> {
            var spec = factory.spec(command(root));
            assertFalse(spec.automatic(), root);
            assertFalse(spec.routes().isEmpty(), root);
        });
    }

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
    void complexRoutesAcceptDeclaredShapesAndRejectMalformedShapes() {
        var valid = Map.ofEntries(
                Map.entry("ping", List.of(new String[]{}, new String[]{"hello", "world"})),
                Map.entry("getpos", List.of(new String[]{}, new String[]{"Alex"})),
                Map.entry("exp", List.of(new String[]{"show"}, new String[]{"give", "Alex", "5L"})),
                Map.entry("clearinventory", List.of(new String[]{}, new String[]{"*", "minecraft:stone", "4"}, new String[]{"confirm", "abc"})),
                Map.entry("clearinventoryconfirmtoggle", List.of(new String[]{}, new String[]{"off"})),
                Map.entry("book", List.of(new String[]{}, new String[]{"title", "A", "safe", "title"})),
                Map.entry("more", List.of(new String[]{}, new String[]{"12"})),
                Map.entry("recipe", List.of(new String[]{"minecraft:stone"}, new String[]{"minecraft:stone", "2"})),
                Map.entry("editsign", List.of(new String[]{"copy"}, new String[]{"set", "2", "Hello", "world"})),
                Map.entry("spawnmob", List.of(new String[]{"minecraft:pig"}, new String[]{"minecraft:pig", "2", "Alex"})),
                Map.entry("spawner", List.of(new String[]{"minecraft:pig"}, new String[]{"minecraft:pig", "40"})),
                Map.entry("sudo", List.<String[]>of(new String[]{"Alex", "msg", "hello"})),
                Map.entry("fireball", List.of(new String[]{}, new String[]{"arrow", "1.5"})),
                Map.entry("thunder", List.of(new String[]{"true"}, new String[]{"false", "60"})),
                Map.entry("tree", List.of(new String[]{}, new String[]{"swamp"})),
                Map.entry("bigtree", List.of(new String[]{}, new String[]{"darkoak"})),
                Map.entry("stonecutter", List.<String[]>of(new String[]{})),
                Map.entry("disposal", List.<String[]>of(new String[]{}))
        );
        var invalid = Map.ofEntries(
                Map.entry("getpos", List.<String[]>of(new String[]{"Alex", "extra"})),
                Map.entry("exp", List.of(new String[]{}, new String[]{"set"}, new String[]{"show", "Alex", "extra"})),
                Map.entry("clearinventory", List.of(new String[]{"Alex", "stone", "NaN"}, new String[]{"Alex", "stone", "1", "extra"})),
                Map.entry("clearinventoryconfirmtoggle", List.<String[]>of(new String[]{"maybe"})),
                Map.entry("book", List.of(new String[]{"title"}, new String[]{"unknown", "value"})),
                Map.entry("more", List.of(new String[]{"1.5"}, new String[]{"1", "2"})),
                Map.entry("recipe", List.of(new String[]{}, new String[]{"stone", "NaN"}, new String[]{"stone", "1", "extra"})),
                Map.entry("editsign", List.of(new String[]{}, new String[]{"set", "x", "text"}, new String[]{"copy", "extra"})),
                Map.entry("spawnmob", List.of(new String[]{}, new String[]{"pig", "NaN"}, new String[]{"pig", "1", "Alex", "extra"})),
                Map.entry("spawner", List.of(new String[]{}, new String[]{"pig", "NaN"}, new String[]{"pig", "1", "extra"})),
                Map.entry("sudo", List.<String[]>of(new String[]{"Alex"})),
                Map.entry("fireball", List.of(new String[]{"unknown"}, new String[]{"arrow", "NaN", "extra"})),
                Map.entry("thunder", List.of(new String[]{}, new String[]{"maybe"}, new String[]{"true", "x"})),
                Map.entry("tree", List.of(new String[]{"acacia"}, new String[]{"oak", "extra"})),
                Map.entry("bigtree", List.of(new String[]{"birch"}, new String[]{"oak", "extra"})),
                Map.entry("stonecutter", List.<String[]>of(new String[]{"extra"})),
                Map.entry("disposal", List.<String[]>of(new String[]{"extra"}))
        );

        valid.forEach((root, samples) -> samples.forEach(sample ->
                assertTrue(accepts(factory.spec(command(root)), sample), root + " should accept " + List.of(sample))));
        invalid.forEach((root, samples) -> samples.forEach(sample ->
                assertFalse(accepts(factory.spec(command(root)), sample), root + " should reject " + List.of(sample))));
    }

    private static boolean accepts(CommandSpec spec, String[] tokens) {
        return spec.routes().stream().anyMatch(route -> accepts(route, tokens));
    }

    private static boolean accepts(CommandRoute route, String[] tokens) {
        var cursor = 0;
        for (var index = 0; index < route.parameters().size(); index++) {
            var parameter = route.parameters().get(index);
            if (parameter.type() == CommandParameterType.GREEDY_STRING
                    || parameter.type() == CommandParameterType.ITEM) {
                if (cursor >= tokens.length) return parameter.optional();
                return index == route.parameters().size() - 1;
            }
            if (cursor >= tokens.length) {
                if (parameter.optional()) continue;
                return false;
            }
            if (!matches(parameter, tokens[cursor++])) return false;
        }
        return cursor == tokens.length;
    }

    private static boolean matches(CommandParameter parameter, String value) {
        if (!parameter.choices().isEmpty()
                && parameter.choices().stream().noneMatch(choice -> choice.equalsIgnoreCase(value))) return false;
        return switch (parameter.type()) {
            case INTEGER -> parsesInteger(value);
            case LONG -> parsesLong(value);
            case DOUBLE -> parsesFiniteDouble(value);
            case BOOLEAN -> value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false");
            default -> !value.isBlank();
        };
    }

    private static boolean parsesInteger(String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException _) {
            return false;
        }
    }

    private static boolean parsesLong(String value) {
        try {
            Long.parseLong(value);
            return true;
        } catch (NumberFormatException _) {
            return false;
        }
    }

    private static boolean parsesFiniteDouble(String value) {
        try {
            return Double.isFinite(Double.parseDouble(value));
        } catch (NumberFormatException _) {
            return false;
        }
    }

    @Test
    void everyRootAcceptsItsMinimalDocumentedShape() {
        var samples = Map.ofEntries(
                Map.entry("antioch", new String[]{}),
                Map.entry("beezooka", new String[]{}),
                Map.entry("book", new String[]{}),
                Map.entry("break", new String[]{}),
                Map.entry("bigtree", new String[]{"darkoak"}),
                Map.entry("burn", new String[]{"Alex", "30"}),
                Map.entry("clearinventory", new String[]{}),
                Map.entry("clearinventoryconfirmtoggle", new String[]{}),
                Map.entry("condense", new String[]{}),
                Map.entry("compass", new String[]{}),
                Map.entry("depth", new String[]{}),
                Map.entry("disposal", new String[]{}),
                Map.entry("exp", new String[]{"show"}),
                Map.entry("ext", new String[]{}),
                Map.entry("fireball", new String[]{}),
                Map.entry("gc", new String[]{}),
                Map.entry("getpos", new String[]{}),
                Map.entry("hat", new String[]{}),
                Map.entry("ice", new String[]{}),
                Map.entry("itemdb", new String[]{}),
                Map.entry("kill", new String[]{"Alex"}),
                Map.entry("kittycannon", new String[]{}),
                Map.entry("lightning", new String[]{}),
                Map.entry("more", new String[]{}),
                Map.entry("nuke", new String[]{}),
                Map.entry("ping", new String[]{}),
                Map.entry("powertoollist", new String[]{}),
                Map.entry("powertooltoggle", new String[]{}),
                Map.entry("realname", new String[]{"nick"}),
                Map.entry("recipe", new String[]{"minecraft:stone"}),
                Map.entry("rest", new String[]{}),
                Map.entry("editsign", new String[]{"copy"}),
                Map.entry("skull", new String[]{}),
                Map.entry("spawner", new String[]{"minecraft:pig"}),
                Map.entry("spawnmob", new String[]{"minecraft:pig"}),
                Map.entry("stonecutter", new String[]{}),
                Map.entry("sudo", new String[]{"Alex", "help"}),
                Map.entry("suicide", new String[]{}),
                Map.entry("thunder", new String[]{"true"}),
                Map.entry("tree", new String[]{"oak"})
        );
        assertEquals(Set.copyOf(ROOTS), samples.keySet());
        samples.forEach((root, sample) ->
                assertTrue(accepts(factory.spec(command(root)), sample), root + " should accept " + List.of(sample)));
    }

    @Test
    void fixedArityRootsRejectOneUnexpectedTrailingArgument() {
        var greedyRoots = Set.of("antioch", "book", "ping", "sudo");
        var minimal = Map.ofEntries(
                Map.entry("beezooka", new String[]{}), Map.entry("break", new String[]{}),
                Map.entry("bigtree", new String[]{"oak"}), Map.entry("burn", new String[]{"Alex", "1"}),
                Map.entry("clearinventoryconfirmtoggle", new String[]{}), Map.entry("compass", new String[]{}),
                Map.entry("depth", new String[]{}), Map.entry("disposal", new String[]{}),
                Map.entry("exp", new String[]{"show", "Alex"}), Map.entry("ext", new String[]{"Alex"}),
                Map.entry("fireball", new String[]{}), Map.entry("gc", new String[]{}),
                Map.entry("getpos", new String[]{"Alex"}), Map.entry("hat", new String[]{}),
                Map.entry("ice", new String[]{"Alex"}), Map.entry("kill", new String[]{"Alex"}),
                Map.entry("kittycannon", new String[]{}), Map.entry("lightning", new String[]{"Alex"}),
                Map.entry("more", new String[]{}), Map.entry("nuke", new String[]{"Alex"}),
                Map.entry("powertoollist", new String[]{}), Map.entry("powertooltoggle", new String[]{}),
                Map.entry("realname", new String[]{"nick"}), Map.entry("recipe", new String[]{"stone"}),
                Map.entry("rest", new String[]{"Alex"}), Map.entry("editsign", new String[]{"copy"}),
                Map.entry("skull", new String[]{"Owner", "Alex"}), Map.entry("spawner", new String[]{"pig"}),
                Map.entry("spawnmob", new String[]{"pig"}), Map.entry("stonecutter", new String[]{}),
                Map.entry("suicide", new String[]{}), Map.entry("thunder", new String[]{"true"}),
                Map.entry("tree", new String[]{"oak"})
        );
        minimal.forEach((root, sample) -> {
            var extra = Arrays.copyOf(sample, sample.length + 1);
            extra[extra.length - 1] = "unexpected";
            assertFalse(accepts(factory.spec(command(root)), extra), root + " should reject trailing input");
        });
        assertTrue(ROOTS.containsAll(greedyRoots));
    }

    @Test
    void greedyTextIsLimitedToFreeTextTails() {
        var allowed = Set.of("message", "command", "title", "author", "text");
        ROOTS.forEach(root -> factory.spec(command(root))
                .routes()
                .forEach(route ->
                        IntStream.range(0, route.parameters().size())
                                .forEach(index -> {
                                    var parameter = route.parameters().get(index);
                                    if (parameter.type() != CommandParameterType.GREEDY_STRING) return;
                                    assertTrue(allowed.contains(parameter.name()), root + ":" + parameter.name());
                                    assertEquals(route.parameters().size() - 1, index, root + ":" + parameter.name());
                                })
                )
        );
    }

}
