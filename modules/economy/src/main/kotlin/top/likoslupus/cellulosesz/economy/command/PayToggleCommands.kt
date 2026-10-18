package top.likoslupus.cellulosesz.economy.command

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
import top.likoslupus.cellulosesz.core.text.presentation.TextPresentation
import top.likoslupus.cellulosesz.core.text.presentation.replyNotice
import top.likoslupus.cellulosesz.core.text.presentation.sendNotices
import top.likoslupus.cellulosesz.economy.format.EconomyMessages
import top.likoslupus.cellulosesz.economy.payment.PayConfirmationState
import java.util.*

/**
 * The two payment preferences: `/paytoggle` (whether a player accepts transfers) and
 * `/payconfirmtoggle` (whether a transfer has to be repeated before it runs).
 */
internal object PayToggleCommands {

    fun commands(
        preferences: PlayerPreferenceService,
        confirmations: PayConfirmationState,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): List<CommandDefinition> = listOf(
        payToggleDefinition(
            preferences,
            kernel,
            messages,
            adventure,
            presentation,
        ),
        payConfirmToggleDefinition(
            preferences,
            confirmations,
            kernel,
            messages,
            adventure,
            presentation,
        ),
    )

    private fun payToggleDefinition(
        preferences: PlayerPreferenceService,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): CommandDefinition =
        command(
            name = "paytoggle",
            category = CommandCategory.ECONOMY,
            permission = CommandPermissions.PAY_TOGGLE,
            documentation = "economy/paytoggle",
        ) {
            executesPlayer {
                payToggleStatus(
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
                    applyPayToggle(
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
                    applyPayToggle(
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
                permission = CommandPermissions.PAY_TOGGLE_OTHERS,
            ) { player ->
                suggests { suggestAll(onlineNames(source)) }
                executes {
                    payToggleStatus(
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
                        applyPayToggle(
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
                        applyPayToggle(
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
        }

    private fun payConfirmToggleDefinition(
        preferences: PlayerPreferenceService,
        confirmations: PayConfirmationState,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): CommandDefinition =
        command(
            name = "payconfirmtoggle",
            category = CommandCategory.ECONOMY,
            permission = CommandPermissions.PAY_CONFIRM_TOGGLE,
            documentation = "economy/payconfirmtoggle",
        ) {
            executesPlayer {
                context.source.replyNotice(
                    adventure,
                    presentation,
                    playerId,
                    EconomyMessages.payConfirmStatus(
                        preferences.isEnabled(
                            playerId,
                            PlayerPreference.PAY_CONFIRMATION
                        )
                    ),
                )
            }
            literal("on") {
                executesPlayer {
                    applyPayConfirmToggle(
                        source,
                        playerId,
                        true,
                        preferences,
                        confirmations,
                        kernel,
                        messages,
                        adventure,
                        presentation,
                    )
                }
            }
            literal("off") {
                executesPlayer {
                    applyPayConfirmToggle(
                        source,
                        playerId,
                        false,
                        preferences,
                        confirmations,
                        kernel,
                        messages,
                        adventure,
                        presentation,
                    )
                }
            }
        }

    private fun payToggleStatus(
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
            MessageKey("economy.pay.error.unknown_player"),
        ) ?: return 0
        return source.replyNotice(
            adventure,
            presentation,
            actorId,
            EconomyMessages.payToggleStatus(
                isOther = targetName != null,
                name = target.name,
                enabled = preferences.isEnabled(
                    target.id,
                    PlayerPreference.ACCEPTING_PAY
                ),
            ),
        )
    }

    private fun applyPayToggle(
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
            MessageKey("economy.pay.error.unknown_player"),
        )
            ?: return 0
        val targetId = target.id
        return economyLaunch(
            source,
            kernel,
            messages,
            adventure
        ) { feedback ->
            val notice = when (
                preferences.set(
                    targetId,
                    PlayerPreference.ACCEPTING_PAY,
                    enabled
                )
            ) {
                PreferenceWriteResult.StorageUnavailable ->
                    EconomyMessages.preferenceStorageUnavailable()

                else -> EconomyMessages.payToggleUpdated(
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

    private fun applyPayConfirmToggle(
        source: CommandSourceStack,
        playerId: UUID,
        enabled: Boolean,
        preferences: PlayerPreferenceService,
        confirmations: PayConfirmationState,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int =
        economyLaunch(
            source,
            kernel,
            messages,
            adventure
        ) { feedback ->
            val result = preferences.set(
                playerId,
                PlayerPreference.PAY_CONFIRMATION,
                enabled,
            )
            confirmations.clear(playerId)
            val notice = when (result) {
                PreferenceWriteResult.StorageUnavailable ->
                    EconomyMessages.preferenceStorageUnavailable()

                else -> EconomyMessages.payConfirmUpdated(enabled)
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
