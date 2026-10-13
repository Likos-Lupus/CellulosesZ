package top.likoslupus.cellulosesz.player.command

import net.minecraft.commands.CommandSourceStack
import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.command.dsl.command
import top.likoslupus.cellulosesz.core.command.dsl.greedyString
import top.likoslupus.cellulosesz.core.command.dsl.word
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.i18n.LocalizedMessages
import top.likoslupus.cellulosesz.core.text.presentation.TextPresentation
import top.likoslupus.cellulosesz.core.text.presentation.sendNotices
import top.likoslupus.cellulosesz.player.format.PlayerMessages
import top.likoslupus.cellulosesz.player.nickname.NicknameLookup
import top.likoslupus.cellulosesz.player.nickname.NicknameService
import java.util.*

/**
 * `/nick` (status + `set`/`clear` for self, `setfor`/`clearfor` for staff) and
 * `/realname <nickname>`: durable nicknames and their resolution.
 */
internal object NicknameCommands {

    fun commands(
        nicknames: NicknameService,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): List<CommandDefinition> = listOf(
        command(
            name = "nick",
            category = CommandCategory.PLAYER,
            permission = CommandPermissions.NICK,
            documentation = "player/nick",
        ) {
            executesPlayer {
                status(
                    source,
                    playerId,
                    nicknames,
                    kernel,
                    messages,
                    adventure,
                    presentation
                )
            }
            literal("set") {
                argument("nickname", greedyString()) { name ->
                    executesPlayer {
                        setOwn(
                            source,
                            playerId,
                            get(name),
                            nicknames,
                            kernel,
                            messages,
                            adventure,
                            presentation
                        )
                    }
                }
            }
            literal("clear") {
                executesPlayer {
                    clearOwn(
                        source,
                        playerId,
                        nicknames,
                        kernel,
                        messages,
                        adventure,
                        presentation
                    )
                }
            }
            literal("setfor") {
                argument(
                    "player",
                    word(),
                    permission = CommandPermissions.NICK_OTHERS,
                ) { player ->
                    suggests { suggestAll(onlineNames(source)) }
                    argument("nickname", greedyString()) { name ->
                        executes {
                            setFor(
                                source,
                                get(player),
                                get(name),
                                nicknames,
                                kernel,
                                messages,
                                adventure,
                                presentation
                            )
                        }
                    }
                }
            }
            literal("clearfor") {
                argument(
                    "player",
                    word(),
                    permission = CommandPermissions.NICK_OTHERS,
                ) { player ->
                    suggests { suggestAll(onlineNames(source)) }
                    executes {
                        clearFor(
                            source,
                            get(player),
                            nicknames,
                            kernel,
                            messages,
                            adventure,
                            presentation
                        )
                    }
                }
            }
        },

        command(
            name = "realname",
            category = CommandCategory.PLAYER,
            permission = CommandPermissions.REALNAME,
            documentation = "player/realname",
        ) {
            argument("nickname", greedyString()) { key ->
                executes {
                    resolve(
                        source,
                        get(key),
                        nicknames,
                        kernel,
                        messages,
                        adventure,
                        presentation,
                    )
                }
            }
        },
    )

    private fun status(
        source: CommandSourceStack,
        playerId: UUID,
        nicknames: NicknameService,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int =
        playerLaunch(source, kernel, messages, adventure) { target ->
            val current = nicknames.current(playerId)
            kernel.sendNotices(
                target,
                adventure,
                presentation,
                playerId,
                listOf(
                    PlayerMessages.nicknameSelfStatus(current),
                    PlayerMessages.nicknameUsage(),
                ),
            )
        }

    private fun setOwn(
        source: CommandSourceStack,
        playerId: UUID,
        raw: String,
        nicknames: NicknameService,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int =
        playerLaunch(source, kernel, messages, adventure) { target ->
            kernel.sendNotices(
                target,
                adventure,
                presentation,
                playerId,
                listOf(PlayerMessages.nicknameFeedback(nicknames.set(playerId, raw)))
            )
        }

    private fun clearOwn(
        source: CommandSourceStack,
        playerId: UUID,
        nicknames: NicknameService,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int =
        playerLaunch(source, kernel, messages, adventure) { target ->
            kernel.sendNotices(
                target,
                adventure,
                presentation,
                playerId,
                listOf(PlayerMessages.nicknameFeedback(nicknames.clear(playerId)))
            )
        }

    private fun setFor(
        source: CommandSourceStack,
        targetName: String,
        raw: String,
        nicknames: NicknameService,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val actorId = source.player?.uuid
        return playerLaunch(source, kernel, messages, adventure) { target ->
            val result = nicknames.setFor(actorId, targetName, raw)
            kernel.sendNotices(
                target,
                adventure,
                presentation,
                actorId,
                listOf(PlayerMessages.nicknameFor(targetName, result))
            )
        }
    }

    private fun clearFor(
        source: CommandSourceStack,
        targetName: String,
        nicknames: NicknameService,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val actorId = source.player?.uuid
        return playerLaunch(source, kernel, messages, adventure) { target ->
            val result = nicknames.clearFor(targetName)
            kernel.sendNotices(
                target,
                adventure,
                presentation,
                actorId,
                listOf(PlayerMessages.nicknameFor(targetName, result))
            )
        }
    }

    private fun resolve(
        source: CommandSourceStack,
        raw: String,
        nicknames: NicknameService,
        kernel: RuntimeKernel,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val requesterId = source.player?.uuid
        return playerLaunch(
            source,
            kernel,
            messages,
            adventure
        ) { target ->
            val notice = when (val result = nicknames.resolve(raw)) {
                is NicknameLookup.Found ->
                    PlayerMessages.realnameFound(raw, result.actualName)

                NicknameLookup.NotFound ->
                    PlayerMessages.realnameNotFound(raw)

                NicknameLookup.StorageUnavailable ->
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

}
