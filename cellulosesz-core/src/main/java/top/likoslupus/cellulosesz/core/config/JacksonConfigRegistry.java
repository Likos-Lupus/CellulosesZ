package top.likoslupus.cellulosesz.core.config;

import top.likoslupus.cellulosesz.api.config.ConfigRegistry;
import top.likoslupus.cellulosesz.api.logging.CellulosesZLogger;
import top.likoslupus.cellulosesz.api.service.Registration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static top.likoslupus.cellulosesz.api.validation.Checks.requireNonBlank;

import static java.util.Objects.requireNonNull;

public final class JacksonConfigRegistry implements ConfigRegistry {

    private final Path root;
    private final CellulosesZLogger logger;
    private final Map<String, ConfigDefinition<?>> definitions = new LinkedHashMap<>();
    private final Map<String, Object> values = new LinkedHashMap<>();
    private final Map<Path, ConfigDefinition<?>> paths = new LinkedHashMap<>();
    private long definitionSequence;

    public JacksonConfigRegistry(
            Path root,
            CellulosesZLogger logger
    ) {
        this.root = requireNonNull(root, "root");
        this.logger = requireNonNull(logger, "logger");
    }

    @Override
    public synchronized Registration register(
            String key,
            Class<?> type,
            String relativePath,
            Supplier<?> defaultSupplier,
            String owner
    ) {
        requireNonBlank(key, "Configuration key");
        requireNonNull(type, "type");
        requireNonNull(defaultSupplier, "defaultSupplier");
        requireNonBlank(owner, "owner");

        if (definitions.containsKey(key)) {
            throw new IllegalStateException("Configuration key is already registered: " + key);
        }

        var relative = Path.of(relativePath);
        if (relative.isAbsolute()) {
            throw new IllegalArgumentException(
                    "Configuration path must be relative: " + relativePath
            );
        }

        var normalizedRoot = root.toAbsolutePath().normalize();
        var resolved = normalizedRoot.resolve(relative).normalize();
        if (!resolved.startsWith(normalizedRoot)) {
            throw new IllegalArgumentException(
                    "Configuration path escapes the configuration root: " + relativePath
            );
        }

        var existingPath = paths.get(resolved);
        if (existingPath != null) {
            throw new IllegalStateException(
                    "Configuration path is already registered by %s: %s"
                            .formatted(existingPath.key(), relativePath)
            );
        }

        var definition = new ConfigDefinition<>(
                key,
                type,
                resolved,
                defaultSupplier,
                owner,
                definitionSequence++
        );

        try {
            var value = loadUnknown(definition);
            definitions.put(key, definition);
            paths.put(resolved, definition);
            values.put(key, value);
            return new ConfigRegistration(definition);
        } catch (IOException exception) {
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
        if (!snapshot.definitionVersions().equals(definitionVersions())) {
            throw new IllegalStateException(
                    "Configuration definitions changed while reload was being prepared"
            );
        }

        values.clear();
        values.putAll(snapshot.values());
    }

    /**
     * Reads and validates every registered configuration without publishing it. This method
     * performs file I/O and must not run on the Minecraft server thread.
     */
    public ReloadSnapshot prepareReload() {
        List<ConfigDefinition<?>> currentDefinitions;
        Map<String, Long> versions;
        synchronized (this) {
            currentDefinitions = List.copyOf(definitions.values());
            versions = definitionVersions();
        }

        var reloaded = new LinkedHashMap<String, Object>();
        try {
            for (var definition : currentDefinitions) {
                reloaded.put(definition.key(), loadUnknown(definition));
            }
        } catch (IOException exception) {
            logger.error(
                    "Configuration reload failed; the previous complete configuration remains active.",
                    exception
            );
            throw new IllegalStateException("Configuration reload failed", exception);
        }

        return new ReloadSnapshot(Map.copyOf(reloaded), versions);
    }

    private synchronized Map<String, Long> definitionVersions() {
        var versions = new LinkedHashMap<String, Long>();
        definitions.forEach((key, definition) ->
                versions.put(key, definition.version())
        );
        return Map.copyOf(versions);
    }

    private Object loadUnknown(ConfigDefinition<?> definition) throws IOException {
        return load(definition);
    }

    private IllegalStateException configurationFailure(
            ConfigDefinition<?> definition,
            IOException exception
    ) {
        logger.error(
                "Failed to load configuration %s at %s".formatted(
                        definition.key(),
                        definition.path()
                ),
                exception
        );
        return new IllegalStateException(
                "Failed to load configuration " + definition.key(),
                exception
        );
    }

    private <T> T load(ConfigDefinition<T> definition) throws IOException {
        Files.createDirectories(definition.path().getParent());
        if (Files.notExists(definition.path())) {
            var defaultValue = definition.type().cast(definition.defaultSupplier().get());
            JacksonCodecs.writeYaml(definition.path(), defaultValue);

            return defaultValue;
        }

        return JacksonCodecs.readYaml(definition.path(), definition.type());
    }

    public synchronized ReloadSnapshot snapshot() {
        return new ReloadSnapshot(Map.copyOf(values), definitionVersions());
    }

    public synchronized void restore(ReloadSnapshot snapshot) {
        definitions.forEach((key, _) -> {
            var value = snapshot.values().get(key);
            if (value != null) {
                values.put(key, value);
            }
        });
    }

    private synchronized void remove(ConfigDefinition<?> definition) {
        if (definitions.remove(definition.key(), definition)) {
            values.remove(definition.key());
            paths.remove(definition.path(), definition);
        }
    }

    public record ReloadSnapshot(
            Map<String, Object> values,
            Map<String, Long> definitionVersions
    ) {

        public ReloadSnapshot {
            values = Map.copyOf(values);
            definitionVersions = Map.copyOf(definitionVersions);
        }

        public <T> T require(String key, Class<T> type) {
            var value = values.get(key);
            if (value == null) {
                throw new IllegalStateException("Configuration is not registered: " + key);
            }

            return type.cast(value);
        }

    }

    private record ConfigDefinition<T>(
            String key,
            Class<T> type,
            Path path,
            Supplier<?> defaultSupplier,
            String owner,
            long version
    ) {

    }

    private final class ConfigRegistration implements Registration {

        private final ConfigDefinition<?> definition;
        private final AtomicBoolean closed = new AtomicBoolean();

        private ConfigRegistration(ConfigDefinition<?> definition) {
            this.definition = definition;
        }

        @Override
        public String owner() {
            return definition.owner();
        }

        @Override
        public boolean closed() {
            return closed.get();
        }

        @Override
        public void close() {
            if (closed.compareAndSet(false, true)) {
                remove(definition);
            }
        }

    }

}
