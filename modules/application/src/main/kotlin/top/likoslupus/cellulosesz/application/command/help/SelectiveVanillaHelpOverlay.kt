package top.likoslupus.cellulosesz.application.command.help

import com.mojang.brigadier.Command
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import top.likoslupus.cellulosesz.core.command.help.CommandDocumentationService
import top.likoslupus.cellulosesz.core.command.help.CommandHelpResolver
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.adventure.reply
import top.likoslupus.cellulosesz.core.text.document.DocumentTheme

/**
 * Selectively enhances the Vanilla `/help` query branch. It never touches the no-argument `/help`
 * executor and never replaces non-CellulosesZ queries: the saved original executor runs unchanged for
 * anything the resolver does not own.
 *
 * The decoration uses Brigadier's public merge semantics: re-registering a `help` literal that
 * carries no executor of its own keeps the Vanilla no-argument executor, while the same-named
 * `command` greedy child replaces the query executor. The previous executor identity is checked so
 * the overlay is never wrapped twice for one dispatcher.
 */
internal class SelectiveVanillaHelpOverlay(
    private val resolver: CommandHelpResolver,
    private val documentation: CommandDocumentationService,
    private val adventure: AdventureRuntime,
    private val theme: () -> DocumentTheme,
) {

    @Volatile private var installed: Command<CommandSourceStack>? = null

    fun install(dispatcher: CommandDispatcher<CommandSourceStack>) {
        val help = dispatcher.root.getChild(HELP)
            ?: return
        val query = help.getChild(QUERY_ARGUMENT)
            ?: return
        val original = query.command
            ?: return
        if (original === installed) {
            return
        }

        val wrapper = Command { context ->
            val text = StringArgumentType.getString(context, QUERY_ARGUMENT)
            val resolution = resolver.resolve(text, context.source)
            if (resolution == null) {
                original.run(context)
            } else {
                val page = documentation.page(resolution, context.source, theme())
                page.lines.forEach { line -> context.source.reply(adventure, line) }
                page.lines.size.coerceAtLeast(1)
            }
        }

        installed = wrapper
        dispatcher.register(
            Commands.literal(HELP).then(
                Commands.argument(
                    QUERY_ARGUMENT,
                    StringArgumentType.greedyString()
                ).executes(wrapper)
            )
        )
    }

    private companion object {

        private const val HELP = "help"
        private const val QUERY_ARGUMENT = "command"

    }

}
