package top.likoslupus.cellulosesz.administration.playerstate

import net.minecraft.commands.CommandSourceStack
import net.minecraft.server.level.ServerPlayer
import top.likoslupus.cellulosesz.administration.format.*
import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.command.dsl.command
import top.likoslupus.cellulosesz.core.command.dsl.word
import top.likoslupus.cellulosesz.core.command.requiresPermission
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.permission.PermissionService
import top.likoslupus.cellulosesz.core.player.PlayerResolver
import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.adventure.replyError
import top.likoslupus.cellulosesz.core.text.i18n.LocalizedMessages
import top.likoslupus.cellulosesz.core.text.presentation.TextPresentation
import top.likoslupus.cellulosesz.core.text.presentation.replyNotice
import java.util.*

/**
 * Operator control over individual player state (health, food, flight, invulnerability). This is not
 * moderation: nothing here is a sanction or is persisted.
 */
internal object PlayerStateCommands {

    fun commands(
        state: PlayerStateService,
        permissions: PermissionService,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): List<CommandDefinition> = listOf(
        command(
            name = "heal",
            category = CommandCategory.ADMINISTRATION,
            permission = CommandPermissions.HEAL,
            documentation = "administration/heal",
        ) {
            executesPlayer {
                heal(
                    source,
                    null,
                    state,
                    permissions,
                    messages,
                    adventure,
                    presentation
                )
            }
            argument("player", word(), permission = CommandPermissions.HEAL_OTHERS) { player ->
                executesPlayer {
                    heal(
                        source,
                        get(player),
                        state,
                        permissions,
                        messages,
                        adventure,
                        presentation
                    )
                }
            }
        },

        command(
            name = "feed",
            category = CommandCategory.ADMINISTRATION,
            permission = CommandPermissions.FEED,
            documentation = "administration/feed",
        ) {
            executesPlayer {
                feed(
                    source,
                    null,
                    messages,
                    adventure,
                    presentation
                )
            }
            argument("player", word()) { player ->
                executesPlayer {
                    feed(
                        source,
                        get(player),
                        messages,
                        adventure,
                        presentation
                    )
                }
            }
        },

        command(
            name = "fly",
            category = CommandCategory.ADMINISTRATION,
            permission = CommandPermissions.FLY,
            documentation = "administration/fly",
        ) {
            executesPlayer {
                fly(
                    source,
                    null,
                    state,
                    messages,
                    adventure,
                    presentation
                )
            }
            argument(
                "player",
                word(),
                permission = CommandPermissions.FLY_OTHERS
            ) { player ->
                executesPlayer {
                    fly(
                        source,
                        get(player),
                        state,
                        messages,
                        adventure,
                        presentation
                    )
                }
            }
        },

        command(
            name = "god",
            category = CommandCategory.ADMINISTRATION,
            permission = CommandPermissions.GOD,
            documentation = "administration/god",
        ) {
            executesPlayer {
                god(
                    source,
                    null,
                    state,
                    messages,
                    adventure,
                    presentation
                )
            }
            argument(
                "player",
                word(),
                permission = CommandPermissions.GOD_OTHERS
            ) { player ->
                executesPlayer {
                    god(
                        source,
                        get(player),
                        state,
                        messages,
                        adventure,
                        presentation
                    )
                }
            }
        },
    )

    private fun heal(
        source: CommandSourceStack,
        targetName: String?,
        state: PlayerStateService,
        permissions: PermissionService,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val targetId = resolveTargetId(source, targetName)
            ?: return playerNotFound(source, messages, adventure)
        val bypass = source.requiresPermission(
            permissions,
            CommandPermissions.HEAL_COOLDOWN_BYPASS
        )
        val actorId = source.player?.uuid ?: targetId
        return source.replyNotice(
            adventure,
            presentation,
            source.player?.uuid,
            AdministrationMessages.heal(
                state.heal(
                    actorId,
                    targetId,
                    bypass
                )
            ),
        )
    }

    private fun feed(
        source: CommandSourceStack,
        targetName: String?,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val target = resolveTarget(source, targetName)
            ?: return playerNotFound(source, messages, adventure)
        target.foodData.foodLevel = 20
        target.foodData.setSaturation(20f)
        return source.replyNotice(
            adventure,
            presentation,
            source.player?.uuid,
            AdministrationMessages.fed(target.name.string),
        )
    }

    private fun fly(
        source: CommandSourceStack,
        targetName: String?,
        state: PlayerStateService,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val targetId = resolveTargetId(source, targetName)
            ?: return playerNotFound(source, messages, adventure)
        return source.replyNotice(
            adventure,
            presentation,
            source.player?.uuid,
            AdministrationMessages.flight(state.toggleFly(targetId)),
        )
    }

    private fun god(
        source: CommandSourceStack,
        targetName: String?,
        state: PlayerStateService,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val targetId = resolveTargetId(source, targetName)
            ?: return playerNotFound(source, messages, adventure)
        return source.replyNotice(
            adventure,
            presentation,
            source.player?.uuid,
            AdministrationMessages.god(state.toggleGod(targetId)),
        )
    }

    private fun resolveTargetId(source: CommandSourceStack, targetName: String?): UUID? =
        when (targetName) {
            null -> source.player?.uuid
            else -> PlayerResolver.onlineByName(source.server, targetName)?.uuid
        }

    private fun resolveTarget(
        source: CommandSourceStack,
        targetName: String?,
    ): ServerPlayer? =
        when (targetName) {
            null -> source.player
            else -> PlayerResolver.onlineByName(source.server, targetName)
        }

    private fun playerNotFound(
        source: CommandSourceStack,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
    ): Int =
        source.replyError(
            adventure,
            messages.renderFor(
                source.player?.uuid,
                MessageKey("administration.playerstate.player_not_found"),
            ),
        )

}
