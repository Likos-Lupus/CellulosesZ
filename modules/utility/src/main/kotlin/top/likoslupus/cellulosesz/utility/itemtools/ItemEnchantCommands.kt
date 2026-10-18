package top.likoslupus.cellulosesz.utility.itemtools

import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.command.dsl.*
import top.likoslupus.cellulosesz.core.command.requiresPermission
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.permission.PermissionService
import top.likoslupus.cellulosesz.core.player.KnownPlayerResolver
import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.adventure.replyError
import top.likoslupus.cellulosesz.core.text.i18n.LocalizedMessages
import top.likoslupus.cellulosesz.core.text.presentation.*
import top.likoslupus.cellulosesz.utility.format.*
import java.util.*

internal object ItemEnchantCommands {

    fun definitions(
        service: ItemToolsService,
        known: KnownPlayerResolver,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
        permissions: PermissionService,
    ): List<CommandDefinition> = listOf(
        command(
            name = "enchant",
            category = CommandCategory.UTILITY,
            permission = CommandPermissions.ENCHANT,
            documentation = "utility/enchant",
        ) {
            argument("enchantment", word()) { enchantment ->
                suggests { suggestEnchantments(this) }
                executesPlayer {
                    enchant(
                        context,
                        get(enchantment),
                        null,
                        playerId,
                        null,
                        service,
                        permissions,
                        messages,
                        adventure,
                        presentation
                    )
                }
                argument("level", integer(min = 0)) { level ->
                    executesPlayer {
                        enchant(
                            context,
                            get(enchantment),
                            get(level),
                            playerId,
                            null,
                            service,
                            permissions,
                            messages,
                            adventure,
                            presentation
                        )
                    }
                    argument("player", word()) { player ->
                        suggests { source.server.playerList.players.forEach { suggest(it.gameProfile.name) } }
                        executes {
                            enchant(
                                context,
                                get(enchantment),
                                get(level),
                                null,
                                get(player),
                                service,
                                permissions,
                                messages,
                                adventure,
                                presentation
                            )
                        }
                    }
                }
            }
        },

        command(
            name = "potion",
            category = CommandCategory.UTILITY,
            permission = CommandPermissions.POTION,
            documentation = "utility/potion",
        ) {
            literal("apply", permission = CommandPermissions.POTION_APPLY) {
                executesPlayer {
                    potion(context, playerId, PotionSpec.Apply, service, adventure, presentation)
                }
            }
            literal("clear") {
                executesPlayer {
                    potion(context, playerId, PotionSpec.Clear, service, adventure, presentation)
                }
                argument(
                    "player",
                    word(),
                    permission = CommandPermissions.POTION_OTHERS,
                ) { player ->
                    suggests { source.server.playerList.players.forEach { suggest(it.gameProfile.name) } }
                    executes {
                        potionFor(
                            context,
                            get(player),
                            PotionSpec.Clear,
                            known,
                            service,
                            messages,
                            adventure,
                            presentation
                        )
                    }
                }
            }
            argument("spec", word()) { spec ->
                suggests { suggestPotionSpecs(this) }
                executesPlayer {
                    potionSpec(
                        context,
                        playerId,
                        get(spec),
                        service,
                        adventure,
                        presentation
                    )
                }
                argument(
                    "player",
                    word(),
                    permission = CommandPermissions.POTION_OTHERS,
                ) { player ->
                    suggests { source.server.playerList.players.forEach { suggest(it.gameProfile.name) } }
                    executes {
                        potionSpecFor(
                            context,
                            get(player),
                            get(spec),
                            known,
                            service,
                            messages,
                            adventure,
                            presentation
                        )
                    }
                }
            }
        },
    )

    private fun enchant(
        context: CommandContext<CommandSourceStack>,
        enchantment: String,
        level: Int?,
        actorId: UUID?,
        targetName: String?,
        service: ItemToolsService,
        permissions: PermissionService,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val source = context.source
        val unsafe = source.requiresPermission(permissions, CommandPermissions.ENCHANT_UNSAFE)
        val resolvedTarget = targetName
            ?: source.player?.gameProfile?.name
            ?: return source.replyError(
                adventure,
                messages.renderFor(
                    null,
                    MessageKey("core.error.player_required"),
                ),
            )
        return source.replyNotice(
            adventure,
            presentation,
            actorId,
            UtilityMessages.enchant(
                service.enchant(
                    resolvedTarget,
                    enchantment,
                    level,
                    unsafe
                )
            ),
        )
    }

    private fun potion(
        context: CommandContext<CommandSourceStack>,
        actorId: UUID,
        spec: PotionSpec,
        service: ItemToolsService,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int =
        context.source.replyNotice(
            adventure,
            presentation,
            actorId,
            UtilityMessages.potion(service.potion(actorId, spec)),
        )

    private fun potionSpec(
        context: CommandContext<CommandSourceStack>,
        actorId: UUID,
        raw: String,
        service: ItemToolsService,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int =
        potion(
            context,
            actorId,
            PotionSpec.parse(raw),
            service,
            adventure,
            presentation
        )

    private fun potionFor(
        context: CommandContext<CommandSourceStack>,
        targetName: String,
        spec: PotionSpec,
        known: KnownPlayerResolver,
        service: ItemToolsService,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val targetId = known.onlineByName(targetName)?.id
            ?: return notFound(context, messages, adventure)
        return potion(
            context,
            targetId,
            spec,
            service,
            adventure,
            presentation
        )
    }

    private fun potionSpecFor(
        context: CommandContext<CommandSourceStack>,
        targetName: String,
        raw: String,
        known: KnownPlayerResolver,
        service: ItemToolsService,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int =
        potionFor(
            context,
            targetName,
            PotionSpec.parse(raw),
            known,
            service,
            messages,
            adventure,
            presentation
        )

    private fun notFound(
        context: CommandContext<CommandSourceStack>,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
    ): Int =
        context.source.replyError(
            adventure,
            messages.renderFor(
                context.source.player?.uuid,
                MessageKey("utility.error.player_not_found"),
            ),
        )

    private fun suggestEnchantments(scope: SuggestionScope) {
        scope.source.server.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .listElements()
                .forEach { scope.suggest(it.key().identifier().path) }
    }

    private fun suggestPotionSpecs(scope: SuggestionScope) {
        scope.suggest("apply")
        scope.suggest("clear")
        BuiltInRegistries.MOB_EFFECT.keySet().forEach {
            scope.suggest("effect:" + it.path)
        }
    }

}
