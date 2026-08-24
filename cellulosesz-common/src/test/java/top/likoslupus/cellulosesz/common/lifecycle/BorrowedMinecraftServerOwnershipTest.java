package top.likoslupus.cellulosesz.common.lifecycle;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

final class BorrowedMinecraftServerOwnershipTest {

    private static final Pattern TRY_WITH_RESOURCES = Pattern.compile(
            "try\\s*\\([^)]*(?:MinecraftServer|ServerLevel|activeServer|requireServer|requireRunning|currentServer)[^)]*\\)"
    );

    private static final Pattern FORBIDDEN_SERVER_CLOSE = Pattern.compile(
            "(?:server|activeServer|currentServer|level|world)\\.close\\s*\\(\\)"
    );

    @Test
    void productionSources_doNotManageBorrowedServerInTryWithResources() throws IOException {
        var root = projectRoot();
        var mainJavaRoots = new HashSet<Path>();

        try (var paths = Files.walk(root)) {
            paths.filter(Files::isDirectory)
                    .filter(path -> path.endsWith(Path.of("src", "main", "java")))
                    .forEach(mainJavaRoots::add);
        }

        assertFalse(mainJavaRoots.isEmpty(), "Expected to find main java source roots");

        var violations = new ArrayList<String>();

        for (var sourceRoot : mainJavaRoots) {
            try (var files = Files.walk(sourceRoot)) {
                files.filter(path -> path.toString().endsWith(".java"))
                        .forEach(file -> {
                            try {
                                var content = Files.readString(file);
                                if (TRY_WITH_RESOURCES.matcher(content).find()) {
                                    violations.add(
                                            "Try-with-resources on borrowed server/level in: "
                                                    + file
                                    );
                                }
                                if (FORBIDDEN_SERVER_CLOSE.matcher(content).find()) {
                                    violations.add(
                                            "Direct close() on borrowed server/level in: " + file
                                    );
                                }
                            } catch (IOException e) {
                                violations.add("Failed to read " + file + ": " + e.getMessage());
                            }
                        });
            }
        }

        assertTrue(
                violations.isEmpty(),
                () -> "Found forbidden borrowed Minecraft object ownership:\n" + String.join(
                        "\n",
                        violations
                )
        );
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

    @Test
    void serverHandle_attachDetachLifecycle_managesStateWithoutClosing() {
        var handle = new MinecraftServerHandle();
        assertTrue(handle.current().isEmpty());
        assertThrows(IllegalStateException.class, handle::requireRunning);
        assertFalse(handle.serverThread());
    }

}
