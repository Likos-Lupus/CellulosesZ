package top.likoslupus.cellulosesz.modules.item.command;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.item.ItemService;
import top.likoslupus.cellulosesz.api.recipe.RecipeDescription;
import top.likoslupus.cellulosesz.api.recipe.RecipePlatformService;
import top.likoslupus.cellulosesz.modules.item.ItemConfig;

import java.util.Map;
import java.util.stream.IntStream;

public final class RecipeCommand implements CellCommand {

    private final ItemService items;
    private final RecipePlatformService recipes;
    private final ItemConfig config;

    public RecipeCommand(
            ItemService items,
            RecipePlatformService recipes,
            ItemConfig config
    ) {
        this.items = items;
        this.recipes = recipes;
        this.config = config;
    }

    @Override
    public String permission() {
        return "cellulosesz.command.recipe";
    }

    @Override
    public String usage() {
        return "/recipe <item> [number]";
    }

    @Override
    public String name() {
        return "recipe";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        if (invocation.args().length < 1 || invocation.args().length > 2) return usage(invocation);
        var descriptor = items.parse(invocation.args()[0]);
        if (descriptor.isEmpty()) {
            invocation.errorKey("commands.item.recipe.invalid-item", Map.of("item", invocation.args()[0]));
            return 0;
        }
        var result = recipes.recipesFor(descriptor.orElseThrow()
                .normalizedItem(), config.maximumRecipeIngredientCandidates);
        if (!result.successful() || result.value().isEmpty()) {
            invocation.platformError(result.status());
            return 0;
        }
        var matches = result.value().orElseThrow();
        if (matches.isEmpty()) {
            invocation.errorKey("commands.item.recipe.none", Map.of("item", descriptor.orElseThrow().normalizedItem()));
            return 0;
        }
        if (matches.size() > config.maximumRecipeResults) {
            matches = matches.subList(0, config.maximumRecipeResults);
        }
        var number = 1;
        if (invocation.args().length == 2) {
            try {
                number = Integer.parseInt(invocation.args()[1]);
                if (number < 1 || number > matches.size()) throw new NumberFormatException();
            } catch (NumberFormatException failure) {
                invocation.errorKey("commands.item.recipe.invalid-number", Map.of("maximum", matches.size()));
                return 0;
            }
        }
        var recipe = matches.get(number - 1);
        show(invocation, recipe, number, matches.size());
        return 1;
    }

    private int usage(CommandInvocation invocation) {
        invocation.errorKey("commands.item.recipe.usage", Map.of("usage", usage()));
        return 0;
    }

    private static void show(CommandInvocation invocation, RecipeDescription recipe, int number, int total) {
        if (!recipe.displayable()) {
            invocation.errorKey("commands.item.recipe.unsupported", Map.of("id", recipe.id(), "type", recipe.type()));
            return;
        }
        invocation.replyKey("commands.item.recipe.header", Map.of(
                "number", number, "total", total, "id", recipe.id(), "type", recipe.type(),
                "width", recipe.width(), "height", recipe.height(),
                "output", recipe.outputItem(), "count", recipe.outputCount()
        ));
        IntStream.range(0, recipe.ingredients().size()).forEach(index -> {
            var ingredient = recipe.ingredients().get(index);
            invocation.replyKey("commands.item.recipe.ingredient", Map.of(
                    "slot", index + 1,
                    "tag", ingredient.tagId().orElse(""),
                    "candidates", String.join(", ", ingredient.candidates())
            ));
        });
    }

}
