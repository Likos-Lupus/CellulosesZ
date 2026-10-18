package top.likoslupus.cellulosesz.application.command

import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition

/**
 * A late-bound view of the registered declarations, used by `/cellulosesz help`. The root command is
 * declared before the registry exists, so it captures this holder and the registry fills it in after
 * validation; no command definition is duplicated.
 */
internal class CommandHelpCatalog {

    var definitions: List<CommandDefinition> = emptyList()

}
