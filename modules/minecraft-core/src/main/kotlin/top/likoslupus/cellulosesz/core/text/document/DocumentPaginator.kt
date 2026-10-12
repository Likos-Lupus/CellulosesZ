package top.likoslupus.cellulosesz.core.text.document

import net.kyori.adventure.text.Component

/**
 * A rendered document block with layout hints. Grouping the rendered lines lets the paginator keep a
 * heading with its following block and keep a code block or list item together, instead of slicing a
 * flat line list.
 */
internal data class RenderedBlock(
    val lines: List<Component>,
    val keepWithNext: Boolean = false,
)

/**
 * Paginates rendered blocks with a fixed page budget. It is a heuristic chat layout, not a pixel
 * model: client font metrics are not controlled by the server.
 */
internal object DocumentPaginator {

    const val DEFAULT_LINES_PER_PAGE: Int = 10

    fun paginate(
        blocks: List<RenderedBlock>,
        pageSize: Int = DEFAULT_LINES_PER_PAGE,
    ): List<List<Component>> {
        require(pageSize > 0) { "pageSize must be positive" }
        if (blocks.isEmpty()) {
            return listOf(emptyList())
        }

        val pages = mutableListOf<List<Component>>()
        var current = mutableListOf<Component>()

        blocks.forEachIndexed { index, block ->
            if (current.isNotEmpty()
                && current.size + block.lines.size > pageSize
            ) {
                pages += current.toList()
                current = mutableListOf()
            }

            current += block.lines

            val next = blocks.getOrNull(index + 1)
            if (block.keepWithNext
                && next != null
                && current.size + next.lines.size > pageSize
            ) {
                pages += current.toList()
                current = mutableListOf()
            }

            while (current.size > pageSize) {
                pages += current.take(pageSize)
                current = current.drop(pageSize).toMutableList()
            }
        }

        if (current.isNotEmpty() || pages.isEmpty()) {
            pages += current.toList()
        }
        return pages
    }

}
