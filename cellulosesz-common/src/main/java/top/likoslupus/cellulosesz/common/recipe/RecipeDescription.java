package top.likoslupus.cellulosesz.common.recipe;

import java.util.List;

import static java.util.Objects.requireNonNull;

public record RecipeDescription(
        String id,
        String type,
        int width,
        int height,
        List<RecipeIngredient> ingredients,
        String outputItem,
        int outputCount,
        boolean displayable
) {

    public RecipeDescription {
        id = requireNonNull(id, "id");
        type = requireNonNull(type, "type");
        ingredients = List.copyOf(requireNonNull(ingredients, "ingredients"));
        outputItem = requireNonNull(outputItem, "outputItem");
        if (width < 0 || height < 0 || outputCount < 1) {
            throw new IllegalArgumentException("recipe dimensions and output count are invalid");
        }
    }

}
