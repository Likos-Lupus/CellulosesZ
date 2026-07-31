package top.likoslupus.cellulosesz.core.architecture;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.*;

final class ArchitecturyCommandBoundaryTest {

    private static final Set<String> DIRECT = Set.of(
            "afk", "back", "balance", "balancetop", "ban", "banip", "bottom", "broadcast",
            "broadcastworld", "burn", "cellulosesz", "compass", "createkit", "customtext", "delhome",
            "deljail", "delkit", "delwarp", "depth", "eco", "exp", "ext", "feed", "fly", "gamemode",
            "getpos", "god", "heal", "help", "helpop", "home", "ice", "ignore", "info", "jail",
            "jailedplayers", "jails", "jump", "kick", "kickall", "kill", "kit", "kitreset", "list", "mail",
            "me", "motd", "msg", "msgtoggle", "mute", "near", "nick", "pay", "payconfirmtoggle",
            "paytoggle", "ping", "playtime", "ptime", "pweather", "r", "realname", "renamehome", "rest",
            "rtoggle", "rules", "seen", "sell", "sethome", "setjail", "settpr", "setwarp", "setworth",
            "showkit", "socialspy", "speed", "sudo", "suicide", "tempban", "tempbanip", "top", "tp", "tpa",
            "tpaall", "tpacancel", "tpaccept", "tpahere", "tpall", "tpauto", "tpdeny", "tphere", "tpo",
            "tpoffline", "tpohere", "tppos", "tpr", "tptoggle", "unban", "unbanip", "vanish", "warp",
            "warpinfo", "whois", "world", "worth"
    );

    private static final List<String> COMMON_MODULES = List.of(
            "text", "home", "warp", "kit", "command", "messaging", "economy", "playerstate", "admin", "teleport"
    );

    private static final List<String> SEGMENT_2_MODULES = List.of(
            "command", "messaging", "economy", "playerstate"
    );

    private static final List<String> SEGMENT_3_MODULES = List.of("admin", "teleport");

