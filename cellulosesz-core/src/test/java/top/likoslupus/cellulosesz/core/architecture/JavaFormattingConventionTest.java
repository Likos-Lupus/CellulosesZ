package top.likoslupus.cellulosesz.core.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class JavaFormattingConventionTest {

    private static final Pattern SINGLE_LINE_TYPE = Pattern.compile(
            "(?m)^\\s*(?:public\\s+)?(?:final\\s+)?(?:class|record|enum|interface)\\s+[^\\n{]+\\{[^\\n]+}\\s*$"
    );

    @Test
    void sourcePackagesAreNullMarkedAndFormatterBypassesAreForbidden() throws IOException {
        var root = projectRoot();
        try (var paths = Files.walk(root)) {
            for (var source : paths.filter(path -> path.toString().endsWith(".java")).toList()) {
                if (source.toString().contains("/build/") || source.toString().contains("\\build\\")) {
                    continue;
                }
                var text = Files.readString(source);
                assertFalse(text.contains("spotless:" + "off"), source.toString());
                assertFalse(text.contains("@formatter:" + "off"), source.toString());
                assertFalse(SINGLE_LINE_TYPE.matcher(text).find(), source.toString());
            }
        }

        try (var directories = Files.walk(root)) {
            for (var directory : directories.filter(Files::isDirectory).toList()) {
                var normalized = directory.toString().replace('\\', '/');
                if (!normalized.contains("/src/main/java/") && !normalized.contains("/src/test/java/")) {
                    continue;
                }
                try (var children = Files.list(directory)) {
                    if (children.noneMatch(path -> path.toString().endsWith(".java"))) {
                        continue;
                    }
                }
                var packageInfo = directory.resolve("package-info.java");
                assertTrue(Files.exists(packageInfo), directory.toString());
                assertTrue(Files.readString(packageInfo).contains("@NullMarked"), packageInfo.toString());
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

}
