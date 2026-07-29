package top.likoslupus.cellulosesz.core.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AllMainPackagesAreNullMarkedTest {

    @Test
    void everyHandWrittenMainPackageIsNullMarked() throws IOException {
        var failures = inspect(projectRoot());
        assertTrue(failures.isEmpty(), () -> "NullMarked package violations:\n" + String.join("\n", failures));
    }

    private static List<String> inspect(Path root) throws IOException {
        var failures = new ArrayList<String>();
        for (var sourceRoot : sourceRoots(root)) {
            try (var directories = Files.walk(sourceRoot)) {
                for (var directory : directories.filter(Files::isDirectory).toList()) {
                    try (var children = Files.list(directory)) {
                        var ordinaryJava = children.anyMatch(path ->
                                path.getFileName().toString().endsWith(".java")
                                        && !path.getFileName().toString().equals("package-info.java")
                        );
                        if (!ordinaryJava) continue;
                    }
                    var packageInfo = directory.resolve("package-info.java");
                    if (!Files.exists(packageInfo)) {
                        failures.add("missing package-info.java: " + root.relativize(directory));
                        continue;
                    }
                    var text = Files.readString(packageInfo);
                    if (!text.contains("@NullMarked"))
                        failures.add("missing @NullMarked: " + root.relativize(packageInfo));
                }
            }
            try (var sources = Files.walk(sourceRoot)) {
                sources.filter(path -> path.toString().endsWith(".java")).forEach(path -> {
                    try {
                        var text = Files.readString(path);
                        if (text.contains("@NullUnmarked"))
                            failures.add("@NullUnmarked is forbidden: " + root.relativize(path));
                        if (text.contains("@SuppressWarnings(\"nullness\")"))
                            failures.add("blanket nullness suppression is forbidden: " + root.relativize(path));
                    } catch (IOException failure) {
                        throw new AssertionError(failure);
                    }
                });
            }
        }
        return List.copyOf(failures);
    }

    private static Path projectRoot() {
        var current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (current != null && !Files.exists(current.resolve("settings.gradle.kts"))) {
            current = current.getParent();
        }
        return requireNonNull(current, "project root");
    }

    private static List<Path> sourceRoots(Path root) throws IOException {
        try (var paths = Files.walk(root)) {
            return paths.filter(path -> path.endsWith("src/main/java"))
                    .filter(path -> !path.toString().contains("/build/"))
                    .filter(path -> !path.toString().contains("\\build\\"))
                    .toList();
        }
    }

    @Test
    void fixtureWithoutPackageInfoFails() throws IOException {
        var root = Files.createTempDirectory("cellulosesz-nullmarked-missing");
        var source = root.resolve("sample/src/main/java/example/missing");
        Files.createDirectories(source);
        Files.writeString(source.resolve("Example.java"), "package example.missing; final class Example {}\n");
        assertTrue(inspect(root).stream().anyMatch(value -> value.contains("example/missing")));
    }

    @Test
    void fixtureWithoutAnnotationFails() throws IOException {
        var root = Files.createTempDirectory("cellulosesz-nullmarked-bad");
        var source = root.resolve("sample/src/main/java/example/bad");
        Files.createDirectories(source);
        Files.writeString(source.resolve("Example.java"), "package example.bad; final class Example {}\n");
        Files.writeString(source.resolve("package-info.java"), "package example.bad;\n");
        assertTrue(inspect(root).stream().anyMatch(value -> value.contains("@NullMarked")));
    }

}
