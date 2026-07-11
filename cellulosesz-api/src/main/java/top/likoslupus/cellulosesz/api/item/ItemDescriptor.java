package top.likoslupus.cellulosesz.api.item;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Platform-neutral description of an item stack.
 *
 * <p>The component map uses namespaced vanilla component identifiers as keys.
 * Values may be scalar values, lists, maps, or {@link RawItemComponent} when the caller intentionally supplies
 * command/SNBT syntax.</p>
 */
public class ItemDescriptor {

    public int schema = 1;
    public String item = "minecraft:air";
    public int count = 1;
    public Map<String, Object> components = new LinkedHashMap<>();

    public ItemDescriptor() {
    }

    public ItemDescriptor(
            String item,
            int count
    ) {
        this.item = item;
        this.count = count;
    }

    public ItemDescriptor(
            String item,
            int count,
            Map<String, Object> components
    ) {
        this.item = item;
        this.count = count;
        this.components = copyMap(components);
    }

    private static Map<String, Object> copyMap(Map<String, Object> source) {
        if (source.isEmpty()) return new LinkedHashMap<>();
        var result = new LinkedHashMap<String, Object>();
        source.forEach((key, value) -> result.put(key, deepCopy(value)));
        return result;
    }

    private static Object deepCopy(Object value) {
        if (value instanceof Map<?, ?> map) {
            var copy = new LinkedHashMap<String, Object>();
            map.forEach((key, nested) -> copy.put(String.valueOf(key), deepCopy(nested)));
            return copy;
        }

        if (value instanceof List<?> list) {
            var copy = new ArrayList<>();
            list.forEach(nested -> copy.add(deepCopy(nested)));
            return copy;
        }

        return value;
    }

    public String normalizedItem() {
        var value = item.trim().toLowerCase();
        if (value.isBlank()) return "minecraft:air";
        return value.indexOf(':') < 0 ? "minecraft:" + value : value;
    }

    public Map<String, Object> normalizedComponents() {
        if (components.isEmpty()) return Map.of();

        var normalized = new LinkedHashMap<String, Object>();
        components.forEach((key, value) -> {
            if (key.isBlank()) return;
            var id = key.trim().toLowerCase();
            normalized.put(id.indexOf(':') < 0 ? "minecraft:" + id : id, deepCopy(value));
        });

        return normalized;
    }

    public ItemDescriptor copy() {
        var copy = new ItemDescriptor(normalizedItem(), count, normalizedComponents());
        copy.schema = schema;
        return copy;
    }

}
