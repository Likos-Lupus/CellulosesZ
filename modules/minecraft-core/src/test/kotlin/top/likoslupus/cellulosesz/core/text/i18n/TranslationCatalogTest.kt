package top.likoslupus.cellulosesz.core.text.i18n

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TranslationCatalogTest {

    private val catalog = TranslationCatalog.loadBundled()

    @Test
    fun `all bundled languages are loadable`() {
        BundledTranslations.languages.forEach {
            assertTrue(
                catalog.supports(it),
                "catalog should support ${it.value}"
            )
        }
    }

    @Test
    fun `every locale has exactly the baseline key set`() {
        val baseline = catalog.keys(BundledTranslations.baseline)
        assertTrue(baseline.isNotEmpty())

        BundledTranslations.languages.forEach {
            assertEquals(
                baseline,
                catalog.keys(it),
                "key set mismatch for ${it.value}",
            )
        }
    }

    @Test
    fun `placeholder sets match the baseline per key`() {
        val baseline = catalog.keys(BundledTranslations.baseline)

        baseline.forEach { key ->
            val expected = catalog.template(BundledTranslations.baseline, key)!!.argumentIndexes
            BundledTranslations.languages.forEach {
                val actual = catalog.template(it, key)!!.argumentIndexes
                assertEquals(
                    expected,
                    actual,
                    "placeholder mismatch for '${key.value}' in ${it.value}"
                )
            }
        }
    }

}
