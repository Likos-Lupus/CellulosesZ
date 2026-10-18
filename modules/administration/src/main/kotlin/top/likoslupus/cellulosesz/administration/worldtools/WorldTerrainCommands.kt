package top.likoslupus.cellulosesz.administration.worldtools

import net.minecraft.commands.CommandSourceStack
import net.minecraft.core.BlockPos
import net.minecraft.data.worldgen.features.TreeFeatures
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.block.Blocks
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
import top.likoslupus.cellulosesz.core.permission.PermissionService
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.presentation.TextPresentation

/** `/tree`, `/bigtree`, `/break`, `/ice`, `/ext` and `/burn`. */
internal object WorldTerrainCommands {

    private const val REACH: Double = 8.0
    private const val BREAK_REACH: Double = 20.0

    private val TREE_SUGGESTIONS: List<String> = listOf(
        "tree",
        "oak",
        "bigtree",
        "redwood",
        "spruce",
        "tallredwood",
        "megaredwood",
        "pine",
        "megapine",
        "birch",
        "tallbirch",
        "jungle",
        "smalljungle",
        "cocoa",
        "junglebush",
        "redmushroom",
        "brownmushroom",
        "swamp",
        "acacia",
        "darkoak",
        "megajungle",
        "crimsonfungus",
        "warpedfungus",
        "cherry",
        "mangrove",
        "tallmangrove",
        "azalea",
        "paleoak",
    )

    private val BIG_TREE_SUGGESTIONS: List<String> = TREE_SUGGESTIONS

    fun commands(
        service: WorldToolsService,
        settings: () -> WorldToolsSettings,
        permissions: PermissionService,
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
                        settings,
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
                        settings,
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
                val player = source.player!!
                val pos = lookedAtBlock(source, BREAK_REACH)
                    ?: return@executesPlayer reply(
                        source,
                        adventure,
                        presentation,
                        WorldToolsMessages.lookAtBlock(),
                    )
                if (player.level().getBlockState(pos).`is`(Blocks.BEDROCK)
                    && !permissions.allows(source, CommandPermissions.BREAK_BEDROCK)
                ) {
                    return@executesPlayer reply(
                        source,
                        adventure,
                        presentation,
                        WorldToolsMessages.bedrockDenied(),
                    )
                }
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
                service.freezePlayer(source.player!!)
                reply(
                    source,
                    adventure,
                    presentation,
                    WorldToolsMessages.iceFrozen()
                )
            }
            argument("player", word(), permission = CommandPermissions.ICE_OTHERS) { target ->
                executes {
                    val source = context.source
                    val player = resolveTarget(source, get(target))
                        ?: return@executes reply(
                            source,
                            adventure,
                            presentation,
                            WorldToolsMessages.playerNotFound(),
                        )
                    service.freezePlayer(player)
                    reply(
                        source,
                        adventure,
                        presentation,
                        WorldToolsMessages.iceFrozenOther(player.gameProfile.name)
                    )
                }
            }
        },
        command(
            name = "ext",
            category = CommandCategory.ADMINISTRATION,
            permission = CommandPermissions.EXT,
            documentation = "administration/ext",
        ) {
            executesPlayer {
                service.extinguishPlayer(source.player!!)
                reply(
                    source,
                    adventure,
                    presentation,
                    WorldToolsMessages.extinguished()
                )
            }
            argument("player", word(), permission = CommandPermissions.EXT_OTHERS) { target ->
                executes {
                    val source = context.source
                    val player = resolveTarget(source, get(target))
                        ?: return@executes reply(
                            source,
                            adventure,
                            presentation,
                            WorldToolsMessages.playerNotFound(),
                        )
                    service.extinguishPlayer(player)
                    reply(
                        source,
                        adventure,
                        presentation,
                        WorldToolsMessages.extinguishedOther(player.gameProfile.name)
                    )
                }
            }
        },
        burnCommand(service, settings, adventure, presentation),
    )

    // ---------------------------------------------------------------- helpers

    private fun lookedAtBlock(source: CommandSourceStack, reach: Double = REACH): BlockPos? {
        val player = source.player
            ?: return null
        return blockPos(player.pick(reach, 0f, false))
    }

    private fun blockPos(hit: HitResult?): BlockPos? =
        (hit as? BlockHitResult)?.blockPos

    // ---------------------------------------------------------------- /tree

    private fun applyTree(
        source: CommandSourceStack,
        rawType: String,
        big: Boolean,
        service: WorldToolsService,
        settings: () -> WorldToolsSettings,
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
        val pos = lookedAtBlock(source, settings().treeCommandRangeLimit)
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
        val base = pos.above()
        if (player.level().getBlockState(base).isSolid) {
            return reply(
                source,
                adventure,
                presentation,
                WorldToolsMessages.treeFailed()
            )
        }
        return when {
            service.growTree(player.level(), base, key) ->
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

    private fun treeFeature(raw: String, big: Boolean): ResourceKey<ConfiguredFeature<*, *>>? {
        val name = raw.lowercase().replace("_", "")
        return when (name) {
            "tree", "oak" -> if (big) TreeFeatures.FANCY_OAK else TreeFeatures.OAK
            "bigtree" -> TreeFeatures.FANCY_OAK
            "redwood", "spruce" -> if (big) TreeFeatures.MEGA_SPRUCE else TreeFeatures.SPRUCE
            "tallredwood", "megaredwood", "megaspruce" -> TreeFeatures.MEGA_SPRUCE
            "pine" -> if (big) TreeFeatures.MEGA_PINE else TreeFeatures.PINE
            "megapine" -> TreeFeatures.MEGA_PINE
            "birch" -> if (big) TreeFeatures.SUPER_BIRCH_BEES_0002 else TreeFeatures.BIRCH
            "tallbirch", "superbirch" -> TreeFeatures.SUPER_BIRCH_BEES_0002
            "jungle" -> if (big) TreeFeatures.MEGA_JUNGLE_TREE else TreeFeatures.JUNGLE_TREE
            "smalljungle" -> TreeFeatures.JUNGLE_TREE_NO_VINE
            "cocoa", "cocoatree" -> TreeFeatures.JUNGLE_TREE
            "junglebush" -> TreeFeatures.JUNGLE_BUSH
            "redmushroom" -> TreeFeatures.HUGE_RED_MUSHROOM
            "brownmushroom" -> TreeFeatures.HUGE_BROWN_MUSHROOM
            "swamp", "swampoak" -> TreeFeatures.SWAMP_OAK
            "acacia" -> TreeFeatures.ACACIA
            "darkoak" -> TreeFeatures.DARK_OAK
            "megajungle" -> TreeFeatures.MEGA_JUNGLE_TREE
            "crimsonfungus", "crimson" -> TreeFeatures.CRIMSON_FUNGUS
            "warpedfungus", "warped" -> TreeFeatures.WARPED_FUNGUS
            "cherry" -> TreeFeatures.CHERRY
            "mangrove" -> TreeFeatures.MANGROVE
            "tallmangrove" -> TreeFeatures.TALL_MANGROVE
            "azalea" -> TreeFeatures.AZALEA_TREE
            "paleoak" -> TreeFeatures.PALE_OAK
            else -> null
        }
    }

}
