package top.likoslupus.cellulosesz.core.command.help

import net.minecraft.commands.CommandSourceStack
import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.command.dsl.CommandDocumentId
import top.likoslupus.cellulosesz.core.command.dsl.CommandNodeSpec
import top.likoslupus.cellulosesz.core.command.dsl.LiteralNodeSpec
import top.likoslupus.cellulosesz.core.permission.PermissionService

/** The outcome of resolving a Vanilla `/help <query>` string against the CellulosesZ command set. */
public sealed interface HelpResolution {

    public data class Owned(
        public val definition: CommandDefinition,
        public val subPath: List<String>,
        public val canonical: String,
        public val documentation: CommandDocumentId,
        public val page: Int,
        public val permitted: Boolean,
    ) : HelpResolution

}

/**
 * Resolves a `/help` query to a CellulosesZ definition. Only literal sub-paths are resolved; a query
 * token that is not a literal child stops the walk (it is never interpreted as a game argument). A
 * trailing `--page N` is parsed as the single optional tail and never leaks into a real command.
 *
 * The most specific documentation id along the resolved path wins (node id, else nearest ancestor,
 * else the root id), and permission is checked along the whole root -> leaf chain.
 */
public class CommandHelpResolver(
    definitions: List<CommandDefinition>,
    private val permissions: PermissionService,
) {

    private val byName: Map<String, CommandDefinition> = buildMap {
        definitions.forEach {
            put(it.name, it)
            it.aliases.forEach { alias -> put(alias, it) }
        }
    }

    public fun resolve(query: String, source: CommandSourceStack): HelpResolution.Owned? {
        val tokens = query
                .trim()
                .removePrefix("/")
                .trim()
                .split(WHITESPACE)
                .filter { it.isNotEmpty() }
                .toMutableList()
        if (tokens.isEmpty()) {
            return null
        }

        var requestedPage = 1
        val pageIndex = tokens.indexOf(PAGE_FLAG)
        if (pageIndex >= 0 && pageIndex == tokens.size - 2) {
            requestedPage = tokens[pageIndex + 1].toIntOrNull() ?: 0
            tokens.subList(pageIndex, tokens.size).clear()
        }
        if (tokens.isEmpty()) {
            return null
        }

        val definition = byName[tokens.first()] ?: return null

        val subPath = mutableListOf<String>()
        val required = mutableListOf(definition.permission)
        var effectiveDocumentation = definition.documentation
        var node: CommandNodeSpec? = definition.root
        var index = 1
        while (index < tokens.size) {
            val child = node?.children
                    ?.filterIsInstance<LiteralNodeSpec>()
                    ?.firstOrNull { it.name == tokens[index] }
                ?: break
            subPath += tokens[index]
            child.permission?.let(required::add)
            child.documentation?.let { effectiveDocumentation = it }
            node = child
            index++
        }

        val canonical = (listOf(definition.name) + subPath).joinToString(" ")
        return HelpResolution.Owned(
            definition = definition,
            subPath = subPath,
            canonical = canonical,
            documentation = effectiveDocumentation,
            page = if (requestedPage < 1) 1 else requestedPage,
            permitted = required.all { permissions.allows(source, it) },
        )
    }

    public companion object {

        private val WHITESPACE = Regex("\\s+")
        private const val PAGE_FLAG = "--page"

    }

}
