package top.likoslupus.cellulosesz.movement.request

import net.minecraft.commands.CommandSourceStack
import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.command.dsl.command
import top.likoslupus.cellulosesz.core.command.dsl.word
import top.likoslupus.cellulosesz.core.command.onlineNames
import top.likoslupus.cellulosesz.core.command.resolveToggleTarget
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.preference.PlayerPreference
import top.likoslupus.cellulosesz.core.preference.PlayerPreferenceService
import top.likoslupus.cellulosesz.core.preference.PreferenceWriteResult
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.i18n.LocalizedMessages
import top.likoslupus.cellulosesz.core.text.presentation.*
import top.likoslupus.cellulosesz.movement.command.movementLaunch
import top.likoslupus.cellulosesz.movement.format.*
import java.util.*

/** `/tptoggle`: whether the caller (or another player) accepts teleport requests. */
internal object TeleportPreferenceCommands {

    fun commands(
        preferences: PlayerPreferenceService,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): List<CommandDefinition> = listOf(
        command(
            name = "tptoggle",
            category = CommandCategory.MOVEMENT,
            permission = CommandPermissions.TP_TOGGLE,
            documentation = "movement/tptoggle",
        ) {
            executesPlayer {
                toggleStatus(
                    source,
                    playerId,
                    null,
                    preferences,
                    messages,
                    adventure,
                    presentation,
                )
            }
            literal("on") {
                executesPlayer {
                    applyToggle(
                        source,
                        playerId,
                        null,
                        true,
                        preferences,
                        kernel,
                        messages,
                        adventure,
                        presentation,
                    )
                }
            }
            literal("off") {
                executesPlayer {
                    applyToggle(
                        source,
                        playerId,
                        null,
                        false,
                        preferences,
                        kernel,
                        messages,
                        adventure,
                        presentation,
                    )
                }
            }
            argument(
                "player",
                word(),
                permission = CommandPermissions.TP_TOGGLE_OTHERS,
            ) { player ->
                suggests { suggestAll(onlineNames(source)) }
                executes {
                    toggleStatus(
                        source,
                        source.player?.uuid,
                        get(player),
                        preferences,
                        messages,
                        adventure,
                        presentation,
                    )
                }
                literal("on") {
                    executes {
                        applyToggle(
                            source,
                            source.player?.uuid,
                            get(player),
                            true,
                            preferences,
                            kernel,
                            messages,
                            adventure,
                            presentation,
                        )
                    }
                }
                literal("off") {
                    executes {
                        applyToggle(
                            source,
                            source.player?.uuid,
                            get(player),
                            false,
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

    private fun toggleStatus(
        source: CommandSourceStack,
        actorId: UUID?,
        targetName: String?,
        preferences: PlayerPreferenceService,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val target = resolveToggleTarget(
            source,
            actorId,
            targetName,
            messages,
            adventure,
            MessageKey("movement.teleport.player_not_online"),
        ) ?: return 0
        val enabled = preferences.isEnabled(
            target.id,
            PlayerPreference.TELEPORT_ENABLED
        )
        return source.replyNotice(
            adventure,
            presentation,
            actorId,
            MovementMessages.teleportToggleStatus(
                isOther = targetName != null,
                name = target.name,
                enabled = enabled,
            ),
        )
    }

    private fun applyToggle(
        source: CommandSourceStack,
        actorId: UUID?,
        targetName: String?,
        enabled: Boolean,
        preferences: PlayerPreferenceService,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val target = resolveToggleTarget(
            source,
            actorId,
            targetName,
            messages,
            adventure,
            MessageKey("movement.teleport.player_not_online"),
        ) ?: return 0
        val targetId = target.id
        return movementLaunch(
            source,
            kernel,
            messages,
            adventure,
            MessageKey("movement.pending.updating_teleport"),
        ) { feedback ->
            val notice = when (
                preferences.set(
                    targetId,
                    PlayerPreference.TELEPORT_ENABLED,
                    enabled,
                )
            ) {
                PreferenceWriteResult.StorageUnavailable ->
                    MovementMessages.preferenceStorageUnavailable()

                else -> MovementMessages.teleportToggleUpdated(
                    isOther = targetName != null,
                    name = target.name,
                    enabled = enabled,
                )
            }
            kernel.sendNotices(
                feedback,
                adventure,
                presentation,
                actorId,
                listOf(notice),
            )
        }
    }

}
