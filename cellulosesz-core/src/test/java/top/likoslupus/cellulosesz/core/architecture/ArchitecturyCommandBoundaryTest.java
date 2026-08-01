package top.likoslupus.cellulosesz.core.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ArchitecturyCommandBoundaryTest {

    private static final List<String> MODULES = List.of(
            "admin",
            "command",
            "economy",
            "home",
            "item",
            "kit",
            "messaging",
            "playerstate",
            "sign",
            "teleport",
            "text",
            "warp",
            "world"
    );

    @Test
    void commonAndFeatureSourcesContainNoLoaderImports() throws IOException {
        var root = projectRoot();
        for (var source : javaSources(root)) {
            var normalized = source.toString().replace('\\', '/');
            if (!normalized.contains("/src/main/java/")) {
                continue;
            }
            if (normalized.contains("/cellulosesz-fabric/")) {
                continue;
            }
            var text = Files.readString(source);
            assertFalse(text.contains("net.fabricmc."), source.toString());
            assertFalse(text.contains("net.neoforged."), source.toString());
            assertFalse(
                    text.contains("top.likoslupus.cellulosesz.fabric."),
                    source.toString()
            );
        }
    }

    private static Path projectRoot() {
        var current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (current != null && !Files.exists(current.resolve("settings.gradle.kts"))) {
            current = current.getParent();
        }
        return requireNonNull(current, "project root");
    }

    private static List<Path> javaSources(Path root) throws IOException {
        if (!Files.exists(root)) {
            return List.of();
        }
        try (var paths = Files.walk(root)) {
            return paths.filter(path -> path.toString().endsWith(".java")).toList();
        }
    }

    @Test
    void featureCommandsUseNativeTreesAndNoLegacyApi() throws IOException {
        var root = projectRoot();
        for (var module : MODULES) {
            var moduleRoot = root.resolve(
                    "cellulosesz-modules/cellulosesz-module-" + module + "/src/main/java"
            );
            if (!Files.exists(moduleRoot)) {
                continue;
            }
            var combined = new StringBuilder();
            for (var source : javaSources(moduleRoot)) {
                combined.append(Files.readString(source));
            }
            var text = combined.toString();
            assertTrue(text.contains("Commands.literal"), module);
            for (var forbidden : List.of(
                    "CellCommand",
                    "CommandInvocation",
                    "CommandSpec",
                    "CommandRoute",
                    "String[] args",
                    "context.commands()"
            )) {
                assertFalse(text.contains(forbidden), module + ": " + forbidden);
            }
        }
    }

    @Test
    void fabricOwnsNoFeatureCommandTree() throws IOException {
        var fabric = projectRoot().resolve("cellulosesz-fabric/src/main/java");
        for (var source : javaSources(fabric)) {
            var text = Files.readString(source);
            assertFalse(text.contains("CommandRegistrationCallback.EVENT.register"), source.toString());
            assertFalse(text.contains("Commands.literal("), source.toString());
        }
        assertFalse(Files.exists(fabric.resolve("top/likoslupus/cellulosesz/fabric/command")));
    }

    @Test
    void removedArchitectureTypesAreAbsent() throws IOException {
        var root = projectRoot();
        for (var source : javaSources(root)) {
            var normalized = source.toString().replace('\\', '/');
            if (!normalized.contains("/src/main/java/")) {
                continue;
            }
            var text = Files.readString(source);
            for (var forbidden : List.of(
                    "top.likoslupus.cellulosesz.api.platform.PlatformService",
                    "require(PlatformService.class)",
                    "PlatformCapability",
                    "FabricPlatformService",
                    "LegacyCommandBridge",
                    "DirectCommandMigrationIndex",
                    "DefaultCommandSpecFactory",
                    "CommandMigrationMode"
            )) {
                assertFalse(text.contains(forbidden), source + ": " + forbidden);
            }
        }
    }

}
