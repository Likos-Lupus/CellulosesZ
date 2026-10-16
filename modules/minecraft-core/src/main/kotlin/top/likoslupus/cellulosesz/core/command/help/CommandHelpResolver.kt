package top.likoslupus.cellulosesz.core.command.help

import net.minecraft.commands.CommandSourceStack
import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.command.dsl.CommandDocumentId
import top.likoslupus.cellulosesz.core.command.dsl.CommandNodeSpec
import top.likoslupus.cellulosesz.core.command.dsl.LiteralNodeSpec
import top.likoslupus.cellulosesz.core.command.registry.CommandRouteName
import top.likoslupus.cellulosesz.core.command.registry.VanillaCommandCatalog
import top.likoslupus.cellulosesz.core.permission.PermissionService

/**
 * A read-only view of the literals CellulosesZ actually installed in the current dispatcher
 * generation. The composition root backs it with the latest registration report, so help ownership
 * is decided from effective routes rather than from the declaration table.
 */
public fun interface EffectiveCommandRoutes {

    public fun installedLiterals(): Set<String>

}

/** The outcome of resolving a Vanilla `/help <query>` string against the CellulosesZ command set. */
public sealed interface HelpResolution {

    public data class Owned(
        public val definition: CommandDefinition,
        public val subPath: List<String>,
        public val canonical: String,
        public val documentation: CommandDocumentId,
        public val permitted: Boolean,
    ) : HelpResolution

}

/**
 * Resolves a `/help` query to a CellulosesZ definition. Only literal sub-paths are resolved; a query
 * token that is not a literal child stops the walk (it is never interpreted as a game argument).
 *
 * The first token must be a route CellulosesZ currently owns ([effectiveRoutes]); a declared but
 * skipped name resolves to `null`, so `/help <skipped>` keeps the vanilla/external document.
 *
 * The most specific documentation id along the resolved path wins (node id, else nearest ancestor,
 * else the root id), and permission is checked along the whole root -> leaf chain.
 */
public class CommandHelpResolver(
    definitions: List<CommandDefinition>,
    private val permissions: PermissionService,
    private val effectiveRoutes: EffectiveCommandRoutes,
) {

    private val byName: Map<String, CommandDefinition> = buildMap {
        definitions.forEach {
            put(it.name, it)
            put(CommandRouteName.namespaced(it.name), it)
            it.aliases.forEach { alias -> put(alias, it) }
            // A protected `cz` fallback (e.g. `/czme`) resolves to the same documentation as the
            // canonical command; it is not a DSL alias, so it is added from the conflict catalog.
            VanillaCommandCatalog.forCanonical(it.name)?.let { vanilla ->
                put(vanilla.fallbackLiteral, it)
            }
        }
    }

    public fun resolve(query: String, source: CommandSourceStack): HelpResolution.Owned? {
        val tokens = query
                .trim()
                .removePrefix("/")
                .trim()
                .split(WHITESPACE)
                .filter { it.isNotEmpty() }
        if (tokens.isEmpty()) {
            return null
        }

        val first = tokens.first()
        if (first !in effectiveRoutes.installedLiterals()) {
            return null
        }

        val definition = byName[first] ?: return null

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
            permitted = required.all { permissions.allows(source, it) },
        )
    }

    public companion object {

        private val WHITESPACE = Regex("\\s+")

    }

}
