package top.likoslupus.cellulosesz.utility.item

import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.command.CommandFeedbackTarget
import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.command.dsl.command
import top.likoslupus.cellulosesz.core.command.dsl.word
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.player.KnownPlayerResolver
import top.likoslupus.cellulosesz.core.preference.PlayerPreference
import top.likoslupus.cellulosesz.core.preference.PlayerPreferenceService
import top.likoslupus.cellulosesz.core.preference.PreferenceWriteResult
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.i18n.LocalizedMessages
import top.likoslupus.cellulosesz.core.text.model.Notice
import top.likoslupus.cellulosesz.core.text.presentation.TextPresentation
import top.likoslupus.cellulosesz.core.text.presentation.replyNotice
import top.likoslupus.cellulosesz.core.text.presentation.sendNotices
import top.likoslupus.cellulosesz.utility.command.utilityLaunch
import top.likoslupus.cellulosesz.utility.format.*
import java.util.*

internal object ClearInventoryCommands {

    fun commands(
        service: ItemUtilityService,
        known: KnownPlayerResolver,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
        audit: AdministrativeActionAuditPort,
        kernel: RuntimeKernel,
        preferences: PlayerPreferenceService,
    ): List<CommandDefinition> = listOf(
        command(
            name = "clearinventory",
            category = CommandCategory.UTILITY,
            permission = CommandPermissions.CLEAR_INVENTORY,
            documentation = "utility/clearinventory",
        ) {
            executesPlayer {
                clearSelf(
                    source,
                    playerId,
                    ClearScope.MAIN,
                    service,
                    preferences,
                    adventure,
                    presentation,
                )
            }
            literal("all") {
                executesPlayer {
                    clearSelf(
                        source,
                        playerId,
                        ClearScope.ALL,
                        service,
                        preferences,
                        adventure,
                        presentation,
                    )
                }
            }
            literal("confirm") {
                argument("code", word()) { code ->
                    executesPlayer {
                        context.source.replyNotice(
                            adventure,
                            presentation,
                            playerId,
                            UtilityMessages.clearInventory(
                                service.confirmInventoryClear(playerId, get(code))
                            ),
                        )
                    }
                }
            }
            literal("target") {
                argument(
                    "player",
                    word(),
                    permission = CommandPermissions.CLEAR_INVENTORY_OTHERS,
                ) { player ->
                    suggests {
                        source.server.playerList.players.forEach { suggest(it.gameProfile.name) }
                    }
                    literal("main") {
                        executesPlayer {
                            requestOther(
                                source,
                                playerId,
                                get(player),
                                ClearScope.MAIN,
                                service,
                                preferences,
                                adventure,
                                presentation,
                            )
                        }
                    }
                    literal("all") {
                        executesPlayer {
                            requestOther(
                                source,
                                playerId,
                                get(player),
                                ClearScope.ALL,
                                service,
                                preferences,
                                adventure,
                                presentation,
                            )
                        }
                    }
                }
            }
            literal("target-confirm") {
                argument(
                    "player",
                    word(),
                    permission = CommandPermissions.CLEAR_INVENTORY_OTHERS,
                ) { player ->
                    suggests {
                        source.server.playerList.players.forEach { suggest(it.gameProfile.name) }
                    }
                    argument("code", word()) { code ->
                        executesPlayer {
                            confirmOther(
                                source,
                                playerId,
                                get(player),
                                get(code),
                                service,
                                known,
                                adventure,
                                presentation,
                                audit,
                                kernel,
                            )
                        }
                    }
                }
            }
        },

        command(
            name = "clearinventoryconfirmtoggle",
            category = CommandCategory.UTILITY,
            permission = CommandPermissions.CLEAR_INVENTORY_CONFIRM_TOGGLE,
            documentation = "utility/clearinventoryconfirmtoggle",
        ) {
            executesPlayer {
                context.source.replyNotice(
                    adventure,
                    presentation,
                    playerId,
                    UtilityMessages.clearConfirmStatus(
                        confirmationRequired(playerId, preferences)
                    ),
                )
            }
            literal("on") {
                executesPlayer {
                    setConfirmation(
                        context,
                        playerId,
                        true,
                        service,
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
                    setConfirmation(
                        context,
                        playerId,
                        false,
                        service,
                        preferences,
                        kernel,
                        messages,
                        adventure,
                        presentation,
                    )
                }
            }
        },
    )

    private fun clearSelf(
        source: CommandSourceStack,
        playerId: UUID,
        scope: ClearScope,
        service: ItemUtilityService,
        preferences: PlayerPreferenceService,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val notice = if (confirmationRequired(playerId, preferences)) {
            UtilityMessages.clearRequest(service.requestInventoryClear(playerId, scope))
        } else {
            UtilityMessages.clearInventory(service.clearInventoryNow(playerId, scope))
        }
        return source.replyNotice(adventure, presentation, playerId, notice)
    }

    private fun requestOther(
        source: CommandSourceStack,
        actorId: UUID,
        targetName: String,
        scope: ClearScope,
        service: ItemUtilityService,
        preferences: PlayerPreferenceService,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        if (!confirmationRequired(actorId, preferences)) {
            val targetId = runningOnlineId(source, targetName)
            val result = service.clearInventoryForNow(targetName, scope)
            if (result is ClearInventoryResult.Cleared && targetId != null) {
                notify(
                    targetId,
                    UtilityMessages.clearTargetDone(result.stacks),
                    adventure,
                    presentation,
                )
            }
            return source.replyNotice(
                adventure,
                presentation,
                actorId,
                UtilityMessages.clearInventory(result),
            )
        }
        val result = service.requestInventoryClearFor(actorId, targetName, scope)
        if (result is ClearRequestResult.Issued && result.targetId != null) {
            notify(
                result.targetId,
                UtilityMessages.clearTargetNotice(result.scope),
                adventure,
                presentation,
            )
        }
        return source.replyNotice(
            adventure,
            presentation,
            actorId,
            UtilityMessages.clearRequest(result),
        )
    }

    private fun setConfirmation(
        context: CommandContext<CommandSourceStack>,
        playerId: UUID,
        enabled: Boolean,
        service: ItemUtilityService,
        preferences: PlayerPreferenceService,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val source = context.source
        return utilityLaunch(
            source,
            kernel,
            messages,
            adventure,
            MessageKey("utility.pending.updating_settings"),
        ) { target ->
            val result = preferences.set(
                playerId,
                PlayerPreference.CLEAR_INVENTORY_CONFIRMATION,
                enabled,
            )
            service.forgetInventoryClear(playerId)
            val notice = when (result) {
                PreferenceWriteResult.StorageUnavailable ->
                    UtilityMessages.preferenceStorageUnavailable()

                else -> UtilityMessages.clearConfirmUpdated(enabled)
            }
            kernel.sendNotices(
                target,
                adventure,
                presentation,
                playerId,
                listOf(notice),
            )
        }
    }

    private fun confirmationRequired(
        playerId: UUID,
        preferences: PlayerPreferenceService,
    ): Boolean =
        preferences.isEnabled(
            playerId,
            PlayerPreference.CLEAR_INVENTORY_CONFIRMATION
        )

    /** Resolves an online player id for a just-completed clear without blocking. Server thread. */
    private fun runningOnlineId(
        source: CommandSourceStack,
        name: String,
    ): UUID? =
        source.server.playerList.players
                .firstOrNull { it.gameProfile.name.equals(name, ignoreCase = true) }
                ?.uuid

    private fun confirmOther(
        source: CommandSourceStack,
        actorId: UUID,
        targetName: String,
        code: String,
        service: ItemUtilityService,
        known: KnownPlayerResolver,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
        audit: AdministrativeActionAuditPort,
        kernel: RuntimeKernel,
    ): Int {
        val targetId = known.onlineByName(targetName)?.id
        val result = service.confirmInventoryClearFor(actorId, targetName, code)
        if (targetId != null) {
            kernel.launch {
                audit.record(
                    actorId = actorId,
                    targetId = targetId,
                    targetName = targetName,
                    cleared = result is ClearInventoryResult.Cleared,
                )
            }
        }
        if (result is ClearInventoryResult.Cleared && targetId != null) {
            notify(
                targetId,
                UtilityMessages.clearTargetDone(result.stacks),
                adventure,
                presentation,
            )
        }
        return source.replyNotice(
            adventure,
            presentation,
            actorId,
            UtilityMessages.clearInventory(result),
        )
    }

    private fun notify(
        targetId: UUID,
        notice: Notice,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ) {
        presentation.renderFor(targetId, notice).forEach { line ->
            adventure.audience(CommandFeedbackTarget.Player(targetId)).sendMessage(line)
        }
    }

}
