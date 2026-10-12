package top.likoslupus.cellulosesz.administration.playerstate

import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Abilities
import top.likoslupus.cellulosesz.administration.format.AdministrationMessages
import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.command.dsl.command
import top.likoslupus.cellulosesz.core.command.dsl.word
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.player.PlayerResolver
import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.adventure.replyError
import top.likoslupus.cellulosesz.core.text.i18n.LocalizedMessages
import top.likoslupus.cellulosesz.core.text.presentation.TextPresentation
import top.likoslupus.cellulosesz.core.text.presentation.replyNotice

/**
 * Operator control over individual player state (health, food, flight, invulnerability). This is not
 * moderation: nothing here is a sanction or is persisted.
 */
internal object PlayerStateCommands {

    fun commands(
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
            executes {
                heal(
                    context,
                    null,
                    messages,
                    adventure,
                    presentation
                )
            }
            argument("player", word()) { player ->
                executes {
                    heal(
                        context,
                        get(player),
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
            executes {
                feed(
                    context,
                    null,
                    messages,
                    adventure,
                    presentation
                )
            }
            argument("player", word()) { player ->
                executes {
                    feed(
                        context,
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
            executes {
                fly(
                    context,
                    null,
                    messages,
                    adventure,
                    presentation
                )
            }
            argument("player", word()) { player ->
                executes {
                    fly(
                        context,
                        get(player),
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
            executes {
                god(
                    context,
                    null,
                    messages,
                    adventure,
                    presentation
                )
            }
            argument("player", word()) { player ->
                executes {
                    god(
                        context,
                        get(player),
                        messages,
                        adventure,
                        presentation
                    )
                }
            }
        },
    )

    private fun heal(
        context: CommandContext<CommandSourceStack>,
        targetName: String?,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val source = context.source
        val playerId = source.player?.uuid
        val target = resolveTarget(source, targetName)
            ?: return source.replyError(
                adventure,
                messages.renderFor(
                    playerId,
                    MessageKey("administration.playerstate.player_not_found")
                ),
            )

        target.health = target.maxHealth
        return source.replyNotice(
            adventure,
            presentation,
            playerId,
            AdministrationMessages.healed(target.name.string)
        )
    }

    private fun feed(
        context: CommandContext<CommandSourceStack>,
        targetName: String?,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val source = context.source
        val playerId = source.player?.uuid
        val target = resolveTarget(source, targetName)
            ?: return source.replyError(
                adventure,
                messages.renderFor(
                    playerId,
                    MessageKey("administration.playerstate.player_not_found")
                ),
            )

        target.foodData.foodLevel = 20
        target.foodData.setSaturation(20f)
        return source.replyNotice(
            adventure,
            presentation,
            playerId,
            AdministrationMessages.fed(target.name.string)
        )
    }

    private fun fly(
        context: CommandContext<CommandSourceStack>,
        targetName: String?,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val source = context.source
        val playerId = source.player?.uuid
        val target = resolveTarget(source, targetName)
            ?: return source.replyError(
                adventure,
                messages.renderFor(
                    playerId,
                    MessageKey("administration.playerstate.player_not_found")
                ),
            )

        val current = target.abilities.pack()
        val enable = !current.mayFly()
        target.abilities.apply(
            Abilities.Packed(
                current.invulnerable(),
                enable,
                enable,
                current.instabuild(),
                current.mayBuild(),
                current.flyingSpeed(),
                current.walkingSpeed(),
            )
        )
        target.onUpdateAbilities()
        return source.replyNotice(
            adventure,
            presentation,
            playerId,
            AdministrationMessages.flight(
                target.name.string,
                enable
            ),
        )
    }

    private fun god(
        context: CommandContext<CommandSourceStack>,
        targetName: String?,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val source = context.source
        val playerId = source.player?.uuid
        val target = resolveTarget(source, targetName)
            ?: return source.replyError(
                adventure,
                messages.renderFor(
                    playerId,
                    MessageKey("administration.playerstate.player_not_found")
                ),
            )

        val enable = !target.isInvulnerable
        target.isInvulnerable = enable
        return source.replyNotice(
            adventure,
            presentation,
            playerId,
            AdministrationMessages.god(
                target.name.string,
                enable
            ),
        )
    }

    private fun resolveTarget(
        source: CommandSourceStack,
        targetName: String?
    ): ServerPlayer? =
        if (targetName == null) {
            source.player
        } else {
            PlayerResolver.onlineByName(
                source.server,
                targetName
            )
        }

}
