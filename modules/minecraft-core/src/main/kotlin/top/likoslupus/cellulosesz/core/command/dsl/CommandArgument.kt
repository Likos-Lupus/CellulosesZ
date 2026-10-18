package top.likoslupus.cellulosesz.core.command.dsl

import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.arguments.DoubleArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.arguments.DimensionArgument
import net.minecraft.commands.arguments.MessageArgument
import net.minecraft.commands.arguments.item.ItemArgument
import net.minecraft.commands.arguments.item.ItemInput
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier

/**
 * A Brigadier argument type together with the typed accessor used by [ExecutionScope.get]. Distinct
 * factory functions exist for the string shapes because Brigadier treats them differently; the raw
 * name is carried so the declared node keeps its real Brigadier identifier.
 */
public class CommandArgument<T : Any> internal constructor(
    private val factory: (CommandBuildContext) -> ArgumentType<T>,
    private val extractor: (CommandContext<CommandSourceStack>, String) -> T,
    internal val greedy: Boolean = false,
) {

    internal fun createType(buildContext: CommandBuildContext): ArgumentType<T> =
        factory(buildContext)

    internal fun read(context: CommandContext<CommandSourceStack>, name: String): T =
        extractor(context, name)

}

/** A single word (no spaces). */
public fun word(): CommandArgument<String> =
    CommandArgument(
        { StringArgumentType.word() },
        StringArgumentType::getString
    )

/** A quoted string that may contain spaces. */
public fun quotedString(): CommandArgument<String> =
    CommandArgument(
        { StringArgumentType.string() },
        StringArgumentType::getString
    )

/** The remainder of the line, including spaces. Must terminate a branch. */
public fun greedyString(): CommandArgument<String> =
    CommandArgument(
        { StringArgumentType.greedyString() },
        StringArgumentType::getString,
        greedy = true
    )

/** A bounded integer. */
public fun integer(
    min: Int = Int.MIN_VALUE,
    max: Int = Int.MAX_VALUE
): CommandArgument<Int> =
    CommandArgument(
        { IntegerArgumentType.integer(min, max) },
        IntegerArgumentType::getInteger
    )

/** A bounded double. */
public fun double(
    min: Double = -Double.MAX_VALUE,
    max: Double = Double.MAX_VALUE,
): CommandArgument<Double> =
    CommandArgument(
        { DoubleArgumentType.doubleArg(min, max) },
        DoubleArgumentType::getDouble
    )

/**
 * A vanilla message argument resolving to a chat [Component]. The Brigadier node is the real
 * `MessageArgument`, whose generic value is its internal `Message` record rather than a component;
 * the typed value is therefore resolved through `MessageArgument.getMessage`. The factory cast only
 * aligns the DSL accessor type, the argument node still carries the genuine `MessageArgument`.
 */
@Suppress("UNCHECKED_CAST")
public fun message(): CommandArgument<Component> =
    CommandArgument(
        { MessageArgument.message() as ArgumentType<Component> },
        { context, name -> MessageArgument.getMessage(context, name) },
    )

/** A vanilla dimension argument resolving to the dimension identifier. */
public fun dimension(): CommandArgument<Identifier> =
    CommandArgument(
        { DimensionArgument.dimension() },
        { context, name ->
            context.getArgument(
                name,
                Identifier::class.java
            )
        },
    )

/**
 * A vanilla item argument in the `id[component=value,...]` form. Its ceiling is [ItemInput], which
 * keeps the component patch intact so the caller can materialize an exact stack with
 * `createItemStack(count)` (strict validation included) instead of re-parsing a string.
 */
public fun item(): CommandArgument<ItemInput> =
    CommandArgument(
        { buildContext -> ItemArgument.item(buildContext) },
        { context, name -> ItemArgument.getItem(context, name) },
    )

/**
 * A typed handle to one declared argument. The same instance is handed to the nested execution
 * block, so [ExecutionScope.get] can prove the argument belongs to the executing path.
 */
public class ArgumentKey<T : Any> internal constructor(
    public val name: String,
    internal val argument: CommandArgument<T>,
)
