package top.likoslupus.cellulosesz.core.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class NullMarkedPackagesTest {

    @Test
    void mainSourcePackages_areNullMarked() throws IOException {
        var root = projectRoot();
        var sourceRoots = new HashSet<Path>();
        try (var paths = Files.walk(root)) {
            paths
                    .filter(Files::isDirectory)
                    .filter(path -> path.endsWith(Path.of("src", "main", "java")))
                    .forEach(sourceRoots::add);
        }

        for (var sourceRoot : sourceRoots) {
            Set<Path> packages = new HashSet<>();
            try (var files = Files.walk(sourceRoot)) {
                files.filter(path -> path.toString().endsWith(".java"))
                        .filter(path -> !path.getFileName().toString().equals("package-info.java"))
                        .map(Path::getParent)
                        .forEach(packages::add);
            }

            for (var packageDirectory : packages) {
                var packageInfo = packageDirectory.resolve("package-info.java");
                assertTrue(Files.isRegularFile(packageInfo), () -> "Missing " + packageInfo);
                var source = Files.readString(packageInfo);
                assertTrue(
                        source.contains("@NullMarked"),
                        () -> "Missing @NullMarked in " + packageInfo
                );
                assertFalse(
                        source.contains("@NullUnmarked"),
                        () -> "@NullUnmarked is forbidden in " + packageInfo
                );
                assertTrue(
                        !source.contains("@SuppressWarnings(\"nullness\")")
                                && !source.contains("@SuppressWarnings({\"nullness\""),
                        () -> "Package-wide nullness suppression is forbidden in " + packageInfo
                );
            }
        }
    }

    private static Path projectRoot() {
        var current = Path.of("").toAbsolutePath().normalize();
        while (current != null && !Files.isRegularFile(current.resolve("settings.gradle.kts"))) {
            current = current.getParent();
        }
        if (current == null) {
            throw new IllegalStateException("Unable to locate project root");
        }
        return current;
    }

}
