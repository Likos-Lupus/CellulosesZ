package top.likoslupus.cellulosesz.core.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

import static java.util.Objects.requireNonNull;

final class RemainingCommandTreeContractTest {

    private static final Set<String> ROOTS = Set.of(
            "anvil",
            "book",
            "cartographytable",
            "clearinventory",
            "clearinventoryconfirmtoggle",
            "condense",
            "disposal",
            "enchant",
            "enderchest",
            "firework",
            "give",
            "grindstone",
            "hat",
            "invsee",
            "item",
            "itemdb",
            "itemlore",
            "itemname",
            "loom",
            "more",
            "potion",
            "powertool",
            "powertoollist",
            "powertooltoggle",
            "recipe",
            "repair",
            "skull",
            "smithingtable",
            "stonecutter",
            "unlimited",
            "workbench",
            "antioch",
            "backup",
            "beezooka",
            "bigtree",
            "break",
            "fireball",
            "gc",
            "kittycannon",
            "lightning",
            "nuke",
            "remove",
            "spawner",
            "spawnmob",
            "thunder",
            "time",
            "tree",
            "weather",
            "editsign"
    );

    private static final Map<String, List<String>> TYPED_MARKERS = typedMarkers();

    private static Map<String, List<String>> typedMarkers() {
        var result = new LinkedHashMap<String, List<String>>();
        result.put("item", List.of("ItemDescriptorArgument", "IntegerArgumentType.integer"));
        result.put(
                "give",
                List.of(
                        "ItemDescriptorArgument",
                        "EntityArgument.player",
                        "IntegerArgumentType.integer"
                )
        );
        result.put(
                "enchant",
                List.of(
                        "ResourceArgument.resource",
                        "Registries.ENCHANTMENT",
                        "IntegerArgumentType.integer"
                )
        );
        result.put(
                "potion",
                List.of(
                        "ResourceArgument.resource",
                        "Registries.MOB_EFFECT",
                        "IntegerArgumentType.integer"
                )
        );
        result.put(
                "clearinventory",
                List.of("ItemDescriptorArgument", "IntegerArgumentType.integer(1, 1_000_000)")
        );
        result.put("condense", List.of("ItemDescriptorArgument"));
        result.put("recipe", List.of("ItemDescriptorArgument", "IntegerArgumentType.integer(1)"));
        result.put("itemdb", List.of("ItemDescriptorArgument"));
        result.put("more", List.of("IntegerArgumentType.integer(1"));
        result.put("powertool", List.of("StringArgumentType.greedyString()"));
        result.put("powertoollist", List.of("IntegerArgumentType.integer(1)"));
        result.put("time", List.of("TimeValueArgument", "DimensionArgument.dimension"));
        result.put("weather", List.of("WeatherTypeArgument", "DimensionArgument.dimension"));
        result.put(
                "remove",
                List.of(
                        "ResourceArgument.resource",
                        "Registries.ENTITY_TYPE",
                        "IntegerArgumentType.integer(1, 4_096)"
                )
        );
        result.put("fireball", List.of("ProjectileTypeArgument", "DoubleArgumentType.doubleArg"));
        result.put(
                "spawner",
                List.of(
                        "ResourceArgument.resource",
                        "Registries.ENTITY_TYPE",
                        "IntegerArgumentType.integer"
                )
        );
        result.put(
                "spawnmob",
                List.of(
                        "ResourceArgument.resource",
                        "Registries.ENTITY_TYPE",
                        "IntegerArgumentType.integer"
                )
        );
        result.put("tree", List.of("TreeTypeArgument"));
        result.put("bigtree", List.of("TreeTypeArgument"));
        result.put(
                "editsign",
                List.of("IntegerArgumentType.integer(1, 4)", "StringArgumentType.greedyString()")
        );
        return Map.copyOf(result);
    }

