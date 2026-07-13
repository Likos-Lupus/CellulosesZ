package top.likoslupus.cellulosesz.core.config;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;

import java.io.IOException;
import java.nio.file.Path;

public final class JacksonCodecs {

    private static final YAMLMapper YAML = YAMLMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();
    private static final JsonMapper JSON = JsonMapper.builder()
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

    public static void writeYaml(Path path, Object value) throws IOException {
        try {
            YAML.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), value);
        } catch (RuntimeException exception) {
            throw new IOException("Failed to write YAML: " + path, exception);
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

    public static void writeJson(Path path, Object value) throws IOException {
        try {
            JSON.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), value);
        } catch (RuntimeException exception) {
            throw new IOException("Failed to write JSON: " + path, exception);
        }
    }

    public static String toDebugString(Object value) {
        try {
            return YAML.writeValueAsString(value);
        } catch (RuntimeException exception) {
            return String.valueOf(value);
        }
    }

}
