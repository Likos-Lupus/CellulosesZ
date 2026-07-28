package top.likoslupus.cellulosesz.core.i18n;

import org.junit.jupiter.api.Test;
import top.likoslupus.cellulosesz.core.config.JacksonCodecs;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MessageCatalogTest {

    private static final List<Pattern> LITERAL_KEY_CALLS = List.of(
            Pattern.compile("(?:replyKey|errorKey)\\s*\\(\\s*\"([^\"]+)\""),
            Pattern.compile("SignUseResult\\.(?:success|failure)\\s*\\(\\s*\"([^\"]+)\""),
            Pattern.compile("AdminResult\\.(?:success|failure)\\s*\\(\\s*\"([^\"]+)\""),
            Pattern.compile("MessageRef\\.of\\s*\\(\\s*\"([^\"]+)\""),
            Pattern.compile("renderer\\.render\\s*\\([^,]+,\\s*\"([^\"]+)\"")
    );

    @Test
    void englishAndChineseKeysAndPlaceholdersMatch() throws IOException {
        var english = catalog("en_us");
        var chinese = catalog("zh_cn");
        assertEquals(english.keySet(), chinese.keySet());
        english.forEach((key, value) -> assertEquals(
                placeholders(value), placeholders(chinese.get(key)), key));
    }

    @Test
    void keysAndPlaceholdersAreSemantic() throws IOException {
        var numericKey = Pattern.compile("\\.(?:error|reply)\\.\\d+$");
        var positionalPlaceholder = Pattern.compile("value\\d+");
        catalog("en_us").forEach((key, value) -> {
            assertFalse(numericKey.matcher(key).find(), key);
            placeholders(value).forEach(placeholder ->
                    assertFalse(positionalPlaceholder.matcher(placeholder).matches(), key)
            );
        });
    }

    private static Set<String> placeholders(String text) {
        return MessageTemplatePlaceholders.names(text);
    }

    @Test
    void chineseCatalogContainsNoMechanicalFallbackText() throws IOException {
        catalog("zh_cn").forEach((key, value) -> {
            assertFalse(value.contains("操作失败："), key);
            assertFalse(value.contains("操作成功："), key);
            assertFalse(value.matches(".*[A-Za-z]{4,}\\s+(?:invalid|failed|success).*"), key);
        });
    }

    @Test
    void catalogsContainNoDevelopmentStageNames() throws IOException {
        catalog("en_us").forEach((key, value) -> {
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
        var catalog = catalog("en_us");
        var missing = new HashSet<String>();
        try (var files = Files.walk(projectRoot())) {
            files.filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> path.toString().contains("/src/main/java/"))
                    .forEach(path -> collectMissing(path, catalog.keySet(), missing));
        }
        assertTrue(missing.isEmpty(), "Missing literal message keys: " + missing);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> catalog(String locale) throws IOException {
        var resource = "/messages/" + locale + ".yml";
        try (var input = requireNonNull(
                MessageCatalogTest.class.getResourceAsStream(resource),
                "Missing test resource " + resource
        )) {
            Map<String, Object> raw = JacksonCodecs.readYaml(input, Map.class);
            var result = new LinkedHashMap<String, String>();
            raw.forEach((key, value) -> result.put(key, String.valueOf(value)));
            return Map.copyOf(result);
        }
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
