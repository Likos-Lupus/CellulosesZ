package top.likoslupus.cellulosesz.core.i18n;

import top.likoslupus.cellulosesz.api.i18n.MessageService;
import top.likoslupus.cellulosesz.api.logging.CellulosesZLogger;
import top.likoslupus.cellulosesz.api.text.LocalizedMessage;
import top.likoslupus.cellulosesz.api.text.MessageRenderer;
import top.likoslupus.cellulosesz.api.text.RichText;
import top.likoslupus.cellulosesz.api.text.TextStyle;
import top.likoslupus.cellulosesz.core.config.JacksonCodecs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;

public final class DefaultMessageService implements MessageService, MessageRenderer {

    private static final Pattern HEX = Pattern.compile("^#[0-9a-fA-F]{6}$");
    private static final Map<Character, String> LEGACY_COLORS = Map.ofEntries(
            Map.entry('0', "#000000"),
            Map.entry('1', "#0000AA"),
            Map.entry('2', "#00AA00"),
            Map.entry('3', "#00AAAA"),
            Map.entry('4', "#AA0000"),
            Map.entry('5', "#AA00AA"),
            Map.entry('6', "#FFAA00"),
            Map.entry('7', "#AAAAAA"),
            Map.entry('8', "#555555"),
            Map.entry('9', "#5555FF"),
            Map.entry('a', "#55FF55"),
            Map.entry('b', "#55FFFF"),
            Map.entry('c', "#FF5555"),
            Map.entry('d', "#FF55FF"),
            Map.entry('e', "#FFFF55"),
            Map.entry('f', "#FFFFFF")
    );
    private static final Map<String, String> NAMED_COLORS = Map.ofEntries(
            Map.entry("black", "#000000"),
            Map.entry("dark_blue", "#0000AA"),
            Map.entry("dark_green", "#00AA00"),
            Map.entry("dark_aqua", "#00AAAA"),
            Map.entry("dark_red", "#AA0000"),
            Map.entry("dark_purple", "#AA00AA"),
            Map.entry("gold", "#FFAA00"),
            Map.entry("gray", "#AAAAAA"),
            Map.entry("dark_gray", "#555555"),
            Map.entry("blue", "#5555FF"),
            Map.entry("green", "#55FF55"),
            Map.entry("aqua", "#55FFFF"),
            Map.entry("red", "#FF5555"),
            Map.entry("light_purple", "#FF55FF"),
            Map.entry("yellow", "#FFFF55"),
            Map.entry("white", "#FFFFFF")
    );

    private final Path directory;
    private final CellulosesZLogger logger;
    private final Map<String, Map<String, String>> locales = new LinkedHashMap<>();
    private String locale = "zh_cn";
    private String fallback = "en_us";
    private String primaryColor = "#55FF55";
    private String secondaryColor = "#FFFF55";
    private boolean legacyColors = true;

    public DefaultMessageService(
            Path directory,
            CellulosesZLogger logger
    ) {
        this.directory = directory;
        this.logger = logger;
    }

    public void locales(String locale, String fallback) {
        this.locale = normalizeLocale(locale);
        this.fallback = normalizeLocale(fallback);
    }

    private String normalizeLocale(String value) {
        if (value.isBlank()) return locale;
        return value
                .toLowerCase(Locale.ROOT)
                .replace('-', '_');
    }

    public void theme(
            String primaryColor,
            String secondaryColor,
            boolean legacyColors
    ) {
        this.primaryColor = normalizeColor(primaryColor, "#55FF55");
        this.secondaryColor = normalizeColor(secondaryColor, "#FFFF55");
        this.legacyColors = legacyColors;
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
        return rich(locale, key, placeholders).plainText();
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
        var normalized = normalizeLocale(locale);
        return messages(normalized).containsKey(key) || messages(fallback).containsKey(key);
    }

    @Override
    public void reload() {
        var prepared = prepareReload(locale, fallback);
        synchronized (this) {
            locales.clear();
            locales.putAll(prepared.locales());
        }
    }

    private synchronized Map<String, String> messages(String requestedLocale) {
        return locales.getOrDefault(normalizeLocale(requestedLocale), Map.of());
    }

    /**
     * Reads all locale files without publishing them. Runtime rendering never performs file I/O.
     */
    public PreparedMessages prepareReload(String configuredLocale, String configuredFallback) {
        var requestedLocale = normalizeLocaleValue(configuredLocale, "zh_cn");
        var fallbackLocale = normalizeLocaleValue(configuredFallback, "en_us");
        try {
            Files.createDirectories(directory);
            writeDefaultIfMissing("en_us", defaultEnglish());
            writeDefaultIfMissing("zh_cn", defaultChinese());

            var names = new LinkedHashSet<String>();
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
            return new PreparedMessages(loaded);
        } catch (IOException exception) {
            logger.error("Failed to load messages; the previous messages remain active", exception);
            throw new IllegalStateException("Failed to reload messages: " + exception.getMessage(), exception);
        }
    }

