package top.likoslupus.cellulosesz.core.config;

import top.likoslupus.cellulosesz.api.config.ConfigRegistry;
import top.likoslupus.cellulosesz.api.config.ConfigSnapshot;
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
    private final Map<String, Object> restorationOverrides = new LinkedHashMap<>();
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
                    "Configuration path is already registered by %s: %s".formatted(
                            existingPath.key(),
                            relativePath
                    )
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
            var restored = restorationOverrides.remove(key);
            var value = restored == null
                    ? loadUnknown(definition)
                    : deepCopy(definition, restored);

            definitions.put(key, definition);
            paths.put(resolved, definition);
            values.put(key, deepCopy(definition, value));

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

    private Object loadUnknown(ConfigDefinition<?> definition) throws IOException {
        return load(definition);
    }

    private Object deepCopy(ConfigDefinition<?> definition, Object value) {
        return deepCopyUnknown(value, definition.type());
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

    private static <T> T deepCopyUnknown(Object value, Class<T> type) {
        return JacksonCodecs.deepCopy(type.cast(value), type);
    }

    public synchronized void commit(ReloadSnapshot snapshot) {
        requireNonNull(snapshot, "snapshot");
        if (!snapshot.definitionVersions.equals(definitionVersions())) {
            throw new IllegalStateException(
                    "Configuration definitions changed while reload was being prepared"
            );
        }

        restorationOverrides.clear();
        values.clear();
        definitions.forEach((key, definition) -> values.put(
                key,
                deepCopy(definition, snapshot.value(key))
        ));
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
                reloaded.put(
                        definition.key(),
                        deepCopy(definition, loadUnknown(definition))
                );
            }
        } catch (IOException | RuntimeException exception) {
            logger.error(
                    "Configuration reload failed; the previous complete configuration remains active.",
                    exception
            );
            throw new IllegalStateException("Configuration reload failed", exception);
        }

        return new ReloadSnapshot(reloaded, versions);
    }

    public synchronized ReloadSnapshot snapshot() {
        var copied = new LinkedHashMap<String, Object>();
        definitions.forEach((key, definition) -> copied.put(
                key,
                deepCopy(definition, values.get(key))
        ));

        return new ReloadSnapshot(copied, definitionVersions());
    }

    private synchronized Map<String, Long> definitionVersions() {
        var versions = new LinkedHashMap<String, Long>();
        definitions.forEach((key, definition) ->
                versions.put(key, definition.version())
        );

        return Map.copyOf(versions);
    }

    public synchronized void restore(ReloadSnapshot snapshot) {
        requireNonNull(snapshot, "snapshot");
        var currentValues = new LinkedHashMap<>(values);

        restorationOverrides.clear();
        restorationOverrides.putAll(snapshot.values);
        values.clear();

        definitions.forEach((key, definition) -> {
            var value = snapshot.optionalValue(key);
            if (value.isPresent()) {
                values.put(key, deepCopy(definition, value.orElseThrow()));
                restorationOverrides.remove(key);
                return;
            }

            var current = currentValues.get(key);
            if (current != null) {
                values.put(key, deepCopy(definition, current));
            }
        });
    }

    public synchronized void finishRestore() {
        restorationOverrides.clear();
    }

    private synchronized void remove(ConfigDefinition<?> definition) {
        if (definitions.remove(definition.key(), definition)) {
            values.remove(definition.key());
            paths.remove(definition.path(), definition);
        }
    }

    public static final class ReloadSnapshot implements ConfigSnapshot {

        private final Map<String, Object> values;
        private final Map<String, Long> definitionVersions;

        private ReloadSnapshot(
                Map<String, Object> values,
                Map<String, Long> definitionVersions
        ) {
            this.values = Map.copyOf(values);
            this.definitionVersions = Map.copyOf(definitionVersions);
        }

        @Override
        public <T> T require(String key, Class<T> type) {
            var value = values.get(key);
            if (value == null) {
                throw new IllegalStateException("Configuration is not registered: " + key);
            }

            return JacksonCodecs.deepCopy(type.cast(value), type);
        }

        @Override
        public <T> Optional<T> optional(String key, Class<T> type) {
            var value = values.get(key);
            if (value == null) {
                return Optional.empty();
            }

            return Optional.of(JacksonCodecs.deepCopy(type.cast(value), type));
        }

        private Object value(String key) {
            var value = values.get(key);
            if (value == null) {
                throw new IllegalStateException("Configuration is not registered: " + key);
            }

            return value;
        }

        private Optional<Object> optionalValue(String key) {
            return Optional.ofNullable(values.get(key));
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
