package top.likoslupus.cellulosesz.modules.item;

import static java.util.Objects.requireNonNull;

/** Atomically published immutable command settings for the item module. */
public final class ItemRuntimeSettings {

    private volatile Snapshot snapshot;

    public ItemRuntimeSettings(ItemConfig config) {
        snapshot = Snapshot.from(config);
    }

    public void configure(ItemConfig config) {
        snapshot = Snapshot.from(config);
    }

    public int maxLoreLines() {
        return snapshot.maxLoreLines();
    }

    public int clearConfirmationTtlSeconds() {
        return snapshot.clearConfirmationTtlSeconds();
    }

    public int clearMaximumTargets() {
        return snapshot.clearMaximumTargets();
    }

    public int clearLargeRemovalThreshold() {
        return snapshot.clearLargeRemovalThreshold();
    }

    public int maximumRecipeIngredientCandidates() {
        return snapshot.maximumRecipeIngredientCandidates();
    }

    public int maximumCondenseBatches() {
        return snapshot.maximumCondenseBatches();
    }

    public int maximumCondenseRules() {
        return snapshot.maximumCondenseRules();
    }

    public boolean allowOversizedStacks() {
        return snapshot.allowOversizedStacks();
    }

    public int maximumOversizedStack() {
        return snapshot.maximumOversizedStack();
    }

    private record Snapshot(
            int maxLoreLines,
            int clearConfirmationTtlSeconds,
            int clearMaximumTargets,
            int clearLargeRemovalThreshold,
            int maximumRecipeIngredientCandidates,
            int maximumCondenseBatches,
            int maximumCondenseRules,
            boolean allowOversizedStacks,
            int maximumOversizedStack
    ) {

        private static Snapshot from(ItemConfig source) {
            var config = new ItemConfig();
            config.copyFrom(requireNonNull(source, "config"));
            config.validate();
            return new Snapshot(
                    config.maxLoreLines,
                    config.clearConfirmationTtlSeconds,
                    config.clearMaximumTargets,
                    config.clearLargeRemovalThreshold,
                    config.maximumRecipeIngredientCandidates,
                    config.maximumCondenseBatches,
                    config.maximumCondenseRules,
                    config.allowOversizedStacks,
                    config.maximumOversizedStack
            );
        }

    }

}
