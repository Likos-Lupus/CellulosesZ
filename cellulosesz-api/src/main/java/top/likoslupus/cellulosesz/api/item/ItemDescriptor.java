package top.likoslupus.cellulosesz.api.item;

import java.util.LinkedHashMap;
import java.util.Map;

public class ItemDescriptor {

    public String item = "minecraft:air";
    public int count = 1;
    public Map<String, Object> components = new LinkedHashMap<>();

    public ItemDescriptor() {
    }

    public ItemDescriptor(String item, int count) {
        this.item = item;
        this.count = count;
    }

    public String normalizedItem() {
        var value = item.trim().toLowerCase();
        if (value.isBlank()) return "minecraft:air";
        return value.indexOf(':') < 0 ? "minecraft:" + value : value;
    }

}