    @Test
    void pureAndCommonProjectsContainNoLoaderImports() throws IOException {
        var root = projectRoot();
        var sourceRoots = new ArrayList<String>();

        sourceRoots.add("cellulosesz-api/src/main/java");
        sourceRoots.add("cellulosesz-core/src/main/java");
        sourceRoots.add("cellulosesz-common/src/main/java");

        COMMON_MODULES.forEach(domain ->
                sourceRoots.add("cellulosesz-modules/cellulosesz-module-%s/src/main/java".formatted(domain))
        );

        for (var relative : sourceRoots) {
            for (var source : javaSources(root.resolve(relative))) {
                var text = Files.readString(source);
                assertFalse(text.contains("net.fabricmc."), source.toString());
                assertFalse(text.contains("net.neoforged."), source.toString());
                assertFalse(text.contains("top.likoslupus.cellulosesz.fabric."), source.toString());
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
            DIRECT.forEach(command -> assertFalse(
                    text.contains("Commands.literal(\"" + command + "\")"), source.toString()));
        }
        assertFalse(Files.exists(fabric.resolve("top/likoslupus/cellulosesz/fabric/command")));
    }

    @Test
    void moduleCommandsUseDirectTreesAndNoLegacyApi() throws IOException {
        var root = projectRoot();
        for (var domain : COMMON_MODULES) {
            var commandRoot = commandSourceRoot(root, domain);
            var combined = new StringBuilder();
            for (var source : javaSources(commandRoot)) {
                combined.append(Files.readString(source));
            }
            var text = combined.toString();

            assertTrue(text.contains("Commands.literal"), domain);
            List.of(
                    "CommandSpec",
                    "CommandRoute",
                    "CommandParameter",
                    "CellCommand",
                    "CommandInvocation",
                    "DefaultCommandSpecFactory",
                    "String[] args",
                    "invocation.args()"
            ).forEach(forbidden ->
                    assertFalse(
                            text.contains(forbidden),
                            domain + ": " + forbidden
                    )
            );
        }
    }

    private static Path commandSourceRoot(Path root, String domain) {
        var base = root.resolve("cellulosesz-modules/cellulosesz-module-%s/src/main/java/top/likoslupus/cellulosesz/modules/%s".formatted(domain, domain));
        return domain.equals("command")
                ? base
                : base.resolve("command");
    }

    @Test
    void segmentTwoModulesDoNotDependOnBroadPlatformService() throws IOException {
        var root = projectRoot();
        for (var domain : SEGMENT_2_MODULES) {
            var sourceRoot = root.resolve("cellulosesz-modules/cellulosesz-module-%s/src".formatted(domain));
            for (var source : javaSources(sourceRoot)) {
                var text = Files.readString(source);
                assertFalse(text.contains("import top.likoslupus.cellulosesz.api.platform.PlatformService;"), source.toString());
                assertFalse(text.contains("require(PlatformService.class)"), source.toString());
            }
        }
    }

    @Test
    void segmentTwoCommandNamesAreNormalBusinessNames() throws IOException {
        var root = projectRoot();
        for (var domain : SEGMENT_2_MODULES) {
            var commandRoot = commandSourceRoot(root, domain);
            for (var source : javaSources(commandRoot)) {
                var name = source.getFileName().toString();
                List.of("Brigadier", "Registrar", "Direct", "V2", "Legacy")
                        .forEach(rejected -> assertFalse(name.contains(rejected), name));
            }
        }
    }

    @Test
    void commonManagerIsFeatureBlindAndArchitecturyOwnsRegistration() throws IOException {
        var root = projectRoot();
        var manager = Files.readString(root.resolve("cellulosesz-common/src/main/java/top/likoslupus/cellulosesz/common/command/CommandManager.java"));
        var common = Files.readString(root.resolve("cellulosesz-common/src/main/java/top/likoslupus/cellulosesz/common/CellulosesZCommon.java"));
        COMMON_MODULES.forEach(module -> {
            assertFalse(manager.contains("modules." + module));
            assertFalse(common.contains("modules." + module));
        });
        assertTrue(manager.contains("registry.freezeAndSnapshot()"));
        assertTrue(common.contains("CommandRegistrationEvent.EVENT.register"));
        assertFalse(common.contains("CommandRegistrationCallback"));
    }

    @Test
    void directRootIndexHasExpectedCardinality() throws IOException {
        assertEquals(104, DIRECT.size());
        var index = Files.readString(projectRoot().resolve("cellulosesz-core/src/main/java/top/likoslupus/cellulosesz/core/command/DirectCommandMigrationIndex.java"));
        DIRECT.forEach(command -> assertTrue(index.contains("\"" + command + "\""), command));
    }

    @Test
    void segmentTwoLegacyClassesAreDeletedAndContributorCountIsExact() throws IOException {
        var root = projectRoot();
        var contributors = 0;
        for (var domain : SEGMENT_2_MODULES) {
            var commandRoot = commandSourceRoot(root, domain);
            for (var source : javaSources(commandRoot)) {
                if (Files.readString(source).contains("implements CommandContributor")) contributors++;
            }
        }
        assertEquals(45, contributors);
        Stream.of(
                        "cellulosesz-modules/cellulosesz-module-messaging/src/main/java/top/likoslupus/cellulosesz/modules/messaging/command/AbstractMessagingCommand.java",
                        "cellulosesz-modules/cellulosesz-module-economy/src/main/java/top/likoslupus/cellulosesz/modules/economy/command/AbstractEconomyCommand.java",
                        "cellulosesz-modules/cellulosesz-module-playerstate/src/main/java/top/likoslupus/cellulosesz/modules/playerstate/command/AbstractPlayerStateCommand.java",
                        "cellulosesz-modules/cellulosesz-module-command/src/main/java/top/likoslupus/cellulosesz/modules/command/RootCellulosesZCommand.java"
                )
                .map(s -> Files.exists(root.resolve(s)))
                .forEach(Assertions::assertFalse);
    }

    @Test
    void segmentTwoPersonalWorldStateUsesTypedSettingsOutsideStorageAdapter() throws IOException {
        var root = projectRoot();
        var moduleRoot = root.resolve("cellulosesz-modules/cellulosesz-module-playerstate/src/main/java/top/likoslupus/cellulosesz/modules/playerstate");
        for (var source : javaSources(moduleRoot)) {
            var normalized = source.toString().replace('\\', '/');
            if (normalized.endsWith("/service/DefaultPlayerStateService.java")) {
                continue;
            }

            var text = Files.readString(source);
            Arrays.asList(
                            "state.personalTime",
                            "state.personalWeather",
                            "@Nullable Long",
                            "@Nullable String"
                    )
                    .forEach(s -> assertFalse(text.contains(s), source.toString()));
        }
    }

    @Test
    void segmentTwoModulesAreArchitecturyCommonAndFabricTransformsThem() throws IOException {
        var root = projectRoot();
        var fabricBuild = Files.readString(root.resolve("cellulosesz-fabric/build.gradle.kts"));
        for (var domain : SEGMENT_2_MODULES) {
            var modulePath = ":cellulosesz-modules:cellulosesz-module-" + domain;
            var build = Files.readString(root.resolve("cellulosesz-modules/cellulosesz-module-%s/build.gradle.kts".formatted(domain)));

            Arrays.asList(
                            "alias(libs.plugins.architectury.loom.no.remap)",
                            "alias(libs.plugins.architectury.plugin)",
                            "common(\"fabric\", \"neoforge\")",
                            "implementation(project(\":cellulosesz-common\"))"
                    )
                    .forEach(s -> assertTrue(build.contains(s), domain));

            assertFalse(build.contains("cellulosesz-fabric"), domain);
            assertTrue(fabricBuild.contains("\"" + modulePath + "\""), domain);
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

    @Test
    void segmentThreeModulesUseNarrowPortsAndDirectCommandTrees() throws IOException {
        var root = projectRoot();
        for (var domain : SEGMENT_3_MODULES) {
            var sourceRoot = root.resolve("cellulosesz-modules/cellulosesz-module-%s/src/main/java".formatted(domain));
            var contributors = 0;
            for (var source : javaSources(sourceRoot)) {
                var text = Files.readString(source);
                assertFalse(text.contains("net.fabricmc."), source.toString());
                assertFalse(text.contains("net.neoforged."), source.toString());
                assertFalse(text.contains("top.likoslupus.cellulosesz.fabric."), source.toString());
                assertFalse(text.contains("import top.likoslupus.cellulosesz.api.platform.PlatformService;"), source.toString());
                assertFalse(text.contains("require(PlatformService.class)"), source.toString());
                assertFalse(text.contains("CellCommand"), source.toString());
                assertFalse(text.contains("CommandInvocation"), source.toString());
                assertFalse(text.contains("CommandSpec"), source.toString());
                assertFalse(text.contains("CommandRoute"), source.toString());
                assertFalse(text.contains("String[] args"), source.toString());
                assertFalse(text.contains("nativeHandle()"), source.toString());
                if (text.contains("implements CommandContributor")) contributors++;
            }
            assertEquals(domain.equals("admin") ? 20 : 22, contributors, domain);
        }
    }

    @Test
    void segmentThreeCommandNamesAreNormalBusinessNames() throws IOException {
        var root = projectRoot();
        for (var domain : SEGMENT_3_MODULES) {
            for (var source : javaSources(commandSourceRoot(root, domain))) {
                var name = source.getFileName().toString();
                List.of("Brigadier", "Registrar", "Direct", "V2", "Legacy")
                        .forEach(rejected -> assertFalse(name.contains(rejected), name));
            }
        }
    }

}
