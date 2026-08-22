package top.likoslupus.cellulosesz.core.config;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;
import tools.jackson.module.kotlin.KotlinModule;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.file.*;

public final class JacksonCodecs {

    private static final YAMLMapper YAML = YAMLMapper.builder()
            .addModule(new KotlinModule.Builder().build())
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();
    private static final JsonMapper JSON = JsonMapper.builder()
            .addModule(new KotlinModule.Builder().build())
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    private JacksonCodecs() {
    }

    public static <T> T readYaml(Path path, Class<T> type) throws IOException {
        try {
            return YAML.readValue(path.toFile(), type);
        } catch (RuntimeException exception) {
            throw new IOException("Failed to read YAML: " + path, exception);
        }
    }

    public static <T> T readYaml(InputStream input, Class<T> type) throws IOException {
        try {
            return YAML.readValue(input, type);
        } catch (RuntimeException exception) {
            throw new IOException("Failed to read YAML resource", exception);
        }
    }

    public static void writeYaml(Path path, Object value) throws IOException {
        writeAtomically(
                path,
                output -> YAML.writerWithDefaultPrettyPrinter().writeValue(output, value)
        );
    }

    private static void writeAtomically(Path path, StreamWriter writer) throws IOException {
        var target = path.toAbsolutePath().normalize();
        var parent = target.getParent();

        if (parent == null) {
            throw new IOException("Document path has no parent: " + target);
        }

        Files.createDirectories(parent);
        var temporary = Files.createTempFile(parent, "." + target.getFileName() + ".", ".tmp");
        var moved = false;

        try {
            try (
                    var output = Files.newOutputStream(
                            temporary,
                            StandardOpenOption.WRITE,
                            StandardOpenOption.TRUNCATE_EXISTING
                    )
            ) {
                writer.write(output);
                output.flush();
            } catch (RuntimeException exception) {
                throw new IOException("Failed to encode document: " + target, exception);
            }

            try (var channel = FileChannel.open(temporary, StandardOpenOption.WRITE)) {
                channel.force(true);
            }

            try {
                Files.move(
                        temporary,
                        target,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING
                );
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }

            moved = true;
            forceDirectory(parent);
        } finally {
            if (!moved) {
                Files.deleteIfExists(temporary);
            }
        }
    }

    private static void forceDirectory(Path directory) {
        try (var channel = FileChannel.open(directory, StandardOpenOption.READ)) {
            channel.force(true);
        } catch (IOException | UnsupportedOperationException _) {
            // The data file itself has already been forced. Some filesystems do not allow opening
            // directories.
        }
    }

    public static <T> T readJson(Path path, Class<T> type) throws IOException {
        try {
            return JSON.readValue(path.toFile(), type);
        } catch (RuntimeException exception) {
            throw new IOException("Failed to read JSON: " + path, exception);
        }
    }

    public static <T> T readJson(String value, Class<T> type) throws IOException {
        try {
            return JSON.readValue(value, type);
        } catch (RuntimeException exception) {
            throw new IOException("Failed to read JSON value", exception);
        }
    }

    public static String writeJsonString(Object value) {
        return JSON.writeValueAsString(value);
    }

    public static <T> T deepCopy(T value, Class<T> type) {
        try {
            var encoded = JSON.writeValueAsBytes(value);
            return JSON.readValue(encoded, type);
        } catch (RuntimeException exception) {
            throw new IllegalStateException(
                    "Failed to deep-copy configuration type " + type.getName(),
                    exception
            );
        }
    }

    public static void writeJson(Path path, Object value) throws IOException {
        writeAtomically(
                path,
                output -> JSON.writerWithDefaultPrettyPrinter().writeValue(output, value)
        );
    }

    public static String toDebugString(Object value) {
        try {
            return YAML.writeValueAsString(value);
        } catch (RuntimeException _) {
            return String.valueOf(value);
        }
    }

    @FunctionalInterface
    private interface StreamWriter {

        void write(OutputStream output) throws IOException;

    }

}
