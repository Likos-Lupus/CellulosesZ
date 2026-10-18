package top.likoslupus.cellulosesz.utility.itemtools

/** Pure pagination for `/recipe` output. */
internal object RecipePager {

    const val PAGE_SIZE: Int = 8

    fun pageCount(total: Int): Int =
        when {
            total <= 0 -> 1
            else -> (total + PAGE_SIZE - 1) / PAGE_SIZE
        }

    fun slice(items: List<RecipeView>, page: Int): List<RecipeView> {
        val count = pageCount(items.size)
        val index = page.coerceIn(1, count)
        val from = (index - 1) * PAGE_SIZE
        if (from >= items.size) {
            return emptyList()
        }
        val to = minOf(from + PAGE_SIZE, items.size)
        return items.subList(from, to)
    }

}
