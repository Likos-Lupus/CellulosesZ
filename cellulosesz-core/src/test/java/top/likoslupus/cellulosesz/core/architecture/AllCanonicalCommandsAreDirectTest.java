package top.likoslupus.cellulosesz.core.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.*;

final class AllCanonicalCommandsAreDirectTest {

    @Test
    void ledgerContainsExactly153DirectCanonicalRoots() throws IOException {
        var rows = Files.readAllLines(projectRoot().resolve("docs/refactor-command-migration.csv"));
        assertEquals(154, rows.size(), "header plus 153 canonical roots");

        var roots = new HashSet<String>();
        for (var row : rows.subList(1, rows.size())) {
            var columns = csv(row);
            assertEquals(10, columns.size(), row);
            assertTrue(roots.add(columns.getFirst()), "duplicate root: " + columns.getFirst());
            assertEquals("MIGRATED_COMMON_UNVERIFIED_RUNTIME", columns.get(8), row);
            assertFalse(columns.get(1).isBlank(), "missing contributor source: " + row);
            var source = projectRoot().resolve(columns.get(1));
            assertTrue(Files.exists(source), source.toString());
            assertTrue(
                    Files.readString(source).contains("implements CommandContributor"),
                    source.toString()
            );
        }
        assertEquals(153, roots.size());
        assertFalse(roots.contains("essentials"));
    }

    private static Path projectRoot() {
        var current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (current != null && !Files.exists(current.resolve("settings.gradle.kts"))) {
            current = current.getParent();
        }
        return requireNonNull(current, "project root");
    }

    private static List<String> csv(String row) {
        var values = new java.util.ArrayList<String>();
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
    void noLegacyCommandProductionTypeRemains() throws IOException {
        var root = projectRoot();
        for (var forbidden : List.of(
                "CellCommand.java",
                "CommandInvocation.java",
                "CommandSpec.java",
                "LegacyCommandBridge.java",
                "DirectCommandMigrationIndex.java",
                "DefaultCommandSpecFactory.java"
        )) {
            try (var paths = Files.walk(root)) {
                assertTrue(paths.noneMatch(path -> path.getFileName().toString().equals(forbidden)), forbidden);
            }
        }
    }

}
