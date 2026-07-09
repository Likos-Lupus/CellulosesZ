package top.likoslupus.cellulosesz.core.config;

import top.likoslupus.cellulosesz.api.config.ConfigRegistry;
import top.likoslupus.cellulosesz.api.logging.CellulosesZLogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public final class JacksonConfigRegistry implements ConfigRegistry {

    private final Path root;
    private final CellulosesZLogger logger;
    private final Map<String, ConfigDefinition<?>> definitions = new LinkedHashMap<>();
    private final Map<String, Object> values = new LinkedHashMap<>();

    public JacksonConfigRegistry(
            Path root,
            CellulosesZLogger logger
    ) {
        this.root = root;
        this.logger = logger;
    }

    @Override
    @SuppressWarnings("unchecked")
    public synchronized <T> T register(
            String key,
            Class<T> type,
            String relativePath,
            Supplier<T> defaultSupplier
    ) {
        definitions.put(key, new ConfigDefinition<>(key, type, root.resolve(relativePath), defaultSupplier));
        T value = (T) load(definitions.get(key));
        values.put(key, value);
        return value;
    }

    @Override
    public synchronized <T> T require(String key, Class<T> type) {
        var value = values.get(key);
        if (value == null) {
            throw new IllegalStateException("Configuration is not registered: " + key);
        }
        return type.cast(value);
    }

    @Override
    public synchronized <T> Optional<T> optional(String key, Class<T> type) {
        var value = values.get(key);
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(type.cast(value));
    }

    @Override
    public synchronized void reload() {
        values.clear();
        definitions.values().forEach(definition -> {
            var value = load(definition);
            values.put(definition.key(), value);
        });
    }

    private <T> T load(ConfigDefinition<T> definition) {
        try {
            Files.createDirectories(definition.path().getParent());
            if (Files.notExists(definition.path())) {
                T defaultValue = definition.defaultSupplier().get();
                JacksonCodecs.writeYaml(definition.path(), defaultValue);
                return defaultValue;
            }
            return JacksonCodecs.readYaml(definition.path(), definition.type());
        } catch (IOException exception) {
            logger.error("Failed to load configuration %s at %s".formatted(definition.key(), definition.path()), exception);
            return definition.defaultSupplier().get();
        }
    }

    private record ConfigDefinition<T>(
            String key,
            Class<T> type,
            Path path,
            Supplier<T> defaultSupplier
    ) {

    }

}
