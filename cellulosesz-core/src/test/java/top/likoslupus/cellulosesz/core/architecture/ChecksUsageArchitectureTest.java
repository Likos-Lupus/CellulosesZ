package top.likoslupus.cellulosesz.core.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ChecksUsageArchitectureTest {

    @Test
    void structuredRequestsUseSharedChecksAndNoPrivateCloneHelpers() throws IOException {
        var root = projectRoot();
        var api = root.resolve("cellulosesz-api/src/main/java");
        var usageCount = 0;
        for (var source : javaSources(api)) {
            var text = Files.readString(source);
            usageCount += occurrences(text, "Checks.require");
            assertFalse(text.matches("(?s).*private\\s+static.*(requirePositive|validateNonNegative|requireRange).*"), source.toString());
        }
        assertTrue(usageCount >= 40, "expected broad production use of Checks");
    }

    private static Path projectRoot() {
        var current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (current != null && !Files.exists(current.resolve("settings.gradle.kts"))) {
            current = current.getParent();
        }
        return requireNonNull(current, "project root");
    }

    private static List<Path> javaSources(Path root) throws IOException {
        try (var paths = Files.walk(root)) {
            return paths.filter(path -> path.toString().endsWith(".java")).toList();
        }
    }

    private static int occurrences(String text, String token) {
        var count = 0;
        var index = 0;
        while ((index = text.indexOf(token, index)) >= 0) {
            count++;
            index += token.length();
        }
        return count;
    }

    @Test
    void formatterBypassMarkersAreAbsent() throws IOException {
        for (var source : javaSources(projectRoot())) {
            var text = Files.readString(source);
            assertFalse(text.contains("spotless:" + "off"), source.toString());
            assertFalse(text.contains("@formatter:" + "off"), source.toString());
        }
    }

}