    @Test
    void allRemainingRootsHaveModuleOwnedDirectTreesAndAccurateContracts() throws IOException {
        var project = projectRoot();
        var contracts = contracts(project.resolve("docs/refactor-command-contract.csv"));
        assertEquals(ROOTS, contracts.keySet());

        for (var entry : contracts.entrySet()) {
            var root = entry.getKey();
            var contract = entry.getValue();
            var source = project.resolve(contract.sourceFile());
            assertTrue(Files.exists(source), source.toString());
            var text = Files.readString(source);

            assertTrue(text.contains("implements CommandContributor"), root);
            assertTrue(text.contains("Commands.literal("), root);
            assertTrue(text.contains("registerDirect("), root);
            assertFalse(text.contains("CommandInvocation"), root);
            assertFalse(text.contains("CellCommand"), root);
            assertFalse(text.contains("String[] args"), root);
            assertFalse(source.getFileName().toString().contains("Brigadier"), root);
            assertFalse(source.toString().contains("cellulosesz-fabric"), root);

            for (var marker : TYPED_MARKERS.getOrDefault(root, List.of())) {
                assertTrue(text.contains(marker), root + " missing typed marker " + marker);
            }

            var moduleText = moduleText(source);
            for (var alias : contract.aliases()) {
                assertTrue(
                        moduleText.contains('"' + alias + '"'),
                        root + " missing alias " + alias
                );
            }
        }
    }

    private static Path projectRoot() {
        var current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (current != null && !Files.exists(current.resolve("settings.gradle.kts"))) {
            current = current.getParent();
        }
        return requireNonNull(current, "project root");
    }

    private static Map<String, Contract> contracts(Path path) throws IOException {
        var lines = Files.readAllLines(path);
        var headers = csv(lines.get(0));
        var rootIndex = headers.indexOf("root");
        var aliasesIndex = headers.indexOf("aliases");
        var sourceKindIndex = headers.indexOf("source_kind");
        var usageIndex = headers.indexOf("usage");
        var sourceIndex = headers.indexOf("source_file");
        var result = new LinkedHashMap<String, Contract>();
        for (var line : lines.subList(1, lines.size())) {
            var columns = csv(line);
            var root = columns.get(rootIndex);
            if (!ROOTS.contains(root)) {
                continue;
            }
            var aliases = columns.get(aliasesIndex).isBlank()
                    ? List.<String>of()
                    : List.of(columns.get(aliasesIndex).split(";"));
            result.put(
                    root, new Contract(
                            aliases,
                            columns.get(sourceKindIndex),
                            columns.get(usageIndex),
                            columns.get(sourceIndex)
                    )
            );
        }
        return Map.copyOf(result);
    }

    private static String moduleText(Path source) throws IOException {
        var normalized = source.toAbsolutePath().normalize();
        var current = normalized.getParent();
        while (current != null && !Files.exists(current.resolve("build.gradle.kts"))) {
            current = current.getParent();
        }
        current = requireNonNull(current, "module root");
        var text = new StringBuilder();
        try (var paths = Files.walk(current.resolve("src/main/java"))) {
            for (var path : paths.filter(value -> value.toString().endsWith(".java")).toList()) {
                text.append(Files.readString(path)).append('\n');
            }
        }
        return text.toString();
    }

    private static List<String> csv(String row) {
        var values = new ArrayList<String>();
        var value = new StringBuilder();
        var quoted = false;

        for (var index = 0; index < row.length(); index++) {
            var character = row.charAt(index);
            if (character == '"') {
                if (quoted && index + 1 < row.length() && row.charAt(index + 1) == '"') {
                    value.append('"');
                    index++;
                } else {
                    quoted = !quoted;
                }
            } else if (character == ',' && !quoted) {
                values.add(value.toString());
                value.setLength(0);
            } else {
                value.append(character);
            }
        }

        values.add(value.toString());
        return List.copyOf(values);
    }

    @Test
    void contractSyntaxRecordsTheUnambiguousAndBoundedTrees() throws IOException {
        var contracts = contracts(projectRoot().resolve("docs/refactor-command-contract.csv"));
        assertTrue(contracts.get("clearinventory").usage().contains("player <player>"));
        assertTrue(contracts.get("clearinventory").usage().contains("item <item> [amount]"));
        assertTrue(contracts.get("editsign").usage().contains("set <line> <text>"));
        assertTrue(contracts.get("weather").usage().contains("[seconds] [world]"));
        assertTrue(contracts.get("time").usage().contains("[world]"));
        assertTrue(contracts.get("powertool").usage().contains("<command"));
        assertEquals("ANY", contracts.get("enderchest").sourceKind());
        assertEquals("PLAYER_ONLY", contracts.get("remove").sourceKind());
        assertEquals("PLAYER_ONLY", contracts.get("tree").sourceKind());
        assertEquals("PLAYER_ONLY", contracts.get("bigtree").sourceKind());
    }

    private record Contract(
            List<String> aliases,
            String sourceKind,
            String usage,
            String sourceFile
    ) {

        private Contract {
            aliases = List.copyOf(aliases);
        }

    }

}
