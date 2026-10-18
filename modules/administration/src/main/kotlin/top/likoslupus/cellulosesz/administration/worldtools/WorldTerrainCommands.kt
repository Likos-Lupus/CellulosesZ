package top.likoslupus.cellulosesz.administration.worldtools

import net.minecraft.commands.CommandSourceStack
import net.minecraft.core.BlockPos
import net.minecraft.data.worldgen.features.TreeFeatures
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import top.likoslupus.cellulosesz.administration.config.WorldToolsSettings
import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.command.dsl.command
import top.likoslupus.cellulosesz.core.command.dsl.double
import top.likoslupus.cellulosesz.core.command.dsl.word
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.presentation.TextPresentation

/** `/tree`, `/bigtree`, `/break`, `/ice`, `/ext` and `/burn`. */
internal object WorldTerrainCommands {

    private const val REACH: Double = 8.0

    private val TREE_SUGGESTIONS: List<String> = listOf(
        "tree",
        "birch",
        "redwood",
        "redmushroom",
        "brownmushroom",
        "jungle",
        "junglebush",
        "swamp",
    )

    private val BIG_TREE_SUGGESTIONS: List<String> = listOf(
        "tree",
        "redwood",
        "jungle",
        "darkoak",
    )

    fun commands(
        service: WorldToolsService,
        settings: () -> WorldToolsSettings,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): List<CommandDefinition> = listOf(
        command(
            name = "tree",
            category = CommandCategory.ADMINISTRATION,
            permission = CommandPermissions.TREE,
            documentation = "administration/tree",
        ) {
            argument("type", word()) { type ->
                suggests { suggestAll(TREE_SUGGESTIONS) }
                executesPlayer {
                    applyTree(
                        source,
                        get(type),
                        false,
                        service,
                        adventure,
                        presentation
                    )
                }
            }
        },
        command(
            name = "bigtree",
            category = CommandCategory.ADMINISTRATION,
            permission = CommandPermissions.BIG_TREE,
            documentation = "administration/bigtree",
        ) {
            argument("type", word()) { type ->
                suggests { suggestAll(BIG_TREE_SUGGESTIONS) }
                executesPlayer {
                    applyTree(
                        source,
                        get(type),
                        true,
                        service,
                        adventure,
                        presentation
                    )
                }
            }
        },
        command(
            name = "break",
            category = CommandCategory.ADMINISTRATION,
            permission = CommandPermissions.BREAK,
            documentation = "administration/break",
        ) {
            executesPlayer {
                val pos = lookedAtBlock(source)
                    ?: return@executesPlayer reply(
                        source,
                        adventure,
                        presentation,
                        WorldToolsMessages.lookAtBlock(),
                    )
                val player = source.player!!
                service.breakBlock(player.level(), pos, player)
                reply(
                    source,
                    adventure,
                    presentation,
                    WorldToolsMessages.brokeBlock()
                )
            }
        },
        command(
            name = "ice",
            category = CommandCategory.ADMINISTRATION,
            permission = CommandPermissions.ICE,
            documentation = "administration/ice",
        ) {
            executesPlayer {
                val player = source.player!!
                val frozen = service.freezeWater(
                    player.level(),
                    player.blockPosition(),
                    2
                )
                reply(
                    source,
                    adventure,
                    presentation,
                    WorldToolsMessages.iceFrozen(frozen)
                )
            }
        },
        command(
            name = "ext",
            category = CommandCategory.ADMINISTRATION,
            permission = CommandPermissions.EXT,
            documentation = "administration/ext",
        ) {
            executesPlayer {
                val player = source.player!!
                service.extinguish(player, 3)
                reply(
                    source,
                    adventure,
                    presentation,
                    WorldToolsMessages.extinguished()
                )
            }
        },
        burnCommand(service, settings, adventure, presentation),
    )

    // ---------------------------------------------------------------- helpers

    private fun lookedAtBlock(source: CommandSourceStack): BlockPos? {
        val player = source.player
            ?: return null
        return blockPos(player.pick(REACH, 0f, false))
    }

    private fun blockPos(hit: HitResult?): BlockPos? =
        (hit as? BlockHitResult)?.blockPos

    // ---------------------------------------------------------------- /tree

    private fun applyTree(
        source: CommandSourceStack,
        rawType: String,
        big: Boolean,
        service: WorldToolsService,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val key = treeFeature(rawType, big)
            ?: return reply(
                source,
                adventure,
                presentation,
                WorldToolsMessages.treeInvalid()
            )
        val pos = lookedAtBlock(source)
            ?: return reply(
                source,
                adventure,
                presentation,
                WorldToolsMessages.lookAtBlock()
            )
        val player = source.player
            ?: return reply(
                source,
                adventure,
                presentation,
                WorldToolsMessages.playerNotFound()
            )
        return when {
            service.growTree(player.level(), pos, key) ->
                reply(
                    source,
                    adventure,
                    presentation,
                    WorldToolsMessages.treeGrown(rawType)
                )

            else ->
                reply(
                    source,
                    adventure,
                    presentation,
                    WorldToolsMessages.treeFailed()
                )
        }
    }

    // ---------------------------------------------------------------- /burn

    private fun burnCommand(
        service: WorldToolsService,
        settings: () -> WorldToolsSettings,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): CommandDefinition =
        command(
            name = "burn",
            category = CommandCategory.ADMINISTRATION,
            permission = CommandPermissions.BURN,
            documentation = "administration/burn",
        ) {
            argument("player", word()) { target ->
                argument("seconds", double(0.1, 3_600.0)) { seconds ->
                    executes {
                        val source = context.source
                        val player = resolveTarget(source, get(target))
                            ?: return@executes reply(
                                source,
                                adventure,
                                presentation,
                                WorldToolsMessages.playerNotFound(),
                            )
                        val capped = minOf(get(seconds), settings().maxBurnSeconds)
                        service.burn(player, capped)
                        reply(
                            source,
                            adventure,
                            presentation,
                            WorldToolsMessages.burned(
                                player.gameProfile.name,
                                capped
                            ),
                        )
                    }
                }
            }
        }

    // ---------------------------------------------------------------- tree registry

    private fun treeFeature(raw: String, big: Boolean): ResourceKey<ConfiguredFeature<*, *>>? =
        when {
            big -> when (raw.lowercase()) {
                "tree", "oak" -> TreeFeatures.FANCY_OAK
                "redwood", "spruce" -> TreeFeatures.MEGA_SPRUCE
                "jungle" -> TreeFeatures.MEGA_JUNGLE_TREE
                "darkoak", "dark_oak" -> TreeFeatures.DARK_OAK
                else -> null
            }

            else -> when (raw.lowercase()) {
                "tree", "oak" -> TreeFeatures.OAK
                "birch" -> TreeFeatures.BIRCH
                "redwood", "spruce" -> TreeFeatures.MEGA_SPRUCE
                "redmushroom", "red_mushroom" -> TreeFeatures.HUGE_RED_MUSHROOM
                "brownmushroom", "brown_mushroom" -> TreeFeatures.HUGE_BROWN_MUSHROOM
                "jungle" -> TreeFeatures.JUNGLE_TREE
                "junglebush", "jungle_bush" -> TreeFeatures.JUNGLE_BUSH
                "swamp", "swamp_oak" -> TreeFeatures.SWAMP_OAK
                else -> null
            }
        }

}
