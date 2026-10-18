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
import top.likoslupus.cellulosesz.utility.format.UtilityMessages
import java.util.*

internal object ItemToolsCommands {

    fun commands(
        service: ItemToolsService,
        known: KnownPlayerResolver,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
        permissions: PermissionService,
    ): List<CommandDefinition> = listOf(
        command(
            name = "give",
            category = CommandCategory.UTILITY,
            permission = CommandPermissions.GIVE,
            documentation = "utility/give",
        ) {
            argument("player", word()) { player ->
                suggests { source.server.playerList.players.forEach { suggest(it.gameProfile.name) } }
                argument("item", item()) { item ->
                    executes {
                        give(
                            context,
                            get(player),
                            get(item),
                            null,
                            service,
                            adventure,
                            presentation
                        )
                    }
                    argument("amount", integer(min = 1)) { amount ->
                        executes {
                            give(
                                context,
                                get(player),
                                get(item),
                                get(amount),
                                service,
                                adventure,
                                presentation
                            )
                        }
                    }
                }
            }
        },

        command(
            name = "item",
            category = CommandCategory.UTILITY,
            permission = CommandPermissions.ITEM,
            documentation = "utility/item",
        ) {
            argument("item", item()) { item ->
                executesPlayer {
                    grantItem(
                        context,
                        playerId,
                        get(item),
                        null,
                        service,
                        adventure,
                        presentation
                    )
                }
                argument("amount", integer(min = 1)) { amount ->
                    executesPlayer {
                        grantItem(
                            context,
                            playerId,
                            get(item),
                            get(amount),
                            service,
                            adventure,
                            presentation
                        )
                    }
                }
            }
        },

        command(
            name = "itemdb",
            category = CommandCategory.UTILITY,
            permission = CommandPermissions.ITEM_DB,
            documentation = "utility/itemdb",
        ) {
            executesPlayer {
                itemDb(context, playerId, null, service, adventure, presentation)
            }
            argument("item", item()) { item ->
                executesPlayer {
                    itemDb(context, playerId, get(item), service, adventure, presentation)
                }
            }
        },

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

        command(
            name = "skull",
            category = CommandCategory.UTILITY,
            permission = CommandPermissions.SKULL,
            documentation = "utility/skull",
        ) {
            executesPlayer {
                skull(
                    context,
                    playerId,
                    null,
                    null,
                    permissions,
                    messages,
                    service,
                    adventure,
                    presentation
                )
            }
            argument("owner", word()) { owner ->
                executesPlayer {
                    skull(
                        context,
                        playerId,
                        get(owner),
                        null,
                        permissions,
                        messages,
                        service,
                        adventure,
                        presentation
                    )
                }
                argument("player", word()) { player ->
                    suggests { source.server.playerList.players.forEach { suggest(it.gameProfile.name) } }
                    executes {
                        skull(
                            context,
                            context.source.player?.uuid,
                            get(owner),
                            get(player),
                            permissions,
                            messages,
                            service,
                            adventure,
                            presentation
                        )
                    }
                }
            }
        },

        command(
            name = "book",
            category = CommandCategory.UTILITY,
            permission = CommandPermissions.BOOK,
            documentation = "utility/book",
        ) {
            executesPlayer {
                book(context, playerId, BookAction.Convert, service, adventure, presentation)
            }
            literal("author") {
                argument("author", greedyString()) { author ->
                    executesPlayer {
                        book(
                            context,
                            playerId,
                            BookAction.SetAuthor(get(author)),
                            service,
                            adventure,
                            presentation
                        )
                    }
                }
            }
            literal("title") {
                argument("title", greedyString()) { title ->
                    executesPlayer {
                        book(
                            context,
                            playerId,
                            BookAction.SetTitle(get(title)),
                            service,
                            adventure,
                            presentation
                        )
                    }
                }
            }
        },

        command(
            name = "recipe",
            category = CommandCategory.UTILITY,
            permission = CommandPermissions.RECIPE,
            documentation = "utility/recipe",
        ) {
            argument("item", item()) { item ->
                executesPlayer {
                    recipe(context, playerId, get(item), 1, service, adventure, presentation)
                }
                argument("page", integer(min = 1)) { page ->
                    executesPlayer {
                        recipe(
                            context,
                            playerId,
                            get(item),
                            get(page),
                            service,
                            adventure,
                            presentation
                        )
                    }
                }
            }
        },
    )