    public synchronized MessageState snapshot() {
        return new MessageState(
                locales,
                locale,
                fallback,
                primaryColor,
                secondaryColor,
                legacyColors
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
        locale = normalizeLocaleValue(configuredLocale, "zh_cn");
        fallback = normalizeLocaleValue(configuredFallback, "en_us");
        primaryColor = normalizeColor(configuredPrimaryColor, "#55FF55");
        secondaryColor = normalizeColor(configuredSecondaryColor, "#FFFF55");
        legacyColors = configuredLegacyColors;
        locales.clear();
        locales.putAll(prepared.locales());
    }

    private static String normalizeLocaleValue(String value, String fallbackValue) {
        if (value.isBlank()) return fallbackValue;
        return value.toLowerCase(Locale.ROOT).replace('-', '_');
    }

    public synchronized void restore(MessageState state) {
        locale = state.locale();
        fallback = state.fallback();
        primaryColor = state.primaryColor();
        secondaryColor = state.secondaryColor();
        legacyColors = state.legacyColors();
        locales.clear();
        locales.putAll(state.locales());
    }

    @Override
    public RichText render(
            String requestedLocale,
            String key,
            Map<String, ?> placeholders
    ) {
        var template = lookup(normalizeLocale(requestedLocale), key)
                .orElse("<red><missing message: " + key + ">");
        return renderInline(requestedLocale, template, placeholders);
    }

    @Override
    public RichText renderInline(
            String requestedLocale,
            String template,
            Map<String, ?> placeholders
    ) {
        return parse(normalizeLocale(requestedLocale), template, placeholders);
    }

    private Optional<String> lookup(String requestedLocale, String key) {
        var requested = messages(requestedLocale).get(key);
        if (requested != null) return Optional.of(requested);

        var configured = messages(locale).get(key);
        if (configured != null) return Optional.of(configured);

        return Optional.ofNullable(messages(fallback).get(key));
    }

    private Map<String, String> loadLocale(
            String name,
            Map<String, Map<String, String>> destination
    ) throws IOException {
        var loaded = new LinkedHashMap<String, String>();
        if (name.equals("en_us")) flatten("", defaultEnglish(), loaded);
        if (name.equals("zh_cn")) flatten("", defaultChinese(), loaded);

        loaded.putAll(readFlattened(directory.resolve(name + ".yml")));
        var immutable = Map.copyOf(loaded);
        destination.put(name, immutable);
        return immutable;
    }

    private RichText parse(
            String requestedLocale,
            String input,
            Map<String, ?> placeholders
    ) {
        var segments = new ArrayList<RichText.Segment>();
        var buffer = new StringBuilder();
        var style = TextStyle.EMPTY;

        for (var index = 0; index < input.length(); ) {
            var current = input.charAt(index);
            if (current == '{') {
                var end = input.indexOf('}', index + 1);
                if (end > index) {
                    var key = input.substring(index + 1, end);
                    if (placeholders.containsKey(key)) {
                        var value = placeholders.get(key);
                        if (value instanceof RichText(List<RichText.Segment> segments1)) {
                            flush(segments, buffer, style);
                            segments.addAll(segments1);
                        } else if (value instanceof LocalizedMessage(String key1, Map<String, Object> placeholders1)) {
                            flush(segments, buffer, style);
                            segments.addAll(render(
                                    requestedLocale,
                                    key1,
                                    placeholders1
                            ).segments());
                        } else {
                            buffer.append(value);
                        }
                        index = end + 1;
                        continue;
                    }
                }
            }
            if (current == '<') {
                var end = input.indexOf('>', index + 1);
                if (end > index) {
                    var tag = input.substring(index + 1, end).trim().toLowerCase(Locale.ROOT);
                    var updated = applyTag(style, tag);
                    if (updated.isPresent()) {
                        flush(segments, buffer, style);
                        style = updated.orElseThrow();
                        index = end + 1;
                        continue;
                    }
                }
            }

            if (legacyColors && (current == '&' || current == '§') && index + 1 < input.length()) {
                if (input.charAt(index + 1) == '#' && index + 8 <= input.length()) {
                    var hex = input.substring(index + 1, index + 8);
                    if (HEX.matcher(hex).matches()) {
                        flush(segments, buffer, style);
                        style = style.withColor(hex.toUpperCase(Locale.ROOT));
                        index += 8;
                        continue;
                    }
                }
                var code = Character.toLowerCase(input.charAt(index + 1));
                var updated = applyLegacy(style, code);
                if (updated.isPresent()) {
                    flush(segments, buffer, style);
                    style = updated.orElseThrow();
                    index += 2;
                    continue;
                }
            }

            buffer.append(current);
            index++;
        }
        flush(segments, buffer, style);
        return new RichText(segments);
    }

    private Optional<TextStyle> applyTag(TextStyle style, String tag) {
        return switch (tag) {
            case "primary" -> Optional.of(style.withColor(primaryColor));
            case "secondary" -> Optional.of(style.withColor(secondaryColor));
            case "bold", "b" -> Optional.of(style.withBold(true));
            case "/bold", "/b" -> Optional.of(style.withBold(false));
            case "italic", "i" -> Optional.of(style.withItalic(true));
            case "/italic", "/i" -> Optional.of(style.withItalic(false));
            case "underlined", "underline", "u" -> Optional.of(style.withUnderlined(true));
            case "/underlined", "/underline", "/u" -> Optional.of(style.withUnderlined(false));
            case "strikethrough", "st" -> Optional.of(style.withStrikethrough(true));
            case "/strikethrough", "/st" -> Optional.of(style.withStrikethrough(false));
            case "obfuscated", "magic" -> Optional.of(style.withObfuscated(true));
            case "/obfuscated", "/magic" -> Optional.of(style.withObfuscated(false));
            case "reset", "/reset" -> Optional.of(TextStyle.EMPTY);
            default -> {
                if (HEX.matcher(tag).matches()) {
                    yield Optional.of(style.withColor(tag.toUpperCase(Locale.ROOT)));
                }
                yield Optional.ofNullable(NAMED_COLORS.get(tag))
                        .map(style::withColor);
            }
        };
    }

    private Optional<TextStyle> applyLegacy(TextStyle style, char code) {
        var color = LEGACY_COLORS.get(code);
        if (color != null) return Optional.of(TextStyle.EMPTY.withColor(color));

        return switch (code) {
            case 'k' -> Optional.of(style.withObfuscated(true));
            case 'l' -> Optional.of(style.withBold(true));
            case 'm' -> Optional.of(style.withStrikethrough(true));
            case 'n' -> Optional.of(style.withUnderlined(true));
            case 'o' -> Optional.of(style.withItalic(true));
            case 'r' -> Optional.of(TextStyle.EMPTY);
            default -> Optional.empty();
        };
    }

    private void flush(
            List<RichText.Segment> segments,
            StringBuilder buffer,
            TextStyle style
    ) {
        if (buffer.isEmpty()) return;
        segments.add(new RichText.Segment(buffer.toString(), style));
        buffer.setLength(0);
    }

    private void writeDefaultIfMissing(String name, Map<String, Object> value) throws IOException {
        var path = directory.resolve(name + ".yml");
        if (Files.notExists(path)) JacksonCodecs.writeYaml(path, value);
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> readFlattened(Path path) throws IOException {
        if (Files.notExists(path)) return Map.of();

        Map<String, Object> raw = JacksonCodecs.readYaml(path, Map.class);
        Map<String, String> flattened = new LinkedHashMap<>();
        flatten("", raw, flattened);

        Map<String, String> defaults = new LinkedHashMap<>();
        flatten("", defaultEnglish(), defaults);
        var unknown = new TreeSet<>(flattened.keySet());
        unknown.removeAll(defaults.keySet());
        if (!unknown.isEmpty()) {
            throw new IOException("Unknown message keys in " + path + ": " + String.join(", ", unknown));
        }
        return flattened;
    }

    @SuppressWarnings("unchecked")
    private void flatten(
            String prefix,
            Map<String, Object> raw,
            Map<String, String> flattened
    ) {
        raw.forEach((key1, value) -> {
            var key = prefix.isBlank()
                    ? key1
                    : prefix + "." + key1;

            if (value instanceof Map<?, ?> map) {
                flatten(key, (Map<String, Object>) map, flattened);
            } else {
                flattened.put(key, String.valueOf(value));
            }
        });
    }

    private Map<String, Object> defaultEnglish() {
        return unflatten(BuiltInCommandMessages.english());
    }

    private Map<String, Object> defaultChinese() {
        return unflatten(BuiltInCommandMessages.chinese());
    }

    private Map<String, Object> unflatten(Map<String, String> flattened) {
        var root = new LinkedHashMap<String, Object>();
        flattened.forEach((key, value) -> {
            var segments = key.split("\\.");
            Map<String, Object> current = root;
            for (int index = 0; index < segments.length - 1; index++) {
                var segment = segments[index];
                var existing = current.get(segment);
                if (existing == null) {
                    var created = new LinkedHashMap<String, Object>();
                    current.put(segment, created);
                    current = created;
                } else if (existing instanceof Map<?, ?> nested) {
                    @SuppressWarnings("unchecked")
                    var typed = (Map<String, Object>) nested;
                    current = typed;
                } else {
                    throw new IllegalStateException("Message key prefix collides with a value: " + key);
                }
            }
            var leaf = segments[segments.length - 1];
            if (current.putIfAbsent(leaf, value) != null) {
                throw new IllegalStateException("Duplicate message key: " + key);
            }
        });
        return root;
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
