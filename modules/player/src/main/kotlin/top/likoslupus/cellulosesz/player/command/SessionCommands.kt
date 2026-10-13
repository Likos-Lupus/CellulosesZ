package top.likoslupus.cellulosesz.player.command

import kotlinx.coroutines.CancellationException
import net.minecraft.commands.CommandSourceStack
import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.command.dsl.command
import top.likoslupus.cellulosesz.core.command.dsl.word
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.i18n.LocalizedMessages
import top.likoslupus.cellulosesz.core.text.presentation.TextPresentation
import top.likoslupus.cellulosesz.core.text.presentation.sendDetail
import top.likoslupus.cellulosesz.core.text.presentation.sendNotices
import top.likoslupus.cellulosesz.player.format.PlayerMessages
import top.likoslupus.cellulosesz.player.presence.PlayerPresenceService
import top.likoslupus.cellulosesz.player.presence.PresenceLookup
import java.time.Clock
import java.util.*

/** `/seen`, `/playtime` and `/whois`: durable presence and playtime queries. */
internal object SessionCommands {

    fun commands(
        presence: PlayerPresenceService,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
        clock: Clock,
    ): List<CommandDefinition> = listOf(
        command(
            name = "seen",
            category = CommandCategory.PLAYER,
            permission = CommandPermissions.SEEN,
            documentation = "player/seen",
        ) {
            argument("player", word()) { key ->
                suggests { suggestAll(onlineNames(source)) }
                executes {
                    seen(
                        source = source,
                        raw = get(key),
                        presence = presence,
                        kernel = kernel,
                        messages = messages,
                        adventure = adventure,
                        presentation = presentation,
                        clock = clock,
                    )
                }
            }
        },

        command(
            name = "playtime",
            category = CommandCategory.PLAYER,
            permission = CommandPermissions.PLAY_TIME,
            documentation = "player/playtime",
        ) {
            executesPlayer {
                playtimeSelf(
                    source = source,
                    playerId = playerId,
                    presence = presence,
                    kernel = kernel,
                    messages = messages,
                    adventure = adventure,
                    presentation = presentation,
                )
            }
            argument(
                "player",
                word(),
                permission = CommandPermissions.PLAY_TIME_OTHERS,
            ) { key ->
                suggests { suggestAll(onlineNames(source)) }
                executesPlayer {
                    playtimeOther(
                        source = source,
                        requesterId = playerId,
                        raw = get(key),
                        presence = presence,
                        kernel = kernel,
                        messages = messages,
                        adventure = adventure,
                        presentation = presentation,
                    )
                }
            }
        },

        command(
            name = "whois",
            category = CommandCategory.PLAYER,
            permission = CommandPermissions.WHOIS,
            documentation = "player/whois",
        ) {
            argument("player", word()) { key ->
                suggests { suggestAll(onlineNames(source)) }
                executes {
                    whois(
                        source = source,
                        raw = get(key),
                        presence = presence,
                        kernel = kernel,
                        messages = messages,
                        adventure = adventure,
                        presentation = presentation,
                        clock = clock,
                    )
                }
            }
        },
    )

    private fun seen(
        source: CommandSourceStack,
        raw: String,
        presence: PlayerPresenceService,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
        clock: Clock,
    ): Int {
        val requesterId = source.player?.uuid
        return playerLaunch(source, kernel, messages, adventure) { target ->
            val notice = when (val result = presence.lookup(raw)) {
                is PresenceLookup.Found ->
                    PlayerMessages.seen(result.presence, clock.instant())

                PresenceLookup.NotFound ->
                    PlayerMessages.notFound(raw)

                PresenceLookup.StorageUnavailable ->
                    PlayerMessages.storageUnavailable()
            }
            kernel.sendNotices(
                target,
                adventure,
                presentation,
                requesterId,
                listOf(notice)
            )
        }
    }

    private fun playtimeSelf(
        source: CommandSourceStack,
        playerId: UUID,
        presence: PlayerPresenceService,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int =
        playerLaunch(source, kernel, messages, adventure) { target ->
            val duration = try {
                presence.playtime(playerId)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                kernel.sendNotices(
                    target,
                    adventure,
                    presentation,
                    playerId,
                    listOf(PlayerMessages.storageUnavailable())
                )
                return@playerLaunch
            }
            kernel.sendNotices(
                target,
                adventure,
                presentation,
                playerId,
                listOf(PlayerMessages.playtimeSelf(duration))
            )
        }

    private fun playtimeOther(
        source: CommandSourceStack,
        requesterId: UUID,
        raw: String,
        presence: PlayerPresenceService,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int =
        playerLaunch(source, kernel, messages, adventure) { target ->
            val notice = when (val result = presence.lookup(raw)) {
                is PresenceLookup.Found ->
                    PlayerMessages.playtimeOther(result.presence.name, result.presence.playtime)

                PresenceLookup.NotFound ->
                    PlayerMessages.notFound(raw)

                PresenceLookup.StorageUnavailable ->
                    PlayerMessages.storageUnavailable()
            }
            kernel.sendNotices(
                target,
                adventure,
                presentation,
                requesterId,
                listOf(notice)
            )
        }

    private fun whois(
        source: CommandSourceStack,
        raw: String,
        presence: PlayerPresenceService,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
        clock: Clock,
    ): Int {
        val requesterId = source.player?.uuid
        return playerLaunch(
            source,
            kernel,
            messages,
            adventure
        ) { target ->
            when (val result = presence.lookup(raw)) {
                is PresenceLookup.Found -> kernel.sendDetail(
                    target,
                    adventure,
                    presentation,
                    requesterId,
                    PlayerMessages.whois(result.presence, clock.instant())
                )

                PresenceLookup.NotFound -> kernel.sendNotices(
                    target,
                    adventure,
                    presentation,
                    requesterId,
                    listOf(PlayerMessages.notFound(raw))
                )

                PresenceLookup.StorageUnavailable -> kernel.sendNotices(
                    target,
                    adventure,
                    presentation,
                    requesterId,
                    listOf(PlayerMessages.storageUnavailable())
                )
            }
        }
    }

}
