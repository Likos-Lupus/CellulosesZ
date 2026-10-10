package top.likoslupus.cellulosesz.core.command.help

import net.kyori.adventure.text.Component

/**
 * Splits already rendered document lines into pages. It paginates whole rendered blocks, never a
 * raw character slice, so bold spans, links and multi-byte characters are never cut mid-token.
 */
public object CommandHelpPaginator {

    public const val DEFAULT_LINES_PER_PAGE: Int = 12

    public fun paginate(
        lines: List<Component>,
        pageSize: Int = DEFAULT_LINES_PER_PAGE,
    ): List<List<Component>> {
        require(pageSize > 0) { "pageSize must be positive" }
        return when {
            lines.isEmpty() -> listOf(emptyList())
            else -> lines.chunked(pageSize)
        }
    }

}
