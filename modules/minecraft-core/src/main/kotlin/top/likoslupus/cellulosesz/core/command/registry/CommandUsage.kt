package top.likoslupus.cellulosesz.core.command.registry

import net.minecraft.commands.CommandSourceStack
import top.likoslupus.cellulosesz.core.command.dsl.*
import top.likoslupus.cellulosesz.core.permission.PermissionService
import top.likoslupus.cellulosesz.core.permission.PermissionSpec

/**
 * Projects executable usage patterns from the declaration AST, filtered by what the given source may
 * actually use. Markdown documentation carries prose and examples; it never hand-maintains these
 * patterns.
 */
public object CommandUsage {

    public fun patterns(
        definition: CommandDefinition,
        source: CommandSourceStack,
        permissions: PermissionService,
    ): List<String> {
        if (!passes(definition.permission, definition.sourceAccess, source, permissions)) {
            return emptyList()
        }

        val base = "/" + definition.name
        val out = mutableListOf<String>()
        if (definition.root.execution != null) {
            out += base
        }
        append(definition.root.children, base, source, permissions, out)
        if (out.isEmpty()) {
            out += base
        }
        return out.distinct()
    }

    private fun append(
        children: List<CommandNodeSpec>,
        prefix: String,
        source: CommandSourceStack,
        permissions: PermissionService,
        out: MutableList<String>,
    ) {
        children.forEach { child ->
            if (!passes(child.permission, child.sourceAccess, source, permissions)) {
                return@forEach
            }
            val token = token(child)
            val path = "$prefix $token"
            if (child.execution != null) {
                out += path
            }
            append(child.children, path, source, permissions, out)
        }
    }

    private fun token(node: CommandNodeSpec): String =
        when (node) {
            is LiteralNodeSpec -> node.name
            is ArgumentNodeSpec<*> -> {
                val inner = if (node.argument.greedy) "${node.name}..." else node.name
                "<$inner>"
            }
        }

    private fun passes(
        permission: PermissionSpec?,
        access: SourceAccess,
        source: CommandSourceStack,
        permissions: PermissionService,
    ): Boolean {
        val accessOk = when (access) {
            SourceAccess.ANY -> true
            SourceAccess.PLAYER -> source.player != null
            SourceAccess.NON_PLAYER -> source.player == null
        }
        return accessOk
                && (permission == null || permissions.allows(source, permission))
    }

}
