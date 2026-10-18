package top.likoslupus.cellulosesz.administration.amusement

import net.minecraft.commands.CommandSourceStack
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.AgeableMob
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.item.PrimedTnt
import net.minecraft.world.level.Level
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import top.likoslupus.cellulosesz.administration.config.FunSettings
import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.command.CommandFeedbackTarget
import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.command.dsl.command
import top.likoslupus.cellulosesz.core.command.dsl.greedyString
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.model.LocalizedText
import top.likoslupus.cellulosesz.core.text.model.Notice
import top.likoslupus.cellulosesz.core.text.model.NoticeTone
import top.likoslupus.cellulosesz.core.text.presentation.TextPresentation
import top.likoslupus.cellulosesz.core.text.presentation.replyNotice
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay

/** `/antioch`, `/beezooka` and `/kittycannon`, gated by `[administration.fun].enabled`. */
internal object FunCommands {

    private const val PROJECTILE_SPEED: Double = 2.0
    private const val PROJECTILE_LIFETIME_MILLIS: Long = 1_000
    private const val ANTIOCH_REACH: Double = 100.0

    fun commands(
        settings: () -> FunSettings,
        kernel: RuntimeKernel,
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
                        false,
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
            argument("message", greedyString()) {
                executesPlayer {
                    if (settings().enabled) {
                        detonate(
                            source,
                            true,
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
                    launch(source, EntityType.BEE, false, kernel)
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
                    launch(source, EntityType.CAT, true, kernel)
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
        announce: Boolean,
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
        val pos = (player.pick(ANTIOCH_REACH, 0f, false) as? BlockHitResult)?.blockPos
            ?: player.blockPosition()
        val tnt = PrimedTnt(
            level,
            pos.x + 0.5,
            pos.y.toDouble(),
            pos.z + 0.5,
            player
        )
        level.addFreshEntity(tnt)
        if (announce) {
            broadcastAntioch(source, adventure, presentation)
        }
        return reply(
            source,
            adventure,
            presentation,
            success(MessageKey("administration.fun.antioch"))
        )
    }

    private fun broadcastAntioch(
        source: CommandSourceStack,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ) {
        val lines = listOf("line1", "line2").map { suffix ->
            Notice(
                LocalizedText(MessageKey("administration.fun.antioch.$suffix")),
                NoticeTone.INFO
            )
        }
        source.server.playerList.players.forEach { target ->
            lines.forEach { notice ->
                presentation.renderFor(target.uuid, notice).forEach { rendered ->
                    adventure.audience(CommandFeedbackTarget.Player(target.uuid))
                            .sendMessage(rendered)
                }
            }
        }
    }

    private fun launch(
        source: CommandSourceStack,
        type: EntityType<*>,
        baby: Boolean,
        kernel: RuntimeKernel,
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
        if (!level.addFreshEntity(entity)) {
            return
        }
        kernel.launch {
            delay(PROJECTILE_LIFETIME_MILLIS.milliseconds)
            kernel.onServerThread {
                if (!entity.isRemoved) {
                    val location = entity.position()
                    entity.discard()
                    level.explode(
                        null,
                        location.x,
                        location.y,
                        location.z,
                        0.0f,
                        Level.ExplosionInteraction.NONE
                    )
                }
            }
        }
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
