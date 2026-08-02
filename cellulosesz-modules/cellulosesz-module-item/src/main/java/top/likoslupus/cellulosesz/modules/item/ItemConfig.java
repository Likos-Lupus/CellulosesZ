package top.likoslupus.cellulosesz.modules.item;

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
    public int clearConfirmationTtlSeconds = 30;
    public int clearMaximumTargets = 32;
    public int clearLargeRemovalThreshold = 128;
    public int maximumRecipeResults = 32;
    public int maximumRecipeIngredientCandidates = 8;
    public int maximumCondenseBatches = 4096;
    public int maximumCondenseRules = 64;
    public boolean allowOversizedStacks;
    public int maximumOversizedStack = 127;
    public int maximumDisplayedAliases = 20;
    public Map<String, String> aliases = new LinkedHashMap<>();
    public Map<String, CustomItemConfig> customItems = new LinkedHashMap<>();
    public Set<String> blacklist = new LinkedHashSet<>();

    public void copyFrom(ItemConfig source) {
        maxCommandCount = source.maxCommandCount;
        maxLoreLines = source.maxLoreLines;
        allowUnsafeEnchantments = source.allowUnsafeEnchantments;
        repairAllEnabled = source.repairAllEnabled;
        unlimitedMinimum = source.unlimitedMinimum;
        powerToolsEnabled = source.powerToolsEnabled;
        clearConfirmationTtlSeconds = source.clearConfirmationTtlSeconds;
        clearMaximumTargets = source.clearMaximumTargets;
        clearLargeRemovalThreshold = source.clearLargeRemovalThreshold;
        maximumRecipeResults = source.maximumRecipeResults;
        maximumRecipeIngredientCandidates = source.maximumRecipeIngredientCandidates;
        maximumCondenseBatches = source.maximumCondenseBatches;
        maximumCondenseRules = source.maximumCondenseRules;
        allowOversizedStacks = source.allowOversizedStacks;
        maximumOversizedStack = source.maximumOversizedStack;
        maximumDisplayedAliases = source.maximumDisplayedAliases;
        aliases = new LinkedHashMap<>(source.aliases);
        customItems = new LinkedHashMap<>(source.customItems);
        blacklist = new LinkedHashSet<>(source.blacklist);
    }

    public void validate() {
        range(maxCommandCount, 1, 1_000_000, "maxCommandCount");
        range(maxLoreLines, 0, 1024, "maxLoreLines");
        range(unlimitedMinimum, 1, 1_000_000, "unlimitedMinimum");
        range(clearConfirmationTtlSeconds, 1, 3600, "clearConfirmationTtlSeconds");
        range(clearMaximumTargets, 1, 1024, "clearMaximumTargets");
        range(clearLargeRemovalThreshold, 1, 1_000_000, "clearLargeRemovalThreshold");
        range(maximumRecipeResults, 1, 1024, "maximumRecipeResults");
        range(maximumRecipeIngredientCandidates, 1, 64, "maximumRecipeIngredientCandidates");
        range(maximumCondenseBatches, 1, 1_000_000, "maximumCondenseBatches");
        range(maximumCondenseRules, 1, 1024, "maximumCondenseRules");
        range(maximumOversizedStack, 1, 127, "maximumOversizedStack");
        range(maximumDisplayedAliases, 1, 1024, "maximumDisplayedAliases");
    }

    private static void range(
            int value,
            int minimum,
            int maximum,
            String name
    ) {
        if (value < minimum || value > maximum) {
            throw new IllegalArgumentException(
                    name + " must be between " + minimum + " and " + maximum
            );
        }
    }

}
