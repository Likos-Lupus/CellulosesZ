package top.likoslupus.cellulosesz.utility.itemtools

import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.permission.PermissionService
import top.likoslupus.cellulosesz.core.player.KnownPlayerResolver
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.i18n.LocalizedMessages
import top.likoslupus.cellulosesz.core.text.presentation.TextPresentation

internal object ItemToolsCommands {

    fun commands(
        service: ItemToolsService,
        known: KnownPlayerResolver,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
        permissions: PermissionService,
    ): List<CommandDefinition> =
        ItemGrantCommands.commands(
            service,
            adventure,
            presentation,
        ) + ItemEnchantCommands.definitions(
            service,
            known,
            messages,
            adventure,
            presentation,
            permissions
        ) + ItemDisplayCommands.definitions(
            service,
            messages,
            adventure,
            presentation,
            permissions
        )

}
