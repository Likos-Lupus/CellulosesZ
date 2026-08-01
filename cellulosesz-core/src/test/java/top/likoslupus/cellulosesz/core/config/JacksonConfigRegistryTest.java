package top.likoslupus.cellulosesz.core.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import top.likoslupus.cellulosesz.api.logging.CellulosesZLogger;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;

final class JacksonConfigRegistryTest {

    @TempDir Path root;

    @Test
    void duplicateKeysAndPathsFailFast() {
        var registry = new JacksonConfigRegistry(root, new NoopLogger());
        registry.register("one", Document.class, "one.yml", Document::new);
        assertThrows(IllegalStateException.class,
                () -> registry.register("one", Document.class, "two.yml", Document::new));
        assertThrows(IllegalStateException.class,
                () -> registry.register("two", Document.class, "one.yml", Document::new));
    }

    @Test
    void pathsCannotEscapeRoot() {
        var registry = new JacksonConfigRegistry(root, new NoopLogger());
        assertThrows(IllegalArgumentException.class,
                () -> registry.register("escape", Document.class, "../escape.yml", Document::new));
        assertThrows(IllegalArgumentException.class,
                () -> registry.register("absolute", Document.class, root.resolve("absolute.yml")
                        .toString(), Document::new));
    }

    public static final class Document {

        public String value = "";

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
