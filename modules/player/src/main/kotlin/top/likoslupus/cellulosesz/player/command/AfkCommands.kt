package top.likoslupus.cellulosesz.player.command

import net.minecraft.commands.CommandSourceStack
import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.command.dsl.command
import top.likoslupus.cellulosesz.core.command.dsl.greedyString
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.presentation.TextPresentation
import top.likoslupus.cellulosesz.core.text.presentation.replyNotice
import top.likoslupus.cellulosesz.player.afk.AfkChange
import top.likoslupus.cellulosesz.player.afk.AfkService
import top.likoslupus.cellulosesz.player.format.*
import java.util.*

/** `/afk [reason?]`: toggles the caller's session-only AFK state. */
internal object AfkCommands {

    fun commands(
        afk: AfkService,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
        announce: (AfkChange) -> Unit,
    ): List<CommandDefinition> = listOf(
        command(
            name = "afk",
            category = CommandCategory.PLAYER,
            permission = CommandPermissions.AFK,
            documentation = "player/afk",
        ) {
            executesPlayer {
                toggle(
                    source,
                    playerId,
                    null,
                    afk,
                    adventure,
                    presentation,
                    announce
                )
            }
            argument("reason", greedyString()) { key ->
                executesPlayer {
                    toggle(
                        source,
                        playerId,
                        get(key),
                        afk,
                        adventure,
                        presentation,
                        announce
                    )
                }
            }
        },
    )

    private fun toggle(
        source: CommandSourceStack,
        playerId: UUID,
        reason: String?,
        afk: AfkService,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
        announce: (AfkChange) -> Unit,
    ): Int {
        if (!afk.enabled()) {
            return source.replyNotice(
                adventure,
                presentation,
                playerId,
                PlayerMessages.afkDisabled()
            )
        }
        val name = source.player?.gameProfile?.name
            ?: return 0
        val change = afk.toggle(playerId, name, reason)
        announce(change)
        return source.replyNotice(
            adventure,
            presentation,
            playerId,
            PlayerMessages.afkToggle(change)
        )
    }

}
