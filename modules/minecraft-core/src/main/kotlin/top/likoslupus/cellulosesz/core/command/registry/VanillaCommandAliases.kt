package top.likoslupus.cellulosesz.core.command.registry

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.tree.CommandNode
import net.minecraft.commands.CommandSourceStack

/**
 * Keeps a displaced vanilla command reachable at `/minecraft:<literal>`. The alias is a redirect, so
 * the vanilla argument tree and executor are reused verbatim (including the requirement, which is
 * copied because the alias is a separate root node and its `requires` is evaluated first).
 *
 * A detached node is a valid redirect target: the command-tree packet walks redirects in addition to
 * children, so the client receives the vanilla subtree.
 */
internal object VanillaCommandAliases {

    const val NAMESPACE: String = "minecraft"

    fun aliasFor(literal: String): String =
        "$NAMESPACE:$literal"

    /** Returns false when the alias cannot be installed; the caller must then keep vanilla. */
    fun install(
        dispatcher: CommandDispatcher<CommandSourceStack>,
        literal: String,
        vanilla: CommandNode<CommandSourceStack>,
    ): Boolean {
        val aliasLiteral = aliasFor(literal)
        if (dispatcher.root.getChild(aliasLiteral) != null) {
            return false
        }
        val alias = LiteralArgumentBuilder.literal<CommandSourceStack>(aliasLiteral)
                .requires(vanilla.requirement)
                .redirect(vanilla)
                .build()
        dispatcher.root.addChild(alias)
        return dispatcher.root.getChild(aliasLiteral)?.redirect === vanilla
    }

}
