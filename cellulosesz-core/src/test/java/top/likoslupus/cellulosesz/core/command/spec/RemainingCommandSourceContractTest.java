package top.likoslupus.cellulosesz.core.command.spec;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.*;

final class RemainingCommandSourceContractTest {

    private static final List<String> ROOTS = List.of(
            "antioch", "beezooka", "book", "break", "bigtree", "burn", "clearinventory",
            "clearinventoryconfirmtoggle", "condense", "disposal", "ext",
            "fireball", "gc", "hat", "ice", "itemdb", "kill", "kittycannon", "lightning",
            "more", "nuke", "powertoollist", "powertooltoggle", "recipe",
            "editsign", "skull", "spawner", "spawnmob", "stonecutter", "sudo", "suicide", "thunder", "tree"
    );

    @Test
    void allRootsHaveSourceRegistrationAndCelluloseszPermission() throws IOException {
        var root = projectRoot();
        var source = readMainJava(root);
        ROOTS.forEach(command -> {
            if (command.equals("disposal") || command.equals("stonecutter")) {
                assertTrue(source.contains("new WorkstationCommand(platform, \"" + command + "\""), command);
            } else {
                assertTrue(source.contains("return \"" + command + "\";"), command);
                assertTrue(source.contains("cellulosesz.command." + command), command);
            }
            assertTrue(source.contains("\"" + command + "\""), "spec/registration token: " + command);
        });
        assertFalse(source.contains("cellulosesz.command.essentials"));
        assertFalse(source.contains("essentials.command."));
    }

    private static Path projectRoot() {
        var current = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        while (current != null && !Files.exists(current.resolve("settings.gradle.kts"))) current = current.getParent();
        return requireNonNull(current, "Project root not found");
    }

    private static String readMainJava(Path root) throws IOException {
        var builder = new StringBuilder();
        try (var files = Files.walk(root)) {
            files.filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> path.toString().contains("/src/main/java/"))
                    .forEach(path -> {
                        try {
                            builder.append(Files.readString(path)).append('\n');
                        } catch (IOException failure) {
                            throw new java.io.UncheckedIOException(failure);
                        }
                    });
        }
        return builder.toString();
    }

    @Test
    void commandMatrixHasNoMissingRowsAndOneExplicitExclusion() throws IOException {
        var lines = Files.readAllLines(projectRoot().resolve("CellulosesZ-Essentials-command-matrix.csv"));
        assertEquals(154, lines.size());
        var missing = 0;
        var excluded = 0;
        var excludedCommand = "";
        for (var index = 1; index < lines.size(); index++) {
            var fields = csvFields(lines.get(index));
            if (fields.size() < 4) continue;
            if (fields.get(3).equals("MISSING")) missing++;
            if (fields.get(3).equals("EXCLUDED")) {
                excluded++;
                excludedCommand = fields.get(1);
            }
        }
        assertEquals(0, missing);
        assertEquals(1, excluded);
        assertEquals("essentials", excludedCommand);
    }

    private static List<String> csvFields(String line) {
        var result = new java.util.ArrayList<String>();
        var field = new StringBuilder();
        var quoted = false;
        for (var index = 0; index < line.length(); index++) {
            var current = line.charAt(index);
            if (current == '"') {
                if (quoted && index + 1 < line.length() && line.charAt(index + 1) == '"') {
                    field.append('"');
                    index++;
                } else quoted = !quoted;
            } else if (current == ',' && !quoted) {
                result.add(field.toString());
                field.setLength(0);
            } else field.append(current);
        }
        result.add(field.toString());
        return result;
    }

    @Test
    void newAliasesDoNotMechanicallyCopyEssentialsEPrefixes() throws IOException {
        var source = readMainJava(projectRoot());
        for (var command : ROOTS) {
            assertFalse(source.contains("\"e" + command + "\""), command);
        }
        assertTrue(source.contains("List.of(\"pong\")"));
        assertTrue(source.contains("List.of(\"ci\", \"clearinv\")"));
        assertTrue(source.contains("new WorkstationCommand(platform, \"disposal\", List.of(\"trash\")"));
    }

}
