package top.likoslupus.cellulosesz.movement.request

import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.command.dsl.command
import top.likoslupus.cellulosesz.core.command.dsl.word
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.player.PlayerResolver
import top.likoslupus.cellulosesz.core.preference.PlayerPreference
import top.likoslupus.cellulosesz.core.preference.PlayerPreferenceService
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.adventure.replyError
import top.likoslupus.cellulosesz.core.text.i18n.LocalizedMessages
import top.likoslupus.cellulosesz.core.text.i18n.renderFor
import top.likoslupus.cellulosesz.core.text.presentation.*
import top.likoslupus.cellulosesz.movement.command.movementLaunch
import top.likoslupus.cellulosesz.movement.format.*
import java.util.*

/** `/tpauto`: the caller's auto-accept mode and allow list. */
internal object TeleportAutoCommands {

    fun commands(
        tpAuto: TpAutoService,
        preferences: PlayerPreferenceService,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): List<CommandDefinition> = listOf(
        command(
            name = "tpauto",
            category = CommandCategory.MOVEMENT,
            permission = CommandPermissions.TPAUTO,
            documentation = "movement/tpauto",
        ) {
            executesPlayer {
                status(
                    context,
                    tpAuto,
                    preferences,
                    adventure,
                    presentation,
                )
            }
            literal("off") {
                executesPlayer {
                    setMode(
                        context,
                        TpAutoMode.OFF,
                        tpAuto,
                        preferences,
                        kernel,
                        messages,
                        adventure,
                        presentation,
                    )
                }
            }
            literal("trusted") {
                executesPlayer {
                    setMode(
                        context,
                        TpAutoMode.TRUSTED,
                        tpAuto,
                        preferences,
                        kernel,
                        messages,
                        adventure,
                        presentation,
                    )
                }
            }
            literal("all") {
                executesPlayer {
                    setMode(
                        context,
                        TpAutoMode.ALL,
                        tpAuto,
                        preferences,
                        kernel,
                        messages,
                        adventure,
                        presentation,
                    )
                }
            }
            literal("list") {
                executesPlayer {
                    list(
                        context,
                        tpAuto,
                        preferences,
                        adventure,
                        presentation,
                    )
                }
            }
            literal("allow") {
                argument("player", word()) { player ->
                    executesPlayer {
                        changeAllowed(
                            context,
                            get(player),
                            true,
                            tpAuto,
                            preferences,
                            kernel,
                            messages,
                            adventure,
                            presentation,
                        )
                    }
                }
            }
            literal("deny") {
                argument("player", word()) { player ->
                    executesPlayer {
                        changeAllowed(
                            context,
                            get(player),
                            false,
                            tpAuto,
                            preferences,
                            kernel,
                            messages,
                            adventure,
                            presentation,
                        )
                    }
                }
            }
        },
    )

    private fun status(
        context: CommandContext<CommandSourceStack>,
        tpAuto: TpAutoService,
        preferences: PlayerPreferenceService,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val source = context.source
        val player = source.player
            ?: return 0
        teleportDisabled(source, player.uuid, preferences, adventure, presentation)
                ?.let { return it }
        return source.replyNotices(
            adventure,
            presentation,
            player.uuid,
            listOf(
                MovementMessages.tpAutoStatus(tpAuto.mode(player.uuid)),
                MovementMessages.tpAutoHelp(),
            ),
        )
    }

    private fun setMode(
        context: CommandContext<CommandSourceStack>,
        mode: TpAutoMode,
        tpAuto: TpAutoService,
        preferences: PlayerPreferenceService,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val source = context.source
        val player = source.player
            ?: return 0
        val playerId = player.uuid
        teleportDisabled(source, playerId, preferences, adventure, presentation)
                ?.let { return it }
        return movementLaunch(
            source,
            kernel,
            messages,
            adventure,
            MessageKey("movement.pending.updating_auto"),
        ) { target ->
            kernel.sendNotices(
                target,
                adventure,
                presentation,
                playerId,
                listOf(
                    MovementMessages.tpAutoToggle(
                        tpAuto.setMode(playerId, mode)
                    )
                ),
            )
        }
    }

    private fun list(
        context: CommandContext<CommandSourceStack>,
        tpAuto: TpAutoService,
        preferences: PlayerPreferenceService,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val source = context.source
        val player = source.player
            ?: return 0
        teleportDisabled(source, player.uuid, preferences, adventure, presentation)
                ?.let { return it }
        val names = tpAuto.allowedBy(player.uuid)
                .map {
                    PlayerResolver.onlineById(source.server, it)?.name?.string
                        ?: it.toString()
                }
        return source.replyPage(
            adventure,
            presentation,
            player.uuid,
            MovementMessages.tpAutoList(names),
        )
    }

    private fun changeAllowed(
        context: CommandContext<CommandSourceStack>,
        targetName: String,
        add: Boolean,
        tpAuto: TpAutoService,
        preferences: PlayerPreferenceService,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val source = context.source
        val player = source.player
            ?: return source.replyError(
                adventure,
                messages.renderFor(
                    null,
                    MessageKey("core.error.player_required")
                )
            )
        teleportDisabled(source, player.uuid, preferences, adventure, presentation)
                ?.let { return it }
        val target = PlayerResolver.onlineByName(source.server, targetName)
            ?: return source.replyError(
                adventure,
                messages.renderFor(
                    player.uuid,
                    MessageKey("movement.teleport.player_not_online"),
                    targetName
                ),
            )
        val playerId = player.uuid
        val targetId = target.uuid
        val resolvedName = target.name.string
        return movementLaunch(
            source,
            kernel,
            messages,
            adventure,
            MessageKey("movement.pending.updating_auto"),
        ) { feedback ->
            val notice = when {
                add ->
                    MovementMessages.tpAutoAllow(
                        resolvedName,
                        tpAuto.allow(playerId, targetId),
                    )

                else ->
                    MovementMessages.tpAutoDisallow(
                        resolvedName,
                        tpAuto.disallow(playerId, targetId),
                    )
            }
            kernel.sendNotices(
                feedback,
                adventure,
                presentation,
                playerId,
                listOf(notice),
            )
        }
    }

    /** Rejects a `/tpauto` change while the caller has teleport requests disabled. */
    private fun teleportDisabled(
        source: CommandSourceStack,
        playerId: UUID,
        preferences: PlayerPreferenceService,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int? =
        when {
            preferences.isEnabled(
                playerId,
                PlayerPreference.TELEPORT_ENABLED
            ) -> null

            else -> source.replyNotice(
                adventure,
                presentation,
                playerId,
                MovementMessages.tpAutoTeleportDisabled(),
            )
        }

}
