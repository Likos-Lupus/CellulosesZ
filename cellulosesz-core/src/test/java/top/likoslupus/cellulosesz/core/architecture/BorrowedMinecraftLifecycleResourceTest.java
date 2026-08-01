package top.likoslupus.cellulosesz.core.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static java.util.Objects.requireNonNull;

final class BorrowedMinecraftLifecycleResourceTest {

    private static final Pattern TRY_HEADER = Pattern.compile(
            "\\btry\\s*\\((.*?)\\)\\s*\\{",
            Pattern.DOTALL
    );
    private static final Pattern NON_CODE = Pattern.compile(
            "\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*'|//[^\\r\\n]*|/\\*.*?\\*/",
            Pattern.DOTALL
    );
    private static final Pattern BORROW_ACCESSOR = Pattern.compile(
            "\\b(?:activeServer|requireServer|requireRunning)\\s*\\(|\\.\\s*level\\s*\\("
    );
    private static final Pattern CURRENT_HANDLE_BORROW = Pattern.compile(
            "\\bcurrent\\s*\\.\\s*orElseThrow\\s*\\("
    );
    private static final Pattern BORROWED_TYPE = Pattern.compile(
            "\\b(?:MinecraftServer|ServerLevel)\\b[^=;]*="
    );

    @Test
    void borrowedMinecraftLifecycleObjectsAreNotTryResources() throws IOException {
        var root = projectRoot();
        var failures = new ArrayList<String>();
        for (var source : productionJavaSources(root)) {
            var text = Files.readString(source);
            tryResources(text).stream()
                    .filter(resource -> isBorrowedMinecraftResource(text, resource))
                    .map(resource -> "%s: %s".formatted(
                            root.relativize(source).toString().replace('\\', '/'),
                            normalize(resource)
                    ))
                    .forEach(failures::add);
        }

        assertTrue(
                failures.isEmpty(),
                () -> "Borrowed Minecraft lifecycle objects used as try resources:\n"
                        + String.join("\n", failures)
        );
    }

    private static Path projectRoot() {
        var current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (current != null
                && !Files.exists(current.resolve("settings.gradle.kts"))
        ) {
            current = current.getParent();
        }
        return requireNonNull(current, "project root");
    }

    private static List<Path> productionJavaSources(Path root) throws IOException {
        try (var paths = Files.walk(root)) {
            return paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".java"))
                    .filter(path -> {
                        var normalized = path.toString().replace('\\', '/');
                        return normalized.contains("/src/main/java/")
                                && !normalized.contains("/build/")
                                && !normalized.contains("/generated/");
                    })
                    .toList();
        }
    }

    private static List<String> tryResources(String source) {
        var resources = new ArrayList<String>();
        var matcher = TRY_HEADER.matcher(codeOnly(source));
        while (matcher.find()) {
            resources.addAll(splitResources(matcher.group(1)));
        }
        return List.copyOf(resources);
    }

    private static boolean isBorrowedMinecraftResource(String source, String resource) {
        return BORROW_ACCESSOR.matcher(resource).find()
                || BORROWED_TYPE.matcher(resource).find()
                || source.contains("MinecraftServerHandle")
                && CURRENT_HANDLE_BORROW.matcher(resource).find();
    }

    private static String normalize(String resource) {
        return resource.replaceAll("\\s+", " ").strip();
    }

    private static String codeOnly(String source) {
        return NON_CODE.matcher(source).replaceAll(result -> " ".repeat(result.group().length()));
    }

    private static List<String> splitResources(String header) {
        var result = new ArrayList<String>();
        var start = 0;
        var parentheses = 0;
        var brackets = 0;
        var braces = 0;
        for (var index = 0; index < header.length(); index++) {
            switch (header.charAt(index)) {
                case '(' -> parentheses++;
                case ')' -> parentheses--;
                case '[' -> brackets++;
                case ']' -> brackets--;
                case '{' -> braces++;
                case '}' -> braces--;
                case ';' -> {
                    if (parentheses == 0 && brackets == 0 && braces == 0) {
                        addResource(result, header.substring(start, index));
                        start = index + 1;
                    }
                }
                default -> {
                }
            }
        }
        addResource(result, header.substring(start));
        return result;
    }

    private static void addResource(List<String> result, String resource) {
        var normalized = resource.strip();
        if (!normalized.isEmpty()) {
            result.add(normalized);
        }
    }

    @Test
    void banAndBackupSourcesKeepTargetedLifecycleGuards() throws IOException {
        var root = projectRoot();
        var banSource = Files.readString(root.resolve(
                "cellulosesz-fabric/src/main/java/top/likoslupus/cellulosesz/fabric/FabricBanPlatformService.java"
        ));
        assertTrue(
                tryResources(banSource)
                        .stream()
                        .noneMatch(resource -> resource.contains("activeServer(")),
                "FabricBanPlatformService must borrow the active server without closing it"
        );

        var backupSource = Files.readString(root.resolve(
                "cellulosesz-fabric/src/main/java/top/likoslupus/cellulosesz/fabric/FabricBackupOperations.java"
        ));
        var backupResources = tryResources(backupSource);
        assertTrue(
                backupResources.stream().noneMatch(resource -> resource.contains("requireServer(")),
                "FabricBackupOperations must borrow the active server without closing it"
        );
        assertTrue(
                backupResources
                        .stream()
                        .anyMatch(resource -> resource.contains("Files.newOutputStream(")),
                "Backup archive output stream must remain scoped and closed"
        );
        assertTrue(
                backupResources
                        .stream()
                        .anyMatch(resource -> resource.contains("new ZipOutputStream(")),
                "Backup archive zip stream must remain scoped and closed"
        );
        assertTrue(
                backupResources.stream().anyMatch(resource -> resource.contains("Files.walk(")),
                "Backup archive path stream must remain scoped and closed"
        );
        assertFalse(
                backupSource.contains("try (var server = access.requireServer())"),
                "Backup server access must not regress to the original dangerous form"
        );
    }

}
