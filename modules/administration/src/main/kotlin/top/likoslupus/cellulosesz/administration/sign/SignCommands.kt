package top.likoslupus.cellulosesz.administration.sign

import net.minecraft.commands.CommandSourceStack
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.block.entity.SignBlockEntity
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import top.likoslupus.cellulosesz.administration.config.SignEditingSettings
import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.command.dsl.command
import top.likoslupus.cellulosesz.core.command.dsl.integer
import top.likoslupus.cellulosesz.core.command.dsl.message
import top.likoslupus.cellulosesz.core.command.requiresPermission
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.permission.PermissionService
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.model.Notice
import top.likoslupus.cellulosesz.core.text.presentation.TextPresentation
import top.likoslupus.cellulosesz.core.text.presentation.replyNotice

/** `/editsign <set|clear|copy|paste> [line] [text]`, front-only and text-only by default. */
internal object SignCommands {

    private const val REACH: Double = 8.0

    fun commands(
        service: SignEditService,
        permissions: PermissionService,
        settings: () -> SignEditingSettings,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): List<CommandDefinition> = listOf(
        command(
            name = "editsign",
            category = CommandCategory.ADMINISTRATION,
            permission = CommandPermissions.EDIT_SIGN,
            documentation = "administration/editsign",
        ) {
            literal("set") {
                argument("line", integer(1, 4)) { line ->
                    argument("text", message()) { text ->
                        executesPlayer {
                            withSign(
                                source,
                                service,
                                settings,
                                adventure,
                                presentation,
                            ) { sign, back, config, player ->
                                val raw = get(text).string
                                if (raw.length > config.maxLineLength) {
                                    return@withSign reply(
                                        source,
                                        adventure,
                                        presentation,
                                        SignMessages.tooLong(),
                                    )
                                }
                                val allowColors = colorsAllowed(
                                    player,
                                    permissions,
                                    config,
                                )
                                service.setLine(
                                    sign,
                                    back,
                                    get(line) - 1,
                                    raw,
                                    allowColors
                                )
                                reply(
                                    source,
                                    adventure,
                                    presentation,
                                    SignMessages.setLine(get(line)),
                                )
                            }
                        }
                    }
                }
            }
            literal("clear") {
                executesPlayer {
                    withSign(
                        source,
                        service,
                        settings,
                        adventure,
                        presentation,
                    ) { sign, back, _, _ ->
                        service.clearAll(sign, back)
                        reply(
                            source,
                            adventure,
                            presentation,
                            SignMessages.clearedAll()
                        )
                    }
                }
                argument("line", integer(1, 4)) { line ->
                    executesPlayer {
                        withSign(
                            source,
                            service,
                            settings,
                            adventure,
                            presentation,
                        ) { sign, back, _, _ ->
                            service.clearLine(sign, back, get(line) - 1)
                            reply(
                                source,
                                adventure,
                                presentation,
                                SignMessages.clearedLine(get(line)),
                            )
                        }
                    }
                }
            }
            literal("copy") {
                executesPlayer {
                    withSign(
                        source,
                        service,
                        settings,
                        adventure,
                        presentation,
                    ) { sign, back, _, player ->
                        service.copy(
                            player.uuid,
                            service.lines(sign, back)
                        )
                        reply(
                            source,
                            adventure,
                            presentation,
                            SignMessages.copied()
                        )
                    }
                }
            }
            literal("paste") {
                executesPlayer {
                    withSign(
                        source,
                        service,
                        settings,
                        adventure,
                        presentation,
                    ) { sign, back, config, player ->
                        val lines = service.clipboard(player.uuid)
                            ?: return@withSign reply(
                                source,
                                adventure,
                                presentation,
                                SignMessages.noClipboard(),
                            )
                        val allowColors = colorsAllowed(player, permissions, config)
                        service.paste(
                            sign,
                            back,
                            lines,
                            allowColors
                        )
                        reply(
                            source,
                            adventure,
                            presentation,
                            SignMessages.pasted()
                        )
                    }
                }
            }
        },
    )

    private inline fun withSign(
        source: CommandSourceStack,
        service: SignEditService,
        settings: () -> SignEditingSettings,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
        block: (SignBlockEntity, Boolean, SignEditingSettings, ServerPlayer) -> Int,
    ): Int {
        val sourcePlayer = source.player
            ?: return reply(
                source,
                adventure,
                presentation,
                SignMessages.notSign()
            )
        val config = settings()
        if (!config.enabled) {
            return reply(
                source,
                adventure,
                presentation,
                SignMessages.disabled()
            )
        }
        val hit: HitResult = sourcePlayer.pick(REACH, 0f, false)
        val pos: BlockPos = (hit as? BlockHitResult)?.blockPos
            ?: return reply(
                source,
                adventure,
                presentation,
                SignMessages.notSign()
            )
        val sign = service.signAt(sourcePlayer.level(), pos)
            ?: return reply(
                source,
                adventure,
                presentation,
                SignMessages.notSign()
            )
        val center = Vec3(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5)
        if (sourcePlayer.distanceToSqr(center) > config.maxDistance * config.maxDistance) {
            return reply(
                source,
                adventure,
                presentation,
                SignMessages.tooFar()
            )
        }
        val front = sign.isFacingFrontText(sourcePlayer)
        val back = !front
        if (back && !config.allowBack) {
            return reply(
                source,
                adventure,
                presentation,
                SignMessages.backDisabled()
            )
        }
        return block(sign, back, config, sourcePlayer)
    }

    private fun colorsAllowed(
        player: ServerPlayer,
        permissions: PermissionService,
        config: SignEditingSettings,
    ): Boolean =
        config.allowColors &&
                player.createCommandSourceStack().requiresPermission(
                    permissions,
                    CommandPermissions.EDIT_SIGN_COLORS,
                )

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

}
