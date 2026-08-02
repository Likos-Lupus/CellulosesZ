package top.likoslupus.cellulosesz.modules.item.service;

import top.likoslupus.cellulosesz.api.item.ItemDescriptor;
import top.likoslupus.cellulosesz.api.item.ItemPlatformService;
import top.likoslupus.cellulosesz.api.item.ItemService;
import top.likoslupus.cellulosesz.api.item.RawItemComponent;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.core.config.JacksonCodecs;
import top.likoslupus.cellulosesz.modules.item.ItemConfig;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import static top.likoslupus.cellulosesz.api.validation.Checks.requirePositive;

import static java.util.Objects.requireNonNull;

public final class DefaultItemService implements ItemService {

    private static final Pattern ID_PATTERN = Pattern.compile("^[a-z0-9_.-]+:[a-z0-9_./-]+$");
    private static final Pattern INTEGER_PATTERN = Pattern.compile("^[0-9]+$");
    private final ItemPlatformService platform;
    private volatile ItemConfig config;
    private volatile Map<String, String> aliases = Map.of();
    private volatile Map<String, ItemDescriptor> customItems = Map.of();
    private volatile Set<String> blacklist = Set.of();

    public DefaultItemService(
            ItemPlatformService platform,
            ItemConfig config
    ) {
        this.platform = requireNonNull(platform, "platform");
        configure(config);
    }

    public void configure(ItemConfig config) {
        var snapshot = new ItemConfig();
        snapshot.copyFrom(requireNonNull(config, "config"));
        snapshot.validate();
        requirePositive(snapshot.maxCommandCount, "maxCommandCount");
        requirePositive(snapshot.maxLoreLines, "maxLoreLines");

        var aliasCopy = new LinkedHashMap<String, String>();
        requireNonNull(snapshot.aliases, "aliases")
                .forEach((alias, item) -> {
                    var key = key(alias);
                    var normalized = normalizeId(item);

                    if (key.isBlank() || !ID_PATTERN.matcher(normalized).matches()) {
                        throw new IllegalArgumentException("Invalid item alias: " + alias);
                    }

                    if (aliasCopy.put(key, normalized) != null) {
                        throw new IllegalArgumentException("Duplicate item alias: " + alias);
                    }
                });

        var customCopy = new LinkedHashMap<String, ItemDescriptor>();
        requireNonNull(snapshot.customItems, "customItems")
                .forEach((name, item) -> {
                    var key = key(name);
                    requireNonNull(item, "custom item");

                    var copy = item.copy();
                    if (key.isBlank() || !validDescriptorShape(copy)) {
                        throw new IllegalArgumentException("Invalid custom item: " + name);
                    }

                    if (customCopy.put(key, copy) != null) {
                        throw new IllegalArgumentException("Duplicate custom item: " + name);
                    }
                });

        var blacklistCopy = new LinkedHashSet<String>();
        requireNonNull(snapshot.blacklist, "blacklist")
                .forEach(item -> {
                    var normalized = normalizeId(item);
                    if (!ID_PATTERN.matcher(normalized).matches()) {
                        throw new IllegalArgumentException("Invalid blacklisted item: " + item);
                    }

                    blacklistCopy.add(normalized);
                });

        this.config = snapshot;
        this.aliases = Map.copyOf(aliasCopy);
        this.customItems = Map.copyOf(customCopy);
        this.blacklist = Set.copyOf(blacklistCopy);
    }

