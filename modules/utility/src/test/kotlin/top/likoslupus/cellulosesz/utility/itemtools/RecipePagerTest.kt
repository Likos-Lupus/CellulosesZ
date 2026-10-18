package top.likoslupus.cellulosesz.utility.itemtools

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RecipePagerTest {

    private fun entries(count: Int): List<RecipeView> =
        (1..count).map {
            RecipeView(
                "id$it",
                "type",
                "result",
                1,
                emptyList()
            )
        }

    @Test
    fun `page count is at least one`() {
        assertEquals(
            1,
            RecipePager.pageCount(0)
        )
        assertEquals(
            1,
            RecipePager.pageCount(RecipePager.PAGE_SIZE)
        )
        assertEquals(
            2,
            RecipePager.pageCount(RecipePager.PAGE_SIZE + 1)
        )
    }

    @Test
    fun `slice clamps the page index`() {
        assertEquals(
            RecipePager.PAGE_SIZE,
            RecipePager.slice(entries(20), 1).size
        )
        assertEquals(
            4,
            RecipePager.slice(entries(20), 99).size
        )
        assertEquals(
            RecipePager.PAGE_SIZE,
            RecipePager.slice(entries(20), -1).size
        )
    }

}
