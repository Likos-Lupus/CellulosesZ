package top.likoslupus.cellulosesz.core.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ArchitecturyCommandBoundaryTest {

    private static final Set<String> DIRECT = Set.of(
            "customtext", "info", "motd", "rules", "home", "sethome", "delhome", "renamehome",
            "warp", "setwarp", "delwarp", "warpinfo", "kit", "showkit", "createkit", "delkit", "kitreset"
    );

    @Test
    void pureAndCommonProjectsContainNoLoaderImports() throws IOException {
        var root = projectRoot();
        for (var relative : List.of("cellulosesz-api/src/main/java", "cellulosesz-core/src/main/java",
                "cellulosesz-common/src/main/java",
                "cellulosesz-modules/cellulosesz-module-text/src/main/java",
                "cellulosesz-modules/cellulosesz-module-home/src/main/java",
                "cellulosesz-modules/cellulosesz-module-warp/src/main/java",
                "cellulosesz-modules/cellulosesz-module-kit/src/main/java")) {
            for (var source : javaSources(root.resolve(relative))) {
                var text = Files.readString(source);
                assertFalse(text.contains("net.fabricmc."), source.toString());
                assertFalse(text.contains("net.neoforged."), source.toString());
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

    private static List<Path> javaSources(Path root) throws IOException {
        if (!Files.exists(root)) return List.of();
        try (var paths = Files.walk(root)) {
            return paths.filter(path -> path.toString().endsWith(".java")).toList();
        }
    }

    @Test
    void fabricOwnsNoFeatureTreeOrCommandCallback() throws IOException {
        var root = projectRoot();
        var fabric = root.resolve("cellulosesz-fabric/src/main/java");
        for (var source : javaSources(fabric)) {
            var text = Files.readString(source);
            assertFalse(text.contains("CommandRegistrationCallback.EVENT.register"), source.toString());
            List.of(
                    "TextCommandRegistrar",
                    "HomeCommandRegistrar",
                    "WarpCommandRegistrar",
                    "KitCommandRegistrar"
            ).forEach(rejected ->
                    assertFalse(text.contains(rejected), source.toString())
            );
            DIRECT.forEach(command ->
                    assertFalse(
                            text.contains("Commands.literal(\"" + command + "\")"),
                            source.toString()
                    )
            );
        }
        assertFalse(Files.exists(fabric.resolve("top/likoslupus/cellulosesz/fabric/command")));
    }

    @Test
    void moduleCommandsUseDirectBrigadierAndNoLegacyApi() throws IOException {
        var root = projectRoot();
        for (var domain : List.of("text", "home", "warp", "kit")) {
            var commandRoot = root.resolve("cellulosesz-modules/cellulosesz-module-" + domain
                    + "/src/main/java/top/likoslupus/cellulosesz/modules/" + domain + "/command");
            var combined = new StringBuilder();
            for (var source : javaSources(commandRoot)) combined.append(Files.readString(source));
            var text = combined.toString();
            assertTrue(text.contains("Commands.literal"), domain);
            List.of(
                    "CommandSpec",
                    "CommandRoute",
                    "CommandParameter",
                    "CellCommand",
                    "CommandInvocation",
                    "DefaultCommandSpecFactory",
                    "String[] args"
            ).forEach(forbidden ->
                    assertFalse(
                            text.contains(forbidden),
                            domain + ": " + forbidden
                    )
            );
        }
    }

    @Test
    void commonManagerIsFeatureBlindAndArchitecturyOwnsRegistration() throws IOException {
        var root = projectRoot();
        var manager = Files.readString(root.resolve("cellulosesz-common/src/main/java/top/likoslupus/cellulosesz/common/command/CommandManager.java"));
        var common = Files.readString(root.resolve("cellulosesz-common/src/main/java/top/likoslupus/cellulosesz/common/CellulosesZCommon.java"));
        List.of(
                "modules.text",
                "modules.home",
                "modules.warp",
                "modules.kit"
        ).forEach(module -> {
            assertFalse(manager.contains(module));
            assertFalse(common.contains(module));
        });
        assertTrue(manager.contains("registry.freezeAndSnapshot()"));
        assertTrue(common.contains("CommandRegistrationEvent.EVENT.register"));
        assertFalse(common.contains("CommandRegistrationCallback"));
    }

    @Test
    void legacyBoundaryExcludesMigratedRootsAndDoesNotLeakInvocation() throws IOException {
        var root = projectRoot();
        var factory = Files.readString(root.resolve(
                "cellulosesz-core/src/main/java/top/likoslupus/cellulosesz/core/command/spec/DefaultCommandSpecFactory.java"));
        DIRECT.forEach(command -> assertFalse(factory.contains("case \"" + command + "\""), command));
        for (var source : javaSources(root)) {
            var relative = root.relativize(source).toString().replace('\\', '/');
            var text = Files.readString(source);
            if (text.contains("MinecraftLegacyCommandInvocation")) {
                assertTrue(relative.contains("common/command/legacy/"), relative);
            }
        }
    }

    @Test
    void architecturyVersionsAndNoRemapArePinned() throws IOException {
        var root = projectRoot();
        var catalog = Files.readString(root.resolve("gradle/libs.versions.toml"));
        assertTrue(catalog.contains("architectury-api = \"20.0.9\""));
        assertTrue(catalog.contains("architectury-plugin = \"3.5.169\""));
        assertTrue(catalog.contains("dev.architectury.loom-no-remap"));
        assertFalse(catalog.contains("architectury-api = \"21."));
    }

}
