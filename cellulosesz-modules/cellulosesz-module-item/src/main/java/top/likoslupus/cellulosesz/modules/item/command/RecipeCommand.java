package top.likoslupus.cellulosesz.modules.item.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.item.ItemService;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformOperationStatus;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.api.recipe.RecipePlatformService;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.common.command.CommandSuggestionSupport;
import top.likoslupus.cellulosesz.modules.item.ItemConfig;
import top.likoslupus.cellulosesz.modules.item.command.argument.ItemIdArgument;

import java.util.List;

import static java.util.Objects.requireNonNull;

public final class RecipeCommand implements CommandContributor {

    private final ItemService items;
    private final RecipePlatformService recipes;
    private final ItemConfig config;

    public RecipeCommand(
            ItemService items,
            RecipePlatformService recipes,
            ItemConfig config
    ) {
        this.items = requireNonNull(items, "items");
        this.recipes = requireNonNull(recipes, "recipes");
        this.config = requireNonNull(config, "config");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        var descriptor = ItemCommandSupport.descriptor(
                "recipe",
                "cellulosesz.command.recipe",
                CommandSourceKind.ANY
        );

        var item = Commands.argument(
                        "item",
                        ItemIdArgument.itemId(items)
                )
                .suggests((ignored, builder) ->
                        CommandSuggestionSupport.suggest(
                                items::names,
                                builder
                        )
                )
                .executes(command -> execute(
                        context,
                        command,
                        descriptor,
                        1
                ))
                .then(Commands.argument(
                                        "number",
                                        IntegerArgumentType.integer(1)
                                )
                                .executes(command -> execute(
                                        context,
                                        command,
                                        descriptor,
                                        IntegerArgumentType.getInteger(
                                                command,
                                                "number"
                                        )
                                ))
                );

        context.registerDirect(
                moduleId(),
                descriptor,
                List.of(),
                "commands.description.recipe",
                "/recipe <item> [number]",
                Commands.literal("recipe").then(item)
        );
    }

    private int execute(
            CommandRegistrationContext context,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor,
            int number
    ) {
        return ItemCommandSupport.sync(
                context,
                command,
                descriptor,
                "recipe number=" + number,
                _ -> {
                    var result = recipes.recipesFor(
                            ItemIdArgument.get(command, "item"),
                            config.maximumRecipeIngredientCandidates
                    );

                    if (!result.successful() || result.value().isEmpty()) {
                        return result;
                    }

                    var values = result.value().orElseThrow();

                    if (number > values.size()) {
                        return PlatformResult.failure(
                                PlatformOperationStatus.NOT_FOUND,
                                "recipe-number"
                        );
                    }

                    return PlatformResult.success(
                            values.get(number - 1)
                    );
                }
        );
    }

    @Override
    public String moduleId() {
        return ItemCommandSupport.MODULE;
    }

}
