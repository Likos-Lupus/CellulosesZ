package top.likoslupus.cellulosesz.modules.item.service;

import top.likoslupus.cellulosesz.api.item.ItemDescriptor;
import top.likoslupus.cellulosesz.api.item.ItemService;
import top.likoslupus.cellulosesz.api.item.RawItemComponent;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.core.config.JacksonCodecs;
import top.likoslupus.cellulosesz.modules.item.ItemConfig;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

public final class DefaultItemService implements ItemService {

    private static final Pattern ID_PATTERN = Pattern.compile("^[a-z0-9_.-]+:[a-z0-9_./-]+$");
    private static final Pattern INTEGER_PATTERN = Pattern.compile("^[0-9]+$");

    private final PlatformService platform;
    private final ItemConfig config;

    public DefaultItemService(
            PlatformService platform,
            ItemConfig config
    ) {
        this.platform = platform;
        this.config = config;
    }

    @Override
    public Optional<ItemDescriptor> parse(String input) {
        if (input.isBlank()) return Optional.empty();

        var cursor = 0;
        var value = input.trim();
        while (cursor < value.length()
                && !Character.isWhitespace(value.charAt(cursor))
                && value.charAt(cursor) != '['
        ) {
            cursor++;
        }
        var itemId = normalizeId(value.substring(0, cursor));
        if (!ID_PATTERN.matcher(itemId).matches()) return Optional.empty();

        var descriptor = new ItemDescriptor(itemId, 1);
        if (cursor < value.length() && value.charAt(cursor) == '[') {
            var end = matchingBracket(value, cursor);
            if (end < 0 || !parseComponentList(value.substring(cursor + 1, end), descriptor.components)) {
                return Optional.empty();
            }
            cursor = end + 1;
        }

        var tail = value.substring(cursor).trim();
        if (!tail.isBlank()) {
            var firstWhitespace = firstWhitespace(tail);
            var first = firstWhitespace < 0
                    ? tail
                    : tail.substring(0, firstWhitespace);
            if (INTEGER_PATTERN.matcher(first).matches()) {
                try {
                    descriptor.count = Integer.parseInt(first);
                } catch (NumberFormatException _) {
                    return Optional.empty();
                }
                tail = firstWhitespace < 0
                        ? ""
                        : tail.substring(firstWhitespace).trim();
            }
        }

        if (descriptor.count <= 0
                || descriptor.count > Math.max(1, config.maxCommandCount)
                || !tail.isBlank()
                && !parseTrailingComponents(tail, descriptor.components)
        ) return Optional.empty();

        return Optional.of(descriptor);
    }

    @Override
    public String commandArgument(ItemDescriptor item) {
        var id = item.normalizedItem();
        if (!ID_PATTERN.matcher(id).matches()) return "minecraft:air";

        var components = item.normalizedComponents();
        if (components.isEmpty()) return id;

        var parts = new ArrayList<String>();
        components.forEach((key, value) -> {
            if (ID_PATTERN.matcher(key).matches()) parts.add(key + "=" + serialize(value));
        });
        return parts.isEmpty()
                ? id
                : "%s[%s]".formatted(id, String.join(",", parts));
    }

    @Override
    public boolean give(CellPlayer player, ItemDescriptor item) {
        if (item.count <= 0 || item.count > Math.max(1, config.maxCommandCount)) return false;
        return platform.giveItem(player, commandArgument(item), item.count);
    }

    @Override
    public int count(CellPlayer player, ItemDescriptor item) {
        return platform.countItem(player, commandArgument(item));
    }

    @Override
    public boolean take(CellPlayer player, ItemDescriptor item) {
        if (item.count <= 0) return false;
        return platform.takeItem(player, commandArgument(item), item.count);
    }

    @Override
    public Optional<String> heldItemId(CellPlayer player) {
        return platform.heldItemId(player);
    }

    private String serialize(Object value) {
        if (value instanceof RawItemComponent(String value1)) return value1;
        try {
            return JacksonCodecs.writeJsonString(value);
        } catch (RuntimeException exception) {
            return String.valueOf(value);
        }
    }

    private String normalizeId(String value) {
        var normalized = value.trim().toLowerCase();
        return normalized.indexOf(':') < 0 ? "minecraft:" + normalized : normalized;
    }

    private int matchingBracket(String input, int start) {
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
                } else if (current == quote) {
                    quote = '\0';
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

    private boolean parseComponentList(String input, Map<String, Object> output) {
        if (input.isBlank()) return true;

        for (var entry : splitTopLevel(input, ',')) {
            if (entry.isBlank()) return false;

            var equals = topLevelIndex(entry, '=');
            if (equals <= 0 || equals == entry.length() - 1) return false;

            var key = normalizeId(entry.substring(0, equals));
            if (!ID_PATTERN.matcher(key).matches()) return false;

            var raw = entry.substring(equals + 1).trim();
            if (raw.isEmpty()) return false;

            output.put(key, new RawItemComponent(raw));
        }

        return true;
    }

    private int firstWhitespace(String value) {
        return IntStream.range(0, value.length())
                .filter(index -> Character.isWhitespace(value.charAt(index)))
                .findFirst()
                .orElse(-1);
    }

    private boolean parseTrailingComponents(String input, Map<String, Object> output) {
        var value = input.trim();
        if (value.startsWith("{") && value.endsWith("}")) {
            try {
                Map<?, ?> parsed = JacksonCodecs.readJson(value, LinkedHashMap.class);
                parsed.forEach((key, component) ->
                        output.put(String.valueOf(key), component)
                );
                return true;
            } catch (Exception _) {
                return false;
            }
        }

        if (value.startsWith("[") && value.endsWith("]")
                && matchingBracket(value, 0) == value.length() - 1
        ) {
            return parseComponentList(value.substring(1, value.length() - 1), output);
        }

        return false;
    }

    private List<String> splitTopLevel(String input, char delimiter) {
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
                continue;
            }

            if (current == '{' || current == '[' || current == '(') {
                depth++;
            } else if (current == '}' || current == ']' || current == ')') {
                depth--;
                if (depth < 0) return List.of("");
            } else if (current == delimiter && depth == 0) {
                result.add(input.substring(start, index).trim());
                start = index + 1;
            }
        }

        if (quote != '\0' || depth != 0) return List.of("");

        result.add(input.substring(start).trim());
        return result;
    }

    private int topLevelIndex(String input, char target) {
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
                depth--;
            } else if (current == target && depth == 0) {
                return index;
            }
        }

        return -1;
    }

}
