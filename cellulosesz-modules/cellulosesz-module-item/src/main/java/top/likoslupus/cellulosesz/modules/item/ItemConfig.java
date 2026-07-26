package top.likoslupus.cellulosesz.modules.item;

import top.likoslupus.cellulosesz.api.item.ItemDescriptor;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public final class ItemConfig {

    public int maxCommandCount = 6400;
    public int maxLoreLines = 20;
    public boolean allowUnsafeEnchantments;
    public boolean repairAllEnabled = true;
    public int unlimitedMinimum = 2;
    public boolean powerToolsEnabled = true;
    public Map<String, String> aliases = new LinkedHashMap<>();
    public Map<String, ItemDescriptor> customItems = new LinkedHashMap<>();
    public Set<String> blacklist = new LinkedHashSet<>();

    public void copyFrom(ItemConfig source) {
        maxCommandCount = source.maxCommandCount;
        maxLoreLines = source.maxLoreLines;
        allowUnsafeEnchantments = source.allowUnsafeEnchantments;
        repairAllEnabled = source.repairAllEnabled;
        unlimitedMinimum = source.unlimitedMinimum;
        powerToolsEnabled = source.powerToolsEnabled;
        aliases = new LinkedHashMap<>(source.aliases);
        customItems = new LinkedHashMap<>(source.customItems);
        blacklist = new LinkedHashSet<>(source.blacklist);
    }

}
