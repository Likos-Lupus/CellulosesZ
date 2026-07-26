package top.likoslupus.cellulosesz.core.config;

import top.likoslupus.cellulosesz.api.config.ConfigRegistry;
import top.likoslupus.cellulosesz.api.logging.CellulosesZLogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
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
    public synchronized <T> T register(
            String key,
            Class<T> type,
            String relativePath,
            Supplier<T> defaultSupplier
    ) {
        var definition = new ConfigDefinition<>(
                key,
                type,
                root.resolve(relativePath),
                defaultSupplier
        );
        definitions.put(key, definition);
        try {
            T value = load(definition);
            values.put(key, value);
            return value;
        } catch (IOException exception) {
            definitions.remove(key);
            throw configurationFailure(definition, exception);
        }
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
    public void reload() {
        commit(prepareReload());
    }

    public synchronized void commit(ReloadSnapshot snapshot) {
        if (!snapshot.values().keySet().equals(definitions.keySet())) {
            throw new IllegalStateException("Configuration definitions changed while reload was being prepared");
        }
        values.clear();
        values.putAll(snapshot.values());
    }

    /**
     * Reads and validates every registered configuration without publishing it. This method performs file I/O and must
     * not run on the Minecraft server thread.
     */
    public ReloadSnapshot prepareReload() {
        List<ConfigDefinition<?>> currentDefinitions;
        synchronized (this) {
            currentDefinitions = List.copyOf(definitions.values());
        }

        var reloaded = new LinkedHashMap<String, Object>();
        try {
            for (var definition : currentDefinitions) {
                reloaded.put(definition.key(), loadUnknown(definition));
            }
        } catch (IOException exception) {
            logger.error("Configuration reload failed; the previous complete configuration remains active.", exception);
            throw new IllegalStateException("Configuration reload failed", exception);
        }
        return new ReloadSnapshot(Map.copyOf(reloaded));
    }

    private Object loadUnknown(ConfigDefinition<?> definition) throws IOException {
        return load(definition);
    }

    private <T> T load(ConfigDefinition<T> definition) throws IOException {
        Files.createDirectories(definition.path().getParent());
        if (Files.notExists(definition.path())) {
            T defaultValue = definition.defaultSupplier().get();
            JacksonCodecs.writeYaml(definition.path(), defaultValue);
            return defaultValue;
        }
        return JacksonCodecs.readYaml(definition.path(), definition.type());
    }

    private IllegalStateException configurationFailure(
            ConfigDefinition<?> definition,
            IOException exception
    ) {
        logger.error("Failed to load configuration %s at %s".formatted(definition.key(), definition.path()), exception);
        return new IllegalStateException("Failed to load configuration " + definition.key(), exception);
    }

    public synchronized ReloadSnapshot snapshot() {
        return new ReloadSnapshot(Map.copyOf(values));
    }

    public synchronized void restore(ReloadSnapshot snapshot) {
        values.clear();
        values.putAll(snapshot.values());
    }

    public record ReloadSnapshot(Map<String, Object> values) {

        public ReloadSnapshot {
            values = Map.copyOf(values);
        }

        public <T> T require(String key, Class<T> type) {
            var value = values.get(key);
            if (value == null) throw new IllegalStateException("Configuration is not registered: " + key);
            return type.cast(value);
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
