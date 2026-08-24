package top.likoslupus.cellulosesz.common.recipe;

import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;

import java.util.List;
import java.util.Optional;

public interface RecipePlatformService {

    PlatformResult<List<RecipeDescription>> recipesFor(String itemId, int ingredientCandidateLimit);

    PlatformResult<List<CompressionRule>> compressionRules(
            Optional<String> inputItem,
            int maximumRules
    );

}