    private static String key(String value) {
        return requireNonNull(value, "value").trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeId(String value) {
        var normalized = key(value);
        return normalized.indexOf(':') < 0
                ? "minecraft:" + normalized
                : normalized;
    }

    private boolean validDescriptorShape(ItemDescriptor item) {
        if (item.count <= 0
                || item.count > Math.max(1, config.maxCommandCount)
        ) {
            return false;
        }

        var id = item.normalizedItem();
        if (!ID_PATTERN.matcher(id).matches()) {
            return false;
        }

        return item.normalizedComponents().keySet().stream()
                .allMatch(key -> ID_PATTERN.matcher(key).matches());
    }

    private static int firstTokenEnd(String value) {
        for (var i = 0; i < value.length(); i++) {
            var c = value.charAt(i);
            if (Character.isWhitespace(c) || c == '[') {
                return i;
            }
        }

        return value.length();
    }

    private static int firstWhitespace(String value) {
        return IntStream.range(0, value.length())
                .filter(i -> Character.isWhitespace(value.charAt(i)))
                .findFirst()
                .orElse(-1);
    }

    private static int matchingBracket(String input, int start) {
        var depth = 0;
        var quote = '\0';
        var escaped = false;

        for (var index = start; index < input.length(); index++) {
            var current = input.charAt(index);
            if (quote != '\0') {
                if (escaped) {
                    escaped = false;
                } else if (current == '\\') {
                    escaped = true;
                } else {
                    if (current == quote) {
                        quote = '\0';
                    }
                }
                continue;
            }

            if (current == '\'' || current == '"') {
                quote = current;
                continue;
            }

            if (current == '[') {
                depth++;
            } else if (current == ']' && --depth == 0) {
                return index;
            }
        }

        return -1;
    }

    private static boolean parseComponentList(
            String input,
            Map<String, Object> output
    ) {
        if (input.isBlank()) {
            return true;
        }

        for (var entry : splitTopLevel(input, ',')) {
            var equals = topLevelIndex(entry, '=');
            if (equals <= 0 || equals == entry.length() - 1) {
                return false;
            }

            var component = normalizeId(entry.substring(0, equals));
            if (!ID_PATTERN.matcher(component).matches()) {
                return false;
            }

            var raw = entry.substring(equals + 1).trim();
            if (raw.isEmpty()
                    || output.put(component, new RawItemComponent(raw)) != null
            ) {
                return false;
            }
        }

        return true;
    }

    private static boolean parseTrailingComponents(
            String input,
            Map<String, Object> output
    ) {
        var value = input.trim();

        if (value.startsWith("{") && value.endsWith("}")) {
            try {
                Map<?, ?> parsed = JacksonCodecs.readJson(value, LinkedHashMap.class);
                for (var entry : parsed.entrySet()) {
                    var key = normalizeId(String.valueOf(entry.getKey()));
                    if (!ID_PATTERN.matcher(key).matches()
                            || output.put(key, entry.getValue()) != null
                    ) {
                        return false;
                    }
                }

                return true;
            } catch (IOException | RuntimeException _) {
                return false;
            }
        }

        return value.startsWith("[")
                && value.endsWith("]")
                && matchingBracket(value, 0) == value.length() - 1
                && parseComponentList(value.substring(1, value.length() - 1), output);
    }

    private static List<String> splitTopLevel(String input, char delimiter) {
        var result = new ArrayList<String>();

        var start = 0;
        var depth = 0;
        var quote = '\0';
        var escaped = false;

        for (var index = 0; index < input.length(); index++) {
            var current = input.charAt(index);
            if (quote != '\0') {
                if (escaped) {
                    escaped = false;
                } else if (current == '\\') {
                    escaped = true;
                } else if (current == quote) {
                    quote = '\0';
                }
                continue;
            }

            if (current == '\'' || current == '"') {
                quote = current;
            } else if (current == '{' || current == '[' || current == '(') {
                depth++;
            } else if (current == '}' || current == ']' || current == ')') {
                if (--depth < 0) {
                    return List.of("");
                }
            } else if (current == delimiter && depth == 0) {
                result.add(input.substring(start, index).trim());
                start = index + 1;
            }
        }

        if (quote != '\0' || depth != 0) {
            return List.of("");
        }

        result.add(input.substring(start).trim());
        return result;
    }

    private static int topLevelIndex(String input, char target) {
        var depth = 0;
        var quote = '\0';
        var escaped = false;

        for (var index = 0; index < input.length(); index++) {
            var current = input.charAt(index);
            if (quote != '\0') {
                if (escaped) {
                    escaped = false;
                } else if (current == '\\') {
                    escaped = true;
                } else {
                    if (current == quote) {
                        quote = '\0';
                    }
                }
                continue;
            }

            if (current == '\'' || current == '"') {
                quote = current;
            } else if (current == '{' || current == '[' || current == '(') {
                depth++;
            } else if (current == '}' || current == ']' || current == ')') {
                depth--;
            } else {
                if (current == target && depth == 0) {
                    return index;
                }
            }
        }

        return -1;
    }

    @Override
    public Optional<ItemDescriptor> parse(String input) {
        if (input.isBlank()) {
            return Optional.empty();
        }

        var value = input.trim();
        var firstEnd = firstTokenEnd(value);
        var first = value.substring(0, firstEnd);
        var custom = customItems.get(key(first));

        if (custom != null) {
            var result = custom.copy();
            var tail = value.substring(firstEnd).trim();
            if (!tail.isEmpty()) {
                if (!INTEGER_PATTERN.matcher(tail).matches()) {
                    return Optional.empty();
                }

                try {
                    result.count = Integer.parseInt(tail);
                } catch (NumberFormatException _) {
                    return Optional.empty();
                }
            }

            return validDescriptorShape(result)
                    ? Optional.of(result)
                    : Optional.empty();
        }

        var alias = aliases.get(key(first));
        if (alias != null) {
            value = alias + value.substring(firstEnd);
        }

        var cursor = 0;
        while (cursor < value.length()
                && !Character.isWhitespace(value.charAt(cursor))
                && value.charAt(cursor) != '['
        ) {
            cursor++;
        }

        if (cursor == 0) {
            return Optional.empty();
        }

        var itemId = normalizeId(value.substring(0, cursor));
        if (!ID_PATTERN.matcher(itemId).matches()) {
            return Optional.empty();
        }

        var descriptor = new ItemDescriptor(itemId, 1);
        if (cursor < value.length() && value.charAt(cursor) == '[') {
            var end = matchingBracket(value, cursor);
            if (end < 0
                    || !parseComponentList(value.substring(cursor + 1, end), descriptor.components)
            ) {
                return Optional.empty();
            }
            cursor = end + 1;
        }

        var tail = value.substring(cursor).trim();
        if (!tail.isBlank()) {
            var whitespace = firstWhitespace(tail);
            var firstTail = whitespace < 0
                    ? tail
                    : tail.substring(0, whitespace);

            if (INTEGER_PATTERN.matcher(firstTail).matches()) {
                try {
                    descriptor.count = Integer.parseInt(firstTail);
                } catch (NumberFormatException _) {
                    return Optional.empty();
                }

                tail = whitespace < 0
                        ? ""
                        : tail.substring(whitespace).trim();
            }
        }

        if (!tail.isBlank()
                && !parseTrailingComponents(tail, descriptor.components)
        ) {
            return Optional.empty();
        }

        return validDescriptorShape(descriptor)
                ? Optional.of(descriptor)
                : Optional.empty();
    }

    @Override
    public String commandArgument(ItemDescriptor item) {
        requireNonNull(item, "item");
        var id = item.normalizedItem();
        if (!ID_PATTERN.matcher(id).matches()) {
            throw new IllegalArgumentException("Invalid item id: " + id);
        }

        var components = item.normalizedComponents();
        if (components.isEmpty()) {
            return id;
        }

        var parts = new ArrayList<String>();
        components.forEach((key, value) -> {
            if (!ID_PATTERN.matcher(key).matches()) {
                throw new IllegalArgumentException("Invalid component id: " + key);
            }
            parts.add(key + "=" + serialize(value));
        });

        return "%s[%s]".formatted(id, String.join(",", parts));
    }

    @Override
    public boolean valid(ItemDescriptor item) {
        return validDescriptorShape(item) && platform.validItem(item.normalizedItem());
    }

    @Override
    public boolean blacklisted(ItemDescriptor item) {
        return blacklist.contains(item.normalizedItem());
    }

    @Override
    public int maxStackSize(ItemDescriptor item) {
        return Math.max(1, platform.maxStackSize(item.normalizedItem()));
    }

    @Override
    public Set<String> names() {
        var names = new LinkedHashSet<String>();
        names.addAll(platform.itemIds());
        names.addAll(aliases.keySet());
        names.addAll(customItems.keySet());
        return Set.copyOf(names);
    }

    @Override
    public boolean give(CellPlayer player, ItemDescriptor item) {
        return valid(item)
                && item.count <= config.maxCommandCount
                && platform.grant(player, item.copy()).successful();
    }

    @Override
    public int count(CellPlayer player, ItemDescriptor item) {
        if (!valid(item)) {
            return 0;
        }

        var result = platform.count(player, item.copy());
        return result.successful() && result.value().isPresent()
                ? result.value().orElseThrow()
                : 0;
    }

    @Override
    public boolean take(CellPlayer player, ItemDescriptor item) {
        return valid(item)
                && platform.take(player, item.copy()).successful();
    }

    @Override
    public Optional<String> heldItemId(CellPlayer player) {
        var result = platform.heldItemId(player);
        return result.successful()
                ? result.value()
                : Optional.empty();
    }

    private String serialize(Object value) {
        if (value instanceof RawItemComponent(String value1)) {
            if (value1.isBlank()) {
                throw new IllegalArgumentException("Raw component must not be blank");
            }

            return value1;
        }

        return JacksonCodecs.writeJsonString(value);
    }

}
