package top.likoslupus.cellulosesz.utility.itemtools

import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import top.likoslupus.cellulosesz.core.command.CommandCategory
import top.likoslupus.cellulosesz.core.command.dsl.*
import top.likoslupus.cellulosesz.core.command.requiresPermission
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.permission.PermissionService
import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.adventure.replyError
import top.likoslupus.cellulosesz.core.text.i18n.LocalizedMessages
import top.likoslupus.cellulosesz.core.text.presentation.TextPresentation
import top.likoslupus.cellulosesz.core.text.presentation.replyNotice
import top.likoslupus.cellulosesz.core.text.presentation.replyPage
import top.likoslupus.cellulosesz.utility.format.*
import java.util.*

internal object ItemDisplayCommands {

    fun definitions(
        service: ItemToolsService,
        messages: LocalizedMessages,
        adventure: AdventureRuntime,
        presentation: TextPresentation,
        permissions: PermissionService,
    ): List<CommandDefinition> = listOf(
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

}
