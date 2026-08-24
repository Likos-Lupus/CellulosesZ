package top.likoslupus.cellulosesz.common.recipe;

import static java.util.Objects.requireNonNull;

public record CompressionRule(
        String recipeId,
        String inputItem,
        int inputCount,
        String outputItem,
        int outputCount
) {

    public CompressionRule {
        recipeId = requireNonNull(recipeId, "recipeId");
        inputItem = requireNonNull(inputItem, "inputItem");
        outputItem = requireNonNull(outputItem, "outputItem");
        if (inputCount < 2 || outputCount < 1) {
            throw new IllegalArgumentException("invalid compression quantities");
        }
        if (inputItem.equals(outputItem)) {
            throw new IllegalArgumentException("compression must change the item type");
        }
    }

}
