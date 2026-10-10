package top.likoslupus.cellulosesz.core.command.registry

import com.mojang.brigadier.CommandDispatcher
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.CommandSourceStack
import top.likoslupus.cellulosesz.core.command.CommandCatalog
import top.likoslupus.cellulosesz.core.command.CommandDescriptor
import top.likoslupus.cellulosesz.core.command.dsl.BrigadierCompiler
import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.command.dsl.RegisteredCommand
import top.likoslupus.cellulosesz.core.permission.PermissionService

/**
 * The complete set of CellulosesZ command declarations. It validates cross-definition uniqueness
 * (literal, alias, permission node) up front and rebuilds the Brigadier trees on every registration
 * event, so it never retains a node bound to a stale dispatcher.
 */
public class CommandRegistry(
    public val definitions: List<CommandDefinition>,
) {

    init {
        val problems = CommandCatalog.validate(definitions.map(CommandDefinition::toDescriptor))
        require(problems.isEmpty()) {
            "invalid command registry: ${problems.joinToString("; ")}"
        }
    }

    public fun register(
        dispatcher: CommandDispatcher<CommandSourceStack>,
        buildContext: CommandBuildContext,
        permissions: PermissionService,
    ): RegisteredCommands {
        val registered = definitions.map { definition ->
            BrigadierCompiler.register(dispatcher, definition, permissions, buildContext)
        }
        return RegisteredCommands(registered)
    }

}

/** The result of one registration pass: the definitions and the nodes they produced. */
public class RegisteredCommands internal constructor(
    registered: List<RegisteredCommand>,
) {

    private val byName: Map<String, RegisteredCommand> = buildMap {
        registered.forEach { command ->
            put(command.definition.name, command)
            command.definition.aliases.forEach { alias -> put(alias, command) }
        }
    }

    public val all: Collection<RegisteredCommand>
        get() = byName.values.toSet()

    public fun registered(literal: String): RegisteredCommand? =
        byName[literal]

    public fun ownerOf(literal: String): CommandDefinition? =
        byName[literal]?.definition

}

private fun CommandDefinition.toDescriptor(): CommandDescriptor =
    CommandDescriptor(
        literal = name,
        category = category,
        permission = permission,
        playerOnly = playerOnly,
        aliases = aliases,
    )
