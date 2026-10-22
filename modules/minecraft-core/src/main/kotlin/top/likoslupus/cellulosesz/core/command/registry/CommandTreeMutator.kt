package top.likoslupus.cellulosesz.core.command.registry

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.tree.CommandNode
import net.minecraft.commands.CommandSourceStack

/**
 * Removes a root child from the live dispatcher. Brigadier has no removal API and
 * [com.mojang.brigadier.tree.CommandNode.addChild] merges same-named children, so replacing a
 * vanilla literal needs this seam. Injected so the takeover flow is testable without the reflective
 * implementation.
 */
public fun interface CommandTreeMutator {

    /** Returns true only when the literal was present and is gone afterwards. */
    public fun removeRootChild(
        dispatcher: CommandDispatcher<CommandSourceStack>,
        literal: String,
    ): Boolean

}

/**
 * The only reflective code in the command path. Brigadier is a loader-provided library, so it is
 * loaded by the parent class loader before Mixin can transform it; a mixin cannot add the removal
 * operation (verified: the mixin silently does not apply and the takeover falls back). Reflection
 * into an unnamed-module library class needs no `--add-opens`.
 *
 * All three indexes are cleaned: `children` drives parsing, `literals`/`arguments` drive tab
 * completion and `/help`. Failure of any step leaves the tree as it was and returns false, so the
 * caller keeps vanilla.
 */
internal object ReflectiveCommandTreeMutator : CommandTreeMutator {

    override fun removeRootChild(
        dispatcher: CommandDispatcher<CommandSourceStack>,
        literal: String,
    ): Boolean {
        val root = dispatcher.root
        return root.getChild(literal) != null && runCatching {
            removeFrom(root, "children", literal)
            removeFrom(root, "literals", literal)
            removeFrom(root, "arguments", literal)
            root.getChild(literal) == null
        }.getOrDefault(false)
    }

    @Suppress("UNCHECKED_CAST")
    private fun removeFrom(
        node: CommandNode<*>,
        field: String,
        literal: String
    ) {
        val declared = CommandNode::class.java.getDeclaredField(field)
        declared.isAccessible = true
        val values = declared.get(node) as? MutableMap<String, Any?> ?: return
        values.remove(literal)
    }

}
