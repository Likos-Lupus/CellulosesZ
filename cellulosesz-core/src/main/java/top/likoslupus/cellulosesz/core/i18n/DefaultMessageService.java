package top.likoslupus.cellulosesz.core.i18n;

import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import top.likoslupus.cellulosesz.api.i18n.MessageService;
import top.likoslupus.cellulosesz.api.logging.CellulosesZLogger;
import top.likoslupus.cellulosesz.api.text.LocalizedMessage;
import top.likoslupus.cellulosesz.api.text.MessageRenderer;
import top.likoslupus.cellulosesz.api.text.RichText;
import top.likoslupus.cellulosesz.core.config.JacksonCodecs;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;

public final class DefaultMessageService implements MessageService, MessageRenderer {

    private static final String RESOURCE_ROOT = "/messages/";
    private static final Pattern HEX = Pattern.compile("^#[0-9a-fA-F]{6}$");
    private static final Pattern PLACEHOLDER_NAME = Pattern.compile("^[a-z0-9_-]+$");

    private final Path directory;
    private final CellulosesZLogger logger;
    private final Map<String, Map<String, Object>> packagedDefaults;
    private final MiniMessage miniMessage;
    private volatile RuntimeState state = new RuntimeState(
            Map.of(),
            "zh_cn",
            "en_us",
            "#55FF55",
            "#FFFF55",
            true
    );

    public DefaultMessageService(
            Path directory,
            CellulosesZLogger logger
    ) {
        this.directory = directory;
        this.logger = logger;
        this.packagedDefaults = loadPackagedDefaults();
        this.miniMessage = MiniMessage.builder()
                .tags(TagResolver.builder()
                        .resolver(StandardTags.color())
                        .resolver(StandardTags.decorations())
                        .resolver(StandardTags.reset())
                        .build())
                .build();
    }