    private fun give(
        context: CommandContext<CommandSourceStack>,
        targetName: String,
        input: net.minecraft.commands.arguments.item.ItemInput,
        amount: Int?,
        service: ItemToolsService,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val source = context.source
        return source.replyNotices(
            adventure,
            presentation,
            source.player?.uuid,
            UtilityMessages.give(service.give(targetName, input, amount)),
        )
    }

    private fun grantItem(
        context: CommandContext<CommandSourceStack>,
        actorId: UUID,
        input: net.minecraft.commands.arguments.item.ItemInput,
        amount: Int?,
        service: ItemToolsService,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val source = context.source
        return source.replyNotices(
            adventure,
            presentation,
            actorId,
            UtilityMessages.give(service.item(actorId, input, amount)),
        )
    }

    private fun itemDb(
        context: CommandContext<CommandSourceStack>,
        actorId: UUID,
        input: net.minecraft.commands.arguments.item.ItemInput?,
        service: ItemToolsService,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val source = context.source
        val info = service.itemInfo(actorId, input)
            ?: return source.replyNotice(
                adventure,
                presentation,
                actorId,
                UtilityMessages.itemInfoMissing(),
            )
        return source.replyDetail(
            adventure,
            presentation,
            actorId,
            UtilityMessages.itemInfo(info)
        )
    }

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

    private fun skull(
        context: CommandContext<CommandSourceStack>,
        actorId: UUID?,
        ownerName: String?,
        recipientName: String?,
        permissions: PermissionService,
        messages: LocalizedMessages,
        service: ItemToolsService,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val source = context.source
        val actor = actorId
            ?: return source.replyError(
                adventure,
                messages.renderFor(
                    null,
                    MessageKey("core.error.player_required"),
                ),
            )
        if (ownerName != null
            && !ownerName.equals(source.player?.gameProfile?.name, ignoreCase = true)
            && !source.requiresPermission(permissions, CommandPermissions.SKULL_OTHERS)
        ) {
            return source.replyNotice(
                adventure,
                presentation,
                actor,
                UtilityMessages.skullDenied(),
            )
        }
        val spawn = source.requiresPermission(permissions, CommandPermissions.SKULL_SPAWN)
        return source.replyNotice(
            adventure,
            presentation,
            actor,
            UtilityMessages.skull(
                service.skull(
                    actor,
                    recipientName,
                    ownerName,
                    spawn
                )
            ),
        )
    }

    private fun book(
        context: CommandContext<CommandSourceStack>,
        actorId: UUID,
        action: BookAction,
        service: ItemToolsService,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int =
        context.source.replyNotice(
            adventure,
            presentation,
            actorId,
            UtilityMessages.book(service.book(actorId, action)),
        )

    private fun recipe(
        context: CommandContext<CommandSourceStack>,
        actorId: UUID,
        input: net.minecraft.commands.arguments.item.ItemInput,
        page: Int,
        service: ItemToolsService,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
    ): Int {
        val source = context.source
        val entries = service.recipes(actorId, input)
        val pageCount = RecipePager.pageCount(entries.size)
        val index = page.coerceIn(1, pageCount)
        val itemName = service.itemInfo(actorId, input)?.displayName ?: ""
        return source.replyPage(
            adventure,
            presentation,
            actorId,
            UtilityMessages.recipes(
                itemName = itemName,
                entries = RecipePager.slice(entries, index),
                pageIndex = index,
                pageCount = pageCount,
            ),
        )
    }

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
