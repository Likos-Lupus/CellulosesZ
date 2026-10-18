package top.likoslupus.cellulosesz.administration.amusement

import net.minecraft.commands.CommandSourceStack
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.AgeableMob
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.item.PrimedTnt
import net.minecraft.world.phys.Vec3
import top.likoslupus.cellulosesz.administration.config.FunSettings
import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.command.dsl.command
import top.likoslupus.cellulosesz.core.command.dsl.greedyString
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.model.LocalizedText
import top.likoslupus.cellulosesz.core.text.model.Notice
import top.likoslupus.cellulosesz.core.text.model.NoticeTone
import top.likoslupus.cellulosesz.core.text.presentation.TextPresentation
import top.likoslupus.cellulosesz.core.text.presentation.replyNotice

/** `/antioch`, `/beezooka` and `/kittycannon`, gated by `[administration.fun].enabled`. */
internal object FunCommands {

    private const val DEFAULT_ANTIOCH_NAME: String = "ANTIOCH"
    private const val ANTIOCH_FUSE_TICKS: Int = 80
    private const val PROJECTILE_SPEED: Double = 1.6

    fun commands(
        settings: () -> FunSettings,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): List<CommandDefinition> = listOf(
        command(
            name = "antioch",
            category = CommandCategory.ADMINISTRATION,
            permission = CommandPermissions.ANTIOCH,
            documentation = "administration/antioch",
        ) {
            executesPlayer {
                if (settings().enabled) {
                    detonate(
                        source,
                        DEFAULT_ANTIOCH_NAME,
                        adventure,
                        presentation
                    )
                } else {
                    reply(
                        source,
                        adventure,
                        presentation,
                        disabled()
                    )
                }
            }
            argument("message", greedyString()) { message ->
                executesPlayer {
                    if (settings().enabled) {
                        detonate(
                            source,
                            get(message),
                            adventure,
                            presentation
                        )
                    } else {
                        reply(
                            source,
                            adventure,
                            presentation,
                            disabled()
                        )
                    }
                }
            }
        },
        command(
            name = "beezooka",
            category = CommandCategory.ADMINISTRATION,
            permission = CommandPermissions.BEEZOOKA,
            documentation = "administration/beezooka",
        ) {
            executesPlayer {
                if (settings().enabled) {
                    launch(source, EntityType.BEE, false)
                    reply(
                        source,
                        adventure,
                        presentation,
                        success(MessageKey("administration.fun.beezooka"))
                    )
                } else {
                    reply(
                        source,
                        adventure,
                        presentation,
                        disabled()
                    )
                }
            }
        },
        command(
            name = "kittycannon",
            category = CommandCategory.ADMINISTRATION,
            permission = CommandPermissions.KITTY_CANNON,
            documentation = "administration/kittycannon",
        ) {
            executesPlayer {
                if (settings().enabled) {
                    launch(source, EntityType.CAT, true)
                    reply(
                        source,
                        adventure,
                        presentation,
                        success(MessageKey("administration.fun.kittycannon"))
                    )
                } else {
                    reply(
                        source,
                        adventure,
                        presentation,
                        disabled()
                    )
                }
            }
        },
    )

    private fun detonate(
        source: CommandSourceStack,
        message: String,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val player = source.player
            ?: return reply(
                source,
                adventure,
                presentation,
                error(MessageKey("administration.playerstate.player_not_found")),
            )
        val level = player.level()
        val tnt = PrimedTnt(
            level,
            player.x,
            player.y + 1.0,
            player.z,
            player
        )
        tnt.fuse = ANTIOCH_FUSE_TICKS
        tnt.customName = Component.literal(message)
        tnt.isCustomNameVisible = true
        level.addFreshEntity(tnt)
        return reply(
            source,
            adventure,
            presentation,
            success(MessageKey("administration.fun.antioch"))
        )
    }

    private fun launch(
        source: CommandSourceStack,
        type: EntityType<*>,
        baby: Boolean
    ) {
        val player: ServerPlayer = source.player ?: return
        val level = player.level()
        val entity: Entity = type.create(level, EntitySpawnReason.COMMAND) ?: return
        entity.snapTo(
            player.x,
            player.eyeY,
            player.z,
            player.yRot,
            player.xRot
        )
        val direction: Vec3 = player.lookAngle.normalize().scale(PROJECTILE_SPEED)
        entity.deltaMovement = direction
        if (baby && entity is AgeableMob) {
            entity.isBaby = true
        }
        level.addFreshEntity(entity)
    }

    private fun reply(
        source: CommandSourceStack,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
        notice: Notice,
    ): Int =
        source.replyNotice(
            adventure,
            presentation,
            source.player?.uuid,
            notice
        )

    private fun success(key: MessageKey): Notice =
        Notice(
            LocalizedText(key),
            NoticeTone.SUCCESS
        )

    private fun error(key: MessageKey): Notice =
        Notice(
            LocalizedText(key),
            NoticeTone.ERROR
        )

    private fun disabled(): Notice =
        error(MessageKey("administration.fun.disabled"))

}
