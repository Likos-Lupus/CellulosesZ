package top.likoslupus.cellulosesz.administration.worldtools

import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.command.dsl.command
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.presentation.TextPresentation
import top.likoslupus.cellulosesz.core.text.presentation.replyDetail

/** `/gc`. */
internal object WorldInfoCommands {

    fun commands(
        service: WorldToolsService,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): List<CommandDefinition> = listOf(
        gcCommand(service, adventure, presentation),
    )

    // ---------------------------------------------------------------- /gc

    private fun gcCommand(
        service: WorldToolsService,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): CommandDefinition =
        command(
            name = "gc",
            category = CommandCategory.ADMINISTRATION,
            permission = CommandPermissions.GC,
            documentation = "administration/gc",
        ) {
            executes {
                val source = context.source
                source.replyDetail(
                    adventure,
                    presentation,
                    source.player?.uuid,
                    WorldToolsMessages.stats(service.stats(source.server)),
                )
            }
        }

}
