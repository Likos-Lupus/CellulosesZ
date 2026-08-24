package top.likoslupus.cellulosesz.modules.item.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.item.ItemArgument;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.item.ItemDescriptor;
import top.likoslupus.cellulosesz.api.item.ItemService;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformOperationStatus;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.common.command.CommandContributor;
import top.likoslupus.cellulosesz.common.command.CommandRegistrationContext;
import top.likoslupus.cellulosesz.common.command.CommandSuggestionSupport;
import top.likoslupus.cellulosesz.common.recipe.RecipePlatformService;
import top.likoslupus.cellulosesz.modules.item.ItemRuntimeSettings;
import top.likoslupus.cellulosesz.modules.item.command.argument.ItemDescriptors;

import java.util.List;

import static java.util.Objects.requireNonNull;

public final class RecipeCommand implements CommandContributor {

    private final ItemService items;
    private final RecipePlatformService recipes;
    private final ItemRuntimeSettings config;

    public RecipeCommand(
            ItemService items,
            RecipePlatformService recipes,
            ItemRuntimeSettings config
    ) {
        this.items = requireNonNull(items, "items");
        this.recipes = requireNonNull(recipes, "recipes");
        this.config = requireNonNull(config, "config");
    }

    @Override
    public void register(CommandRegistrationContext context) {
        ItemDescriptors.prepare(items);
        var descriptor = ItemCommandSupport.descriptor(
                "recipe",
                "cellulosesz.command.recipe",
                CommandSourceKind.ANY
        );
        var root = Commands.literal("recipe")
                .then(configure(
                        context,
                        descriptor,
                        Commands.argument("item", ItemArgument.item(context.buildContext())),
                        "item",
                        true
                ))
                .then(configure(
                        context,
                        descriptor,
                        Commands.argument("itemAlias", StringArgumentType.word())
                                .suggests((_, builder) -> CommandSuggestionSupport.suggest(
                                        items::itemNames,
                                        builder
                                )),
                        "itemAlias",
                        false
                ));

        context.registerDirect(
                moduleId(), descriptor, List.of(), "commands.description.recipe",
                "/recipe <item> [number]", root
        );
    }

    private RequiredArgumentBuilder<CommandSourceStack, ?> configure(
            CommandRegistrationContext context,
            CommandDescriptor descriptor,
            RequiredArgumentBuilder<CommandSourceStack, ?> argument,
            String name,
            boolean vanilla
    ) {
        return argument
                .executes(command -> execute(
                        context,
                        command,
                        descriptor,
                        item(command, name, vanilla),
                        1
                ))
                .then(Commands.argument("number", IntegerArgumentType.integer(1))
                        .executes(command -> execute(
                                context,
                                command,
                                descriptor,
                                item(command, name, vanilla),
                                IntegerArgumentType.getInteger(command, "number")
                        ))
                );
    }

    private ItemDescriptor item(
            CommandContext<CommandSourceStack> command,
            String name,
            boolean vanilla
    ) throws CommandSyntaxException {
        return vanilla
                ? ItemDescriptors.vanilla(command, name)
                : ItemDescriptors.custom(command, name, items);
    }

    private int execute(
            CommandRegistrationContext context,
            CommandContext<CommandSourceStack> command,
            CommandDescriptor descriptor,
            ItemDescriptor item,
            int number
    ) {
        return ItemCommandSupport.sync(
                context,
                command,
                descriptor,
                "recipe number=" + number,
                _ -> {
                    var result = recipes.recipesFor(
                            item.normalizedItem(),
                            config.maximumRecipeIngredientCandidates()
                    );

                    if (!result.successful() || result.value().isEmpty()) {
                        return result;
                    }

                    var values = result.value();

                    if (number > values.size()) {
                        return PlatformResult.failure(
                                PlatformOperationStatus.NOT_FOUND,
                                "recipe-number"
                        );
                    }

                    return PlatformResult.success(values.get(number - 1));
                }
        );
    }

    @Override
    public String moduleId() {
        return ItemCommandSupport.MODULE;
    }

}
