package top.likoslupus.cellulosesz.administration.format

import top.likoslupus.cellulosesz.core.text.model.TextArgument

/**
 * Small shared constructors for [TextArgument]s used by several administration message factories.
 * Admin-authored text always becomes a [TextArgument.Literal] so it can never be re-parsed as a
 * template.
 */
internal object AdminText {

    fun literal(value: String): TextArgument =
        TextArgument.Literal(value)

    fun identifier(value: String): TextArgument =
        TextArgument.Identifier(value)

    fun player(value: String): TextArgument =
        TextArgument.PlayerName(value)

    fun count(value: Int): TextArgument =
        TextArgument.Count(value.toLong())

    fun count(value: Long): TextArgument =
        TextArgument.Count(value)

}
