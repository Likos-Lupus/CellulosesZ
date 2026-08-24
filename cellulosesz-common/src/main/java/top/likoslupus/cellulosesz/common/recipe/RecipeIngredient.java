package top.likoslupus.cellulosesz.common.recipe;

import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public record RecipeIngredient(
        Optional<String> tagId,
        List<String> candidates
) {

    public RecipeIngredient {
        requireNonNull(tagId, "tagId");
        candidates = List.copyOf(requireNonNull(candidates, "candidates"));
    }

}
