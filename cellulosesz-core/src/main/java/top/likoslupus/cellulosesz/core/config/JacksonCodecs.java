package top.likoslupus.cellulosesz.core.config;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;

import java.io.IOException;
import java.nio.file.Path;

public final class JacksonCodecs {

    private static final YAMLMapper YAML = YAMLMapper.builder().build();
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private JacksonCodecs() {
    }

    public static ObjectMapper yaml() {
        return YAML;
    }

    public static ObjectMapper json() {
        return JSON;
    }

    public static <T> T readYaml(Path path, Class<T> type) throws IOException {
        return YAML.readValue(path.toFile(), type);
    }

    public static void writeYaml(Path path, Object value) throws IOException {
        YAML.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), value);
    }

    public static <T> T readJson(Path path, Class<T> type) throws IOException {
        return JSON.readValue(path.toFile(), type);
    }

    public static void writeJson(Path path, Object value) throws IOException {
        JSON.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), value);
    }

    public static String toDebugString(Object value) {
        try {
            return YAML.writeValueAsString(value);
        } catch (RuntimeException exception) {
            return String.valueOf(value);
        }
    }

}
