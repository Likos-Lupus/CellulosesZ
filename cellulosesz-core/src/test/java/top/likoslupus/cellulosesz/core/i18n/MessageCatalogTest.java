package top.likoslupus.cellulosesz.core.i18n;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.*;

final class MessageCatalogTest {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([^{}]+)}");
    private static final List<Pattern> LITERAL_KEY_CALLS = List.of(
            Pattern.compile("(?:replyKey|errorKey)\\s*\\(\\s*\"([^\"]+)\""),
            Pattern.compile("SignUseResult\\.(?:success|failure)\\s*\\(\\s*\"([^\"]+)\""),
            Pattern.compile("AdminResult\\.(?:success|failure)\\s*\\(\\s*\"([^\"]+)\""),
            Pattern.compile("MessageRef\\.of\\s*\\(\\s*\"([^\"]+)\""),
            Pattern.compile("renderer\\.render\\s*\\([^,]+,\\s*\"([^\"]+)\"")
    );

    @Test
    void englishAndChineseKeysAndPlaceholderOrderMatch() {
        var english = BuiltInCommandMessages.english();
        var chinese = BuiltInCommandMessages.chinese();
        assertEquals(english.keySet(), chinese.keySet());
        english.forEach((key, value) -> assertEquals(
                placeholders(value), placeholders(chinese.get(key)), key));
    }

    private static java.util.List<String> placeholders(String text) {
        var result = new ArrayList<String>();
        var matcher = PLACEHOLDER.matcher(text);
        while (matcher.find()) result.add(matcher.group(1));
        return result;
    }

    @Test
    void catalogsContainNoDevelopmentStageNames() {
        BuiltInCommandMessages.english().forEach((key, value) -> {
            var combined = (key + " " + value).toLowerCase();
            assertFalse(combined.contains("stagee"));
            assertFalse(combined.contains("stage e"));
            assertFalse(combined.contains("phase-e"));
            assertFalse(combined.contains("stagef"));
            assertFalse(combined.contains("stage f"));
            assertFalse(combined.contains("阶段 f"));
        });
    }

    @Test
    void literalRuntimeMessageKeysExist() throws IOException {
        var catalog = BuiltInCommandMessages.english();
        var missing = new HashSet<String>();
        try (var files = Files.walk(projectRoot())) {
            files.filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> path.toString().contains("/src/main/java/"))
                    .forEach(path -> collectMissing(path, catalog.keySet(), missing));
        }
        assertTrue(missing.isEmpty(), "Missing literal message keys: " + missing);
    }

    private static Path projectRoot() {
        var current = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        while (current != null && !Files.exists(current.resolve("settings.gradle.kts"))) {
            current = current.getParent();
        }
        return requireNonNull(current, "Project root not found");
    }

    private static void collectMissing(
            Path source,
            Set<String> catalog,
            Set<String> missing
    ) {
        final String text;
        try {
            text = Files.readString(source);
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
        LITERAL_KEY_CALLS.stream()
                .map(pattern -> pattern.matcher(text))
                .forEach(matcher -> {
                    while (matcher.find()) {
                        var key = matcher.group(1);
                        if (!catalog.contains(key)) missing.add(key);
                    }
                });
    }

}
