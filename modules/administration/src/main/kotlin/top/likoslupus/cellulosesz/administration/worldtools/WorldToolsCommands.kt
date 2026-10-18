package top.likoslupus.cellulosesz.administration.worldtools

import top.likoslupus.cellulosesz.administration.config.WorldToolsSettings
import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.permission.PermissionService
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.presentation.TextPresentation

/**
 * World/environment tools. Every executor runs on the server thread; each mutates Minecraft state
 * directly through [WorldToolsService] and replies with a semantic notice.
 */
internal object WorldToolsCommands {

    fun commands(
        service: WorldToolsService,
        personal: PersonalViews,
        settings: () -> WorldToolsSettings,
        permissions: PermissionService,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): List<CommandDefinition> =
        WorldClockCommands.commands(service, adventure, presentation) +
                WorldProjectileCommands.commands(service, settings, permissions, adventure, presentation) +
                WorldTerrainCommands.commands(service, settings, permissions, adventure, presentation) +
                WorldInfoCommands.commands(service, adventure, presentation) +
                PersonalTimeCommands.commands(service, personal, adventure, presentation) +
                PersonalWeatherCommands.commands(service, personal, adventure, presentation)

}
