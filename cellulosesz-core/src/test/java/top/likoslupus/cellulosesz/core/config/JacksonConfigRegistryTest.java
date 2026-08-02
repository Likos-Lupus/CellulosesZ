package top.likoslupus.cellulosesz.core.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import top.likoslupus.cellulosesz.api.logging.CellulosesZLogger;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

final class JacksonConfigRegistryTest {

    @TempDir Path root;

    @Test
    void duplicateKeysAndPathsFailFast() {
        var registry = new JacksonConfigRegistry(root, new NoopLogger());
        registry.register(
                "one",
                Document.class,
                "one.yml",
                Document::new
        );
        assertThrows(
                IllegalStateException.class,
                () -> registry.register(
                        "one",
                        Document.class,
                        "two.yml",
                        Document::new
                )
        );
        assertThrows(
                IllegalStateException.class,
                () -> registry.register(
                        "two",
                        Document.class,
                        "one.yml",
                        Document::new
                )
        );
    }

    @Test
    void closedRegistrationAllowsIdentitySafeReregistrationAndInvalidatesPreparedReload() {
        var registry = new JacksonConfigRegistry(root, new NoopLogger());
        var first = registry.register(
                "one",
                Document.class,
                "one.yml",
                Document::new,
                "module"
        );
        var prepared = registry.prepareReload();

        first.close();
        var replacement = registry.register(
                "one",
                Document.class,
                "one.yml",
                Document::new,
                "module"
        );
        first.close();

        assertThrows(IllegalStateException.class, () -> registry.commit(prepared));
        registry.require("one", Document.class);
        replacement.close();
    }

    @Test
    void pathsCannotEscapeRoot() {
        var registry = new JacksonConfigRegistry(root, new NoopLogger());
        assertThrows(
                IllegalArgumentException.class,
                () -> registry.register(
                        "escape",
                        Document.class,
                        "../escape.yml",
                        Document::new
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> registry.register(
                        "absolute",
                        Document.class,
                        root.resolve("absolute.yml").toString(),
                        Document::new
                )
        );
    }

    @Test
    void coreCommandCostsDoNotShareReferencesAcrossSnapshots() {
        var registry = new JacksonConfigRegistry(root, new NoopLogger());
        registry.register(
                "core",
                CoreConfig.class,
                "core.yml",
                CoreConfig::new
        );

        var current = registry.require("core", CoreConfig.class);
        current.commands.costs.put("home", new BigDecimal("2.50"));
        var previous = registry.snapshot();

        current.commands.costs.put("home", new BigDecimal("8.00"));
        current.commands.costs.put("warp", BigDecimal.ONE);

        var snapshotted = previous.require("core", CoreConfig.class);
        assertEquals(new BigDecimal("2.50"), snapshotted.commands.costs.get("home"));
        assertFalse(snapshotted.commands.costs.containsKey("warp"));

        registry.restore(previous);
        registry.finishRestore();
        var restored = registry.require("core", CoreConfig.class);
        assertEquals(new BigDecimal("2.50"), restored.commands.costs.get("home"));
        assertFalse(restored.commands.costs.containsKey("warp"));
    }

    @Test
    void snapshotsAndCommitsDeepCopyNestedMutableValues() {
        var registry = new JacksonConfigRegistry(root, new NoopLogger());
        registry.register(
                "nested",
                NestedDocument.class,
                "nested.yml",
                NestedDocument::new
        );

        var current = registry.require("nested", NestedDocument.class);
        current.values.add("before");
        current.groups.put("letters", new ArrayList<>(List.of("a")));
        current.flags.add("old");

        var previous = registry.snapshot();
        current.values.add("current-only");
        current.groups.get("letters").add("b");
        current.flags.add("current");

        var snapshotValue = previous.require("nested", NestedDocument.class);
        assertEquals(List.of("before"), snapshotValue.values);
        assertEquals(List.of("a"), snapshotValue.groups.get("letters"));
        assertEquals(Set.of("old"), snapshotValue.flags);

        snapshotValue.values.add("prepared-only");
        snapshotValue.groups.get("letters").add("prepared");
        snapshotValue.flags.add("prepared");
        registry.commit(previous);

        var committed = registry.require("nested", NestedDocument.class);
        assertEquals(List.of("before"), committed.values);
        assertEquals(List.of("a"), committed.groups.get("letters"));
        assertEquals(Set.of("old"), committed.flags);

        committed.values.add("new-runtime");
        committed.groups.get("letters").add("new-runtime");
        committed.flags.add("new-runtime");
        registry.restore(previous);
        registry.finishRestore();

        var restored = registry.require("nested", NestedDocument.class);
        assertEquals(List.of("before"), restored.values);
        assertEquals(List.of("a"), restored.groups.get("letters"));
        assertEquals(Set.of("old"), restored.flags);
    }

    public static final class Document {

        public String value = "";

    }

    public static final class NestedDocument {

        public List<String> values = new ArrayList<>();
        public Map<String, List<String>> groups = new LinkedHashMap<>();
        public Set<String> flags = new LinkedHashSet<>();

    }

    private static final class NoopLogger implements CellulosesZLogger {

        @Override
        public void warn(String message) {
        }

        @Override
        public void error(String message) {
        }

        @Override
        public void error(String message, Throwable throwable) {
        }

        @Override
        public void info(String message) {
        }

    }

}
