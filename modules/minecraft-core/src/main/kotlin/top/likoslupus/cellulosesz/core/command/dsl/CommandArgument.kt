package top.likoslupus.cellulosesz.core.command.dsl

import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.arguments.DoubleArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.arguments.DimensionArgument
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
 * A typed handle to one declared argument. The same instance is handed to the nested execution
 * block, so [ExecutionScope.get] can prove the argument belongs to the executing path.
 */
public class ArgumentKey<T : Any> internal constructor(
    public val name: String,
    internal val argument: CommandArgument<T>,
)
