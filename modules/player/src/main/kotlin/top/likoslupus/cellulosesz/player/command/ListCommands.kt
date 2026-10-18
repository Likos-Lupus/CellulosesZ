package top.likoslupus.cellulosesz.player.command

import net.minecraft.commands.CommandSourceStack
import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.command.dsl.command
import top.likoslupus.cellulosesz.core.command.dsl.word
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.permission.PermissionService
import top.likoslupus.cellulosesz.core.permission.hasPermission
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.presentation.TextPresentation
import top.likoslupus.cellulosesz.core.text.presentation.replyNotice
import top.likoslupus.cellulosesz.core.text.presentation.replyPage
import top.likoslupus.cellulosesz.player.config.PlayerSettings
import top.likoslupus.cellulosesz.player.format.*
import top.likoslupus.cellulosesz.player.list.PlayerListResult
import top.likoslupus.cellulosesz.player.list.PlayerListService
import java.util.*

/**
 * `/list [group]`: a summary plus the online players grouped by permission. AFK players are marked
 * with the configured `players.afk.listName`; vanished players are hidden unless the viewer holds
 * `LIST_HIDDEN`. Vanish visibility arrives as an injected query so the player module never depends on
 * the vanish owner.
 */
internal object ListCommands {

    fun commands(
        list: PlayerListService,
        permissions: PermissionService,
        settings: () -> PlayerSettings,
        visibility: (UUID, UUID) -> Boolean,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): List<CommandDefinition> = listOf(
        command(
            name = "list",
            category = CommandCategory.PLAYER,
            permission = CommandPermissions.LIST,
            documentation = "player/list",
        ) {
            executes {
                show(
                    source,
                    null,
                    list,
                    permissions,
                    settings,
                    visibility,
                    adventure,
                    presentation
                )
            }
            argument("group", word()) { key ->
                suggests { suggestAll(groupNames(settings())) }
                executes {
                    show(
                        source,
                        get(key),
                        list,
                        permissions,
                        settings,
                        visibility,
                        adventure,
                        presentation
                    )
                }
            }
        },
    )

    private fun groupNames(settings: PlayerSettings): List<String> =
        settings.list.groups.flatMap { listOf(it.id, it.label) } +
                listOf(settings.list.defaultGroup.id, settings.list.defaultGroup.label)

    private fun show(
        source: CommandSourceStack,
        requestedGroup: String?,
        list: PlayerListService,
        permissions: PermissionService,
        settings: () -> PlayerSettings,
        visibility: (UUID, UUID) -> Boolean,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val viewerId = source.player?.uuid
        val includeVanished = source.hasPermission(
            permissions,
            CommandPermissions.LIST_HIDDEN
        )
        val result = list.list(
            viewerId = viewerId,
            includeVanished = includeVanished,
            requestedGroup = requestedGroup,
            visibility = visibility,
        )
        return when (result) {
            is PlayerListResult.Rows -> source.replyPage(
                adventure,
                presentation,
                viewerId,
                PlayerMessages.listPage(result, settings().afk.listName),
            )

            PlayerListResult.UnknownGroup -> source.replyNotice(
                adventure,
                presentation,
                viewerId,
                PlayerMessages.listUnknownGroup(requestedGroup.orEmpty()),
            )
        }
    }

}
