package top.likoslupus.cellulosesz.fabric;

import net.fabricmc.fabric.api.recipe.v1.FabricRecipeManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.api.recipe.CompressionRule;
import top.likoslupus.cellulosesz.api.recipe.RecipeDescription;
import top.likoslupus.cellulosesz.api.recipe.RecipeIngredient;
import top.likoslupus.cellulosesz.api.recipe.RecipePlatformService;
import top.likoslupus.cellulosesz.fabric.mixin.RecipeResultAccessor;

import java.util.*;

final class FabricRecipeOperations implements RecipePlatformService {

    private static final List<RecipeType<?>> STANDARD_TYPES = List.of(
            RecipeType.CRAFTING,
            RecipeType.SMELTING,
            RecipeType.BLASTING,
            RecipeType.SMOKING,
            RecipeType.CAMPFIRE_COOKING,
            RecipeType.STONECUTTING,
            RecipeType.SMITHING
    );

    private final FabricPlatformService platform;

    FabricRecipeOperations(FabricPlatformService platform) {
        this.platform = platform;
    }

    @Override
    public PlatformResult<List<RecipeDescription>> recipesFor(String itemId, int ingredientCandidateLimit) {
        if (ingredientCandidateLimit < 1)
            return PlatformResult.failure(top.likoslupus.cellulosesz.api.platform.operation.PlatformOperationStatus.INVALID_ARGUMENT, "ingredient candidate limit must be positive");
        var normalized = normalize(itemId);
        if (!platform.validItem(normalized))
            return PlatformResult.failure(top.likoslupus.cellulosesz.api.platform.operation.PlatformOperationStatus.INVALID_ARGUMENT, "unknown item");
        try {
            var descriptions = allDescriptions(ingredientCandidateLimit).stream()
                    .filter(description -> description.outputItem().equals(normalized))
                    .sorted(java.util.Comparator.comparing(RecipeDescription::id))
                    .toList();
            return PlatformResult.success(descriptions);
        } catch (RuntimeException failure) {
            return PlatformResult.failure(top.likoslupus.cellulosesz.api.platform.operation.PlatformOperationStatus.INTERNAL_ERROR, failure.getClass()
                    .getSimpleName());
        }
    }

    @Override
    public PlatformResult<List<CompressionRule>> compressionRules(Optional<String> inputItem, int maximumRules) {
        if (maximumRules < 1)
            return PlatformResult.failure(top.likoslupus.cellulosesz.api.platform.operation.PlatformOperationStatus.INVALID_ARGUMENT, "maximum rule count must be positive");
        var normalizedFilter = inputItem.map(FabricRecipeOperations::normalize);
        try {
            var byInput = new LinkedHashMap<String, CompressionRule>();
            for (var description : allDescriptions(1)) {
                if (!description.displayable() || !description.type().equals("crafting")) continue;
                if (description.ingredients().size() < 2 || description.outputCount() < 1) continue;
                String input = null;
                var count = 0;
                var safe = true;
                for (var ingredient : description.ingredients()) {
                    if (ingredient.candidates().size() != 1) {
                        safe = false;
                        break;
                    }
                    var candidate = ingredient.candidates().getFirst();
                    if (input == null) input = candidate;
                    else if (!input.equals(candidate)) {
                        safe = false;
                        break;
                    }
                    count++;
                }
                if (!safe || input == null || count < 2 || input.equals(description.outputItem())) continue;
                if (normalizedFilter.isPresent() && !normalizedFilter.orElseThrow().equals(input)) continue;
                var rule = new CompressionRule(
                        description.id(), input, count, description.outputItem(), description.outputCount()
                );
                var previous = byInput.get(input);
                if (previous == null || rule.inputCount() > previous.inputCount()) byInput.put(input, rule);
                if (byInput.size() >= maximumRules) break;
            }
            return PlatformResult.success(List.copyOf(byInput.values()));
        } catch (RuntimeException failure) {
            return PlatformResult.failure(top.likoslupus.cellulosesz.api.platform.operation.PlatformOperationStatus.INTERNAL_ERROR, failure.getClass()
                    .getSimpleName());
        }
    }

    private static String normalize(String value) {
        var normalized = value.strip().toLowerCase(Locale.ROOT);
        return normalized.contains(":") ? normalized : "minecraft:" + normalized;
    }

    private List<RecipeDescription> allDescriptions(int candidateLimit) {
        var manager = (FabricRecipeManager) platform.requireServer().getRecipeManager();
        var result = new ArrayList<RecipeDescription>();
        for (var type : STANDARD_TYPES) appendType(manager, type, candidateLimit, result);
        return result;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void appendType(
            FabricRecipeManager manager,
            RecipeType type,
            int candidateLimit,
            List<RecipeDescription> result
    ) {
        for (Object entry : manager.getAllOfType(type)) {
            result.add(describe((RecipeHolder<?>) entry, candidateLimit));
        }
    }

    private static RecipeDescription describe(RecipeHolder<?> holder, int candidateLimit) {
        var recipe = holder.value();
        var output = fixedResult(recipe);
        var ingredients = recipe.placementInfo().ingredients().stream()
                .map(ingredient -> new RecipeIngredient(
                        Optional.empty(),
                        ingredient.items()
                                .map(item -> BuiltInRegistries.ITEM.getKey(item.value()).toString())
                                .distinct()
                                .limit(candidateLimit)
                                .toList()
                ))
                .toList();
        var dimensions = dimensions(recipe, ingredients.size());
        return new RecipeDescription(
                holder.id().location().toString(), typeName(recipe), dimensions[0], dimensions[1], ingredients,
                output.map(FabricRecipeOperations::itemId).orElse("minecraft:air"),
                output.map(ItemStack::getCount).orElse(1), output.isPresent()
        );
    }

    private static Optional<ItemStack> fixedResult(Recipe<?> recipe) {
        if (recipe instanceof ShapedRecipe
                || recipe instanceof ShapelessRecipe
                || recipe instanceof AbstractCookingRecipe
                || recipe instanceof SingleItemRecipe
                || recipe instanceof SmithingTransformRecipe) {
            var stack = ((RecipeResultAccessor) recipe).cellulosesz$result();
            return stack.isEmpty() ? Optional.empty() : Optional.of(stack.copy());
        }
        return Optional.empty();
    }

    private static int[] dimensions(Recipe<?> recipe, int ingredientCount) {
        if (recipe instanceof ShapedRecipe) {
            if (ingredientCount <= 1) return new int[]{1, 1};
            if (ingredientCount <= 4) return new int[]{2, (ingredientCount + 1) / 2};
            return new int[]{3, (ingredientCount + 2) / 3};
        }
        return new int[]{0, 0};
    }

    private static String typeName(Recipe<?> recipe) {
        if (recipe instanceof ShapedRecipe) return "crafting";
        if (recipe instanceof ShapelessRecipe) return "crafting";
        var key = BuiltInRegistries.RECIPE_TYPE.getKey(recipe.getType());
        return key == null ? recipe.getType().toString().toLowerCase(Locale.ROOT) : key.getPath();
    }

    private static String itemId(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

}
