package top.likoslupus.cellulosesz.core.text.i18n

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class LanguageIdTest {

    @Test
    fun `normalizes external input to the canonical form`() {
        assertEquals("en_us", LanguageId.parse("en_us")?.value)
        assertEquals("en_us", LanguageId.parse("EN_US")?.value)
        assertEquals("zh_cn", LanguageId.parse("zh-CN")?.value)
        assertEquals("en_us", LanguageId.parse("  en_us  ")?.value)
    }

    @Test
    fun `rejects malformed identifiers`() {
        assertNull(LanguageId.parse("english"))
        assertNull(LanguageId.parse(""))
        assertNull(LanguageId.parse("e_1"))
        assertNull(LanguageId.parse("en"))
        assertNull(LanguageId.parse("en_toolonglanguage"))
    }

}