    private Map<String, Map<String, Object>> loadPackagedDefaults() {
        var loaded = new LinkedHashMap<String, Map<String, Object>>();
        loaded.put("en_us", loadPackaged("en_us"));
        loaded.put("zh_cn", loadPackaged("zh_cn"));
        return Map.copyOf(loaded);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> loadPackaged(String localeName) {
        var resource = RESOURCE_ROOT + localeName + ".yml";
        try (var input = resource(resource)) {
            Map<String, Object> raw = JacksonCodecs.readYaml(input, Map.class);
            return Map.copyOf(raw);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load packaged message catalog " + resource, exception);
        }
    }

    private InputStream resource(String path) throws IOException {
        var input = DefaultMessageService.class.getResourceAsStream(path);
        if (input == null) throw new IOException("Missing packaged message resource: " + path);
        return input;
    }

    private static Optional<String> lookup(RuntimeState state, String requestedLocale, String key) {
        var requested = messages(state, requestedLocale).get(key);
        if (requested != null) return Optional.of(requested);

        var configured = messages(state, state.locale()).get(key);
        if (configured != null) return Optional.of(configured);

        return Optional.ofNullable(messages(state, state.fallback()).get(key));
    }

    public synchronized void locales(String locale, String fallback) {
        var current = state;
        state = new RuntimeState(
                current.locales(),
                normalizeLocaleValue(locale, current.locale()),
                normalizeLocaleValue(fallback, current.fallback()),
                current.primaryColor(),
                current.secondaryColor(),
                current.legacyColors()
        );
    }

    private static String normalizeLocaleValue(String value, String fallbackValue) {
        if (value.isBlank()) return fallbackValue;
        return value.toLowerCase(Locale.ROOT).replace('-', '_');
    }

    private String normalizeLocale(String value) {
        return normalizeLocaleValue(value, state.locale());
    }

    public synchronized void theme(
            String primaryColor,
            String secondaryColor,
            boolean legacyColors
    ) {
        var current = state;
        state = new RuntimeState(
                current.locales(),
                current.locale(),
                current.fallback(),
                normalizeColor(primaryColor, "#55FF55"),
                normalizeColor(secondaryColor, "#FFFF55"),
                legacyColors
        );
    }

    private String normalizeColor(String value, String fallbackColor) {
        var normalized = value.trim();
        if (!normalized.startsWith("#")) normalized = "#" + normalized;
        return HEX.matcher(normalized).matches()
                ? normalized.toUpperCase(Locale.ROOT)
                : fallbackColor;
    }

    @Override
    public String message(String key) {
        return message(key, Map.of());
    }

    @Override
    public String message(String key, Map<String, ?> placeholders) {
        return rich(state.locale(), key, placeholders).plainText();
    }

    @Override
    public RichText rich(
            String locale,
            String key,
            Map<String, ?> placeholders
    ) {
        return render(locale, key, placeholders);
    }

    @Override
    public boolean contains(String locale, String key) {
        var current = state;
        var normalized = normalizeLocaleValue(locale, current.locale());
        return messages(current, normalized).containsKey(key)
                || messages(current, current.fallback()).containsKey(key);
    }

    @Override
    public void reload() {
        var current = state;
        var prepared = prepareReload(
                current.locale(),
                current.fallback(),
                current.primaryColor(),
                current.secondaryColor(),
                current.legacyColors()
        );
        synchronized (this) {
            var latest = state;
            state = new RuntimeState(
                    prepared.locales(),
                    latest.locale(),
                    latest.fallback(),
                    latest.primaryColor(),
                    latest.secondaryColor(),
                    latest.legacyColors()
            );
        }
    }

    private static Map<String, String> messages(RuntimeState state, String requestedLocale) {
        return state.locales().getOrDefault(
                normalizeLocaleValue(requestedLocale, state.locale()),
                Map.of()
        );
    }

    /**
     * Reads and validates all locale files without publishing them. Runtime rendering never performs file I/O.
     */
    public PreparedMessages prepareReload(String configuredLocale, String configuredFallback) {
        var current = state;
        return prepareReload(
                configuredLocale,
                configuredFallback,
                current.primaryColor(),
                current.secondaryColor(),
                current.legacyColors()
        );
    }

    public PreparedMessages prepareReload(
            String configuredLocale,
            String configuredFallback,
            String configuredPrimaryColor,
            String configuredSecondaryColor,
            boolean configuredLegacyColors
    ) {
        var requestedLocale = normalizeLocaleValue(configuredLocale, "zh_cn");
        var fallbackLocale = normalizeLocaleValue(configuredFallback, "en_us");
        var candidatePrimaryColor = normalizeColor(configuredPrimaryColor, "#55FF55");
        var candidateSecondaryColor = normalizeColor(configuredSecondaryColor, "#FFFF55");
        try {
            Files.createDirectories(directory);
            writeDefaultIfMissing("en_us");
            writeDefaultIfMissing("zh_cn");

            var names = new LinkedHashSet<String>();
            names.add("en_us");
            names.add("zh_cn");
            names.add(fallbackLocale);
            names.add(requestedLocale);
            try (var paths = Files.list(directory)) {
                paths.filter(Files::isRegularFile)
                        .map(path -> path.getFileName().toString())
                        .filter(name -> name.endsWith(".yml") && name.length() > 4)
                        .map(name -> normalizeLocaleValue(name.substring(0, name.length() - 4), ""))
                        .filter(name -> !name.isBlank())
                        .forEach(names::add);
            }

            var loaded = new LinkedHashMap<String, Map<String, String>>();
            for (var name : names) loadLocale(name, loaded);
            validateCatalogs(
                    loaded,
                    candidatePrimaryColor,
                    candidateSecondaryColor,
                    configuredLegacyColors
            );
            return new PreparedMessages(loaded);
        } catch (IOException | RuntimeException exception) {
            logger.error("Failed to load messages; the previous messages remain active", exception);
            throw new IllegalStateException("Failed to reload messages: " + exception.getMessage(), exception);
        }
    }

    public MessageState snapshot() {
        var current = state;
        return new MessageState(
                current.locales(),
                current.locale(),
                current.fallback(),
                current.primaryColor(),
                current.secondaryColor(),
                current.legacyColors()
        );
    }

    public synchronized void commit(
            PreparedMessages prepared,
            String configuredLocale,
            String configuredFallback,
            String configuredPrimaryColor,
            String configuredSecondaryColor,
            boolean configuredLegacyColors
    ) {
        state = new RuntimeState(
                prepared.locales(),
                normalizeLocaleValue(configuredLocale, "zh_cn"),
                normalizeLocaleValue(configuredFallback, "en_us"),
                normalizeColor(configuredPrimaryColor, "#55FF55"),
                normalizeColor(configuredSecondaryColor, "#FFFF55"),
                configuredLegacyColors
        );
    }

    public synchronized void restore(MessageState snapshot) {
        state = new RuntimeState(
                snapshot.locales(),
                snapshot.locale(),
                snapshot.fallback(),
                snapshot.primaryColor(),
                snapshot.secondaryColor(),
                snapshot.legacyColors()
        );
    }

    @Override
    public RichText render(
            String requestedLocale,
            String key,
            Map<String, ?> placeholders
    ) {
        return render(state, requestedLocale, key, placeholders);
    }

    @Override
    public RichText renderInline(
            String requestedLocale,
            String template,
            Map<String, ?> placeholders
    ) {
        return renderInline(state, requestedLocale, template, placeholders);
    }

    private RichText render(
            RuntimeState snapshot,
            String requestedLocale,
            String key,
            Map<String, ?> placeholders
    ) {
        var normalizedLocale = normalizeLocaleValue(requestedLocale, snapshot.locale());
        var template = lookup(snapshot, normalizedLocale, key);
        if (template.isEmpty()) {
            logger.warn("Missing message key: " + key);
            template = lookup(snapshot, normalizedLocale, "messages.missing");
        }
        return renderInline(
                snapshot,
                normalizedLocale,
                template.orElse("<red>A message could not be rendered."),
                placeholders
        );
    }

    private RichText renderInline(
            RuntimeState snapshot,
            String requestedLocale,
            String template,
            Map<String, ?> placeholders
    ) {
        var input = snapshot.legacyColors()
                ? LegacyMiniMessagePreprocessor.convert(template)
                : template;
        var resolver = TagResolver.builder()
                .resolver(colorTag("primary", snapshot.primaryColor()))
                .resolver(colorTag("secondary", snapshot.secondaryColor()));

        placeholders.forEach((name, value) -> {
            var normalized = name.toLowerCase(Locale.ROOT);
            if (!PLACEHOLDER_NAME.matcher(normalized).matches()) {
                throw new IllegalArgumentException("Invalid message placeholder name: " + name);
            }
            if (value instanceof RichText richText) {
                resolver.resolver(Placeholder.component(normalized, AdventureRichTextAdapter.toComponent(richText)));
            } else if (value instanceof LocalizedMessage(String key, Map<String, Object> placeholders1)) {
                resolver.resolver(Placeholder.component(
                        normalized,
                        AdventureRichTextAdapter.toComponent(render(
                                snapshot,
                                requestedLocale,
                                key,
                                placeholders1
                        ))
                ));
            } else {
                resolver.resolver(Placeholder.unparsed(normalized, String.valueOf(value)));
            }
        });

        var component = miniMessage.deserialize(input, resolver.build());
        return AdventureRichTextAdapter.fromComponent(component);
    }

    private TagResolver colorTag(String name, String color) {
        var parsed = TextColor.fromHexString(color);
        if (parsed == null) throw new IllegalStateException("Invalid configured message color: " + color);
        return TagResolver.resolver(name, Tag.styling(parsed));
    }

    private Map<String, String> loadLocale(
            String name,
            Map<String, Map<String, String>> destination
    ) throws IOException {
        var loaded = new LinkedHashMap<String, String>();
        var packaged = packagedDefaults.get(name);
        if (packaged != null) flatten("", packaged, loaded);

        loaded.putAll(readFlattened(directory.resolve(name + ".yml")));
        var immutable = Map.copyOf(loaded);
        destination.put(name, immutable);
        return immutable;
    }

    private void writeDefaultIfMissing(String name) throws IOException {
        var path = directory.resolve(name + ".yml");
        if (Files.exists(path)) return;

        var resource = RESOURCE_ROOT + name + ".yml";
        try (var input = resource(resource)) {
            Files.copy(input, path);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> readFlattened(Path path) throws IOException {
        if (Files.notExists(path)) return Map.of();

        Map<String, Object> raw = JacksonCodecs.readYaml(path, Map.class);
        Map<String, String> flattened = new LinkedHashMap<>();
        flatten("", raw, flattened);

        Map<String, String> defaults = new LinkedHashMap<>();
        packagedDefaults.values().forEach(catalog -> flatten("", catalog, defaults));
        var unknown = new TreeSet<>(flattened.keySet());
        unknown.removeAll(defaults.keySet());
        if (!unknown.isEmpty()) {
            logger.warn("Ignoring unknown message keys in " + path + ": " + String.join(", ", unknown));
            unknown.forEach(flattened::remove);
        }
        return flattened;
    }

    @SuppressWarnings("unchecked")
    private void flatten(
            String prefix,
            Map<String, Object> raw,
            Map<String, String> flattened
    ) {
        raw.forEach((key, value) -> {
            var fullKey = prefix.isBlank() ? key : prefix + "." + key;
            if (value instanceof Map<?, ?> map) {
                flatten(fullKey, (Map<String, Object>) map, flattened);
            } else {
                flattened.put(fullKey, String.valueOf(value));
            }
        });
    }

    private void validateCatalogs(
            Map<String, Map<String, String>> loaded,
            String candidatePrimaryColor,
            String candidateSecondaryColor,
            boolean candidateLegacyColors
    ) throws IOException {
        var english = loaded.get("en_us");
        var chinese = loaded.get("zh_cn");
        if (english == null || chinese == null) {
            throw new IOException("The en_us and zh_cn message catalogs are required");
        }
        if (!english.keySet().equals(chinese.keySet())) {
            throw new IOException("The en_us and zh_cn message key sets differ");
        }

        for (var localeEntry : loaded.entrySet()) {
            var localeName = localeEntry.getKey();
            for (var messageEntry : localeEntry.getValue().entrySet()) {
                var expected = english.get(messageEntry.getKey());
                if (expected == null) continue;
                var expectedPlaceholders = MessageTemplatePlaceholders.names(expected);
                var actualPlaceholders = MessageTemplatePlaceholders.names(messageEntry.getValue());
                if (!expectedPlaceholders.equals(actualPlaceholders)) {
                    throw new IOException(
                            "Placeholder mismatch for " + messageEntry.getKey()
                                    + " in " + localeName + ": expected " + expectedPlaceholders
                                    + " but found " + actualPlaceholders
                    );
                }
                validateTemplate(
                        messageEntry.getKey(),
                        messageEntry.getValue(),
                        actualPlaceholders,
                        candidatePrimaryColor,
                        candidateSecondaryColor,
                        candidateLegacyColors
                );
            }
        }
    }

    private void validateTemplate(
            String key,
            String template,
            Set<String> placeholders,
            String candidatePrimaryColor,
            String candidateSecondaryColor,
            boolean candidateLegacyColors
    ) throws IOException {
        var resolver = TagResolver.builder()
                .resolver(colorTag("primary", candidatePrimaryColor))
                .resolver(colorTag("secondary", candidateSecondaryColor));
        placeholders.forEach(name -> resolver.resolver(Placeholder.unparsed(name, "placeholder")));
        try {
            miniMessage.deserialize(
                    candidateLegacyColors ? LegacyMiniMessagePreprocessor.convert(template) : template,
                    resolver.build()
            );
        } catch (RuntimeException exception) {
            throw new IOException("Invalid message template for " + key, exception);
        }
    }

    private record RuntimeState(
            Map<String, Map<String, String>> locales,
            String locale,
            String fallback,
            String primaryColor,
            String secondaryColor,
            boolean legacyColors
    ) {

        private RuntimeState {
            var copied = new LinkedHashMap<String, Map<String, String>>();
            locales.forEach((name, messages) -> copied.put(name, Map.copyOf(messages)));
            locales = Map.copyOf(copied);
        }

    }

    public record PreparedMessages(Map<String, Map<String, String>> locales) {

        public PreparedMessages {
            var copied = new LinkedHashMap<String, Map<String, String>>();
            locales.forEach((name, messages) -> copied.put(name, Map.copyOf(messages)));
            locales = Map.copyOf(copied);
        }

    }

    public record MessageState(
            Map<String, Map<String, String>> locales,
            String locale,
            String fallback,
            String primaryColor,
            String secondaryColor,
            boolean legacyColors
    ) {

        public MessageState {
            var copied = new LinkedHashMap<String, Map<String, String>>();
            locales.forEach((name, messages) -> copied.put(name, Map.copyOf(messages)));
            locales = Map.copyOf(copied);
        }

    }

}
