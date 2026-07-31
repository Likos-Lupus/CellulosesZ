package top.likoslupus.cellulosesz.core.command.spec;

import org.junit.jupiter.api.Test;
import top.likoslupus.cellulosesz.core.command.DirectCommandMigrationIndex;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.*;

final class Segment3LedgerConsistencyTest {

    @Test
    void implementationLedgersContainOneHundredFiftyThreeCanonicalRoots() throws IOException {
        var root = projectRoot();
        var migration = rows(root.resolve("docs/refactor-command-migration.csv"));
        var contract = rows(root.resolve("docs/refactor-command-contract.csv"));

        assertEquals(153, migration.size());
        assertEquals(153, contract.size());
        assertEquals(153, migration.stream().map(row -> row.getFirst()).distinct().count());
        assertEquals(153, contract.stream().map(row -> row.getFirst()).distinct().count());

        var statuses = migration.stream().collect(Collectors.groupingBy(
                row -> row.get(8), Collectors.counting()));
        assertEquals(104L, statuses.get("MIGRATED_COMMON_UNVERIFIED_RUNTIME").longValue());
        assertEquals(49L, statuses.get("NOT_MIGRATED").longValue());
        assertEquals(104, DirectCommandMigrationIndex.roots().size());
    }

    private static Path projectRoot() {
        var current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (current != null && !Files.exists(current.resolve("settings.gradle.kts"))) {
            current = current.getParent();
        }
        return requireNonNull(current, "project root");
    }

    private static List<List<String>> rows(Path path) throws IOException {
        return Files.readAllLines(path).stream().skip(1).filter(line -> !line.isBlank())
                .map(Segment3LedgerConsistencyTest::splitCsvLine).toList();
    }

    /**
     * The ledgers do not contain embedded newlines; this parser handles their quoted commas.
     */
    private static List<String> splitCsvLine(String line) {
        var fields = new java.util.ArrayList<String>();
        var field = new StringBuilder();
        var quoted = false;
        for (var index = 0; index < line.length(); index++) {
            var character = line.charAt(index);
            if (character == '"') {
                if (quoted && index + 1 < line.length() && line.charAt(index + 1) == '"') {
                    field.append('"');
                    index++;
                } else {
                    quoted = !quoted;
                }
            } else if (character == ',' && !quoted) {
                fields.add(field.toString());
                field.setLength(0);
            } else {
                field.append(character);
            }
        }
        fields.add(field.toString());
        return List.copyOf(fields);
    }

    @Test
    void implementationAndReferenceRootDifferencesAreExplicit() throws IOException {
        var root = projectRoot();
        var migration = byRoot(rows(root.resolve("docs/refactor-command-migration.csv")));
        var contract = byRoot(rows(root.resolve("docs/refactor-command-contract.csv")));
        var reference = byColumn(rows(root.resolve("CellulosesZ-Essentials-command-matrix.csv")), 1);

        assertTrue(reference.containsKey("essentials"));
        assertFalse(migration.containsKey("essentials"));
        assertTrue(migration.containsKey("cellulosesz"));
        assertTrue(contract.containsKey("cellulosesz"));

        assertTrue(reference.containsKey("togglejail"));
        assertFalse(migration.containsKey("togglejail"));
        assertTrue(migration.containsKey("jail"));
        assertTrue(migration.get("jail").get(4).contains("togglejail"));

        assertTrue(migration.containsKey("tpa"));
        assertTrue(migration.containsKey("tpahere"));
        assertTrue(contract.containsKey("tpa"));
        assertTrue(contract.containsKey("tpahere"));
    }

    private static Map<String, List<String>> byRoot(List<List<String>> rows) {
        return byColumn(rows, 0);
    }

    private static Map<String, List<String>> byColumn(List<List<String>> rows, int column) {
        return rows.stream().collect(Collectors.toUnmodifiableMap(row -> row.get(column), Function.identity()));
    }

}
