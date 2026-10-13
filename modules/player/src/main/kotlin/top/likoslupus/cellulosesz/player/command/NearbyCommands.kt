package top.likoslupus.cellulosesz.player.command

import net.minecraft.commands.CommandSourceStack
import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.command.dsl.command
import top.likoslupus.cellulosesz.core.command.dsl.integer
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.permission.PermissionService
import top.likoslupus.cellulosesz.core.permission.hasPermission
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.presentation.TextPresentation
import top.likoslupus.cellulosesz.core.text.presentation.replyPage
import top.likoslupus.cellulosesz.player.format.PlayerMessages
import top.likoslupus.cellulosesz.player.nearby.NearbyService
import java.util.*

/** `/near [radius]`: lists same-dimension players within the clamped radius. */
internal object NearbyCommands {

    fun commands(
        nearby: NearbyService,
        permissions: PermissionService,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): List<CommandDefinition> = listOf(
        command(
            name = "near",
            category = CommandCategory.PLAYER,
            permission = CommandPermissions.NEAR,
            documentation = "player/near",
        ) {
            executesPlayer {
                show(
                    source,
                    playerId,
                    null,
                    nearby,
                    permissions,
                    adventure,
                    presentation
                )
            }
            argument("radius", integer(1)) { key ->
                executesPlayer {
                    show(
                        source,
                        playerId,
                        get(key),
                        nearby,
                        permissions,
                        adventure,
                        presentation
                    )
                }
            }
        },
    )

    private fun show(
        source: CommandSourceStack,
        playerId: UUID,
        radius: Int?,
        nearby: NearbyService,
        permissions: PermissionService,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int =
        source.replyPage(
            adventure,
            presentation,
            playerId,
            PlayerMessages.near(
                nearby.nearby(
                    playerId,
                    radius,
                    allowOverMaximum = source.hasPermission(
                        permissions,
                        CommandPermissions.NEAR_BYPASS_RADIUS,
                    ),
                )
            )
        )

}
