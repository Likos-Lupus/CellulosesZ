package top.likoslupus.cellulosesz.utility.inspection

import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.command.dsl.command
import top.likoslupus.cellulosesz.core.command.dsl.word
import top.likoslupus.cellulosesz.core.command.requiresPermission
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.permission.PermissionService
import top.likoslupus.cellulosesz.core.player.KnownPlayerResolver
import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.adventure.replyError
import top.likoslupus.cellulosesz.core.text.i18n.LocalizedMessages
import top.likoslupus.cellulosesz.core.text.presentation.TextPresentation
import top.likoslupus.cellulosesz.core.text.presentation.replyNotice
import top.likoslupus.cellulosesz.utility.config.InspectionSettings
import top.likoslupus.cellulosesz.utility.format.UtilityMessages
import java.util.*

internal object InspectionCommands {

    fun commands(
        service: InspectionService,
        known: KnownPlayerResolver,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
        permissions: PermissionService,
        settings: () -> InspectionSettings,
    ): List<CommandDefinition> = listOf(
        command(
            name = "enderchest",
            category = CommandCategory.UTILITY,
            permission = CommandPermissions.ENDER_CHEST,
            documentation = "utility/enderchest",
        ) {
            executesPlayer {
                source.replyNotice(
                    adventure,
                    presentation,
                    playerId,
                    UtilityMessages.inspection(
                        service.enderChest(playerId, playerId)
                    )
                )
            }
            argument(
                "player",
                word(),
                permission = CommandPermissions.ENDER_CHEST_OTHERS,
            ) { player ->
                suggests { source.server.playerList.players.forEach { suggest(it.gameProfile.name) } }
                executesPlayer {
                    val targetId = known.onlineByName(get(player))?.id
                        ?: return@executesPlayer notFound(
                            context,
                            playerId,
                            messages,
                            adventure
                        )
                    source.replyNotice(
                        adventure,
                        presentation,
                        playerId,
                        UtilityMessages.inspection(
                            service.enderChest(playerId, targetId)
                        )
                    )
                }
            }
        },

        command(
            name = "disposal",
            category = CommandCategory.UTILITY,
            permission = CommandPermissions.DISPOSAL,
            documentation = "utility/disposal",
        ) {
            executesPlayer {
                source.replyNotice(
                    adventure,
                    presentation,
                    playerId,
                    UtilityMessages.inspection(service.disposal(playerId))
                )
            }
        },

        command(
            name = "invsee",
            category = CommandCategory.UTILITY,
            permission = CommandPermissions.INVSEE,
            documentation = "utility/invsee",
        ) {
            argument("player", word()) { player ->
                suggests { source.server.playerList.players.forEach { suggest(it.gameProfile.name) } }
                executesPlayer {
                    inspect(
                        context,
                        playerId,
                        get(player),
                        equip = false,
                        service,
                        known,
                        messages,
                        permissions,
                        settings,
                        adventure,
                        presentation
                    )
                }
                literal("equip", permission = CommandPermissions.INVSEE_EQUIP) {
                    executesPlayer {
                        inspect(
                            context,
                            playerId,
                            get(player),
                            equip = true,
                            service,
                            known,
                            messages,
                            permissions,
                            settings,
                            adventure,
                            presentation
                        )
                    }
                }
            }
        },
    )

    private fun inspect(
        context: CommandContext<CommandSourceStack>,
        viewerId: UUID,
        targetName: String,
        equip: Boolean,
        service: InspectionService,
        known: KnownPlayerResolver,
        messages: LocalizedMessages,
        permissions: PermissionService,
        settings: () -> InspectionSettings,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val source = context.source
        val targetId = known.onlineByName(targetName)?.id
            ?: return notFound(context, viewerId, messages, adventure)
        if (targetId == viewerId) {
            return source.replyNotice(
                adventure,
                presentation,
                viewerId,
                UtilityMessages.inspectionSelf()
            )
        }
        val editable = !equip
                && settings().editable
                && source.requiresPermission(permissions, CommandPermissions.INVSEE_EDIT)
        return source.replyNotice(
            adventure,
            presentation,
            viewerId,
            UtilityMessages.inspection(
                service.inspect(viewerId, targetId, editable, equip)
            )
        )
    }

    private fun notFound(
        context: CommandContext<CommandSourceStack>,
        playerId: UUID?,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
    ): Int =
        context.source.replyError(
            adventure,
            messages.renderFor(
                playerId,
                MessageKey("utility.error.player_not_found")
            ),
        )

}
