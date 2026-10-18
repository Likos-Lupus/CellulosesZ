package top.likoslupus.cellulosesz.application.command

import net.minecraft.commands.CommandSourceStack
import top.likoslupus.cellulosesz.core.command.dsl.CommandSyntax
import top.likoslupus.cellulosesz.core.command.dsl.integer
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.permission.PermissionService
import top.likoslupus.cellulosesz.core.text.MessageArgument
import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.adventure.reply
import top.likoslupus.cellulosesz.core.text.i18n.LocalizedMessages

private const val HELP_PAGE_SIZE: Int = 10

/** `/cellulosesz help [page]`: the paginated command list. */
internal fun CommandSyntax.helpCommands(
    helpCatalog: CommandHelpCatalog,
    permissions: PermissionService,
    messages: LocalizedMessages,
    adventure: AdventureRuntime,
) {
    literal(
        name = "help",
        permission = CommandPermissions.ROOT,
        documentation = "core/help",
    ) {
        executes {
            showHelpList(
                source,
                1,
                helpCatalog,
                permissions,
                messages,
                adventure
            )
        }
        argument("page", integer(1, 1_000)) { page ->
            executes {
                showHelpList(
                    source,
                    get(page),
                    helpCatalog,
                    permissions,
                    messages,
                    adventure,
                )
            }
        }
    }
}

private fun showHelpList(
    source: CommandSourceStack,
    page: Int,
    catalog: CommandHelpCatalog,
    permissions: PermissionService,
    messages: LocalizedMessages,
    adventure: AdventureRuntime,
): Int {
    val playerId = source.player?.uuid
    val visible = catalog.definitions.filter { permissions.allows(source, it.permission) }
    val pageSize = HELP_PAGE_SIZE
    val pages = maxOf(1, (visible.size + pageSize - 1) / pageSize)
    val current = page.coerceIn(1, pages)
    source.reply(
        adventure,
        messages.renderFor(
            playerId,
            MessageKey("core.help.title"),
            listOf(
                MessageArgument(current.toString()),
                MessageArgument(pages.toString()),
            ),
        ),
    )
    visible.drop((current - 1) * pageSize).take(pageSize).forEach { definition ->
        source.reply(
            adventure,
            messages.renderFor(
                playerId,
                MessageKey("core.help.entry"),
                listOf(
                    MessageArgument(definition.name),
                    MessageArgument(definition.permission.description),
                ),
            ),
        )
    }
    return 1
}
