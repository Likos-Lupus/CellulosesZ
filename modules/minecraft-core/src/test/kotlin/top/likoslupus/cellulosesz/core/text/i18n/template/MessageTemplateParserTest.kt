package top.likoslupus.cellulosesz.core.text.i18n.template

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MessageTemplateParserTest {

    private fun parse(source: String): MessageTemplate {
        val result = MessageTemplateParser.parse(source)
        assertTrue(
            result is TemplateParseResult.Success,
            "expected success for '$source': $result"
        )
        return (result as TemplateParseResult.Success).template
    }

    private fun failure(source: String): TemplateParseResult.Failure {
        val result = MessageTemplateParser.parse(source)
        assertTrue(
            result is TemplateParseResult.Failure,
            "expected failure for '$source'"
        )
        return result as TemplateParseResult.Failure
    }

    private fun describe(template: MessageTemplate): String =
        template.parts.joinToString("|") { part ->
            when (part) {
                is MessagePart.Text -> "${part.role}:${part.value}"
                is MessagePart.Argument -> "${part.role}#${part.index}"
            }
        }

    @Test
    fun `plain text is primary`() {
        assertEquals(
            "PRIMARY:hello",
            describe(parse("hello"))
        )
    }

    @Test
    fun `double brackets mark a secondary span`() {
        assertEquals(
            "PRIMARY:a |SECONDARY:b|PRIMARY: c",
            describe(parse("a [[b]] c"))
        )
    }

    @Test
    fun `multiple secondary spans`() {
        assertEquals(
            "PRIMARY:x |SECONDARY:y|PRIMARY: |SECONDARY:z",
            describe(parse("x [[y]] [[z]]"))
        )
    }

    @Test
    fun `arguments inherit the role where they appear`() {
        assertEquals(
            "PRIMARY:x |PRIMARY#0",
            describe(parse("x {0}"))
        )
        assertEquals(
            "SECONDARY#0",
            describe(parse("[[{0}]]"))
        )
        assertEquals(
            "PRIMARY:a |SECONDARY#0|PRIMARY: b |SECONDARY#1",
            describe(parse("a [[{0}]] b [[{1}]]"))
        )
    }

    @Test
    fun `arguments may be reordered`() {
        assertEquals(
            "PRIMARY#1|PRIMARY: |PRIMARY#0",
            describe(parse("{1} {0}"))
        )
        assertEquals(
            setOf(0, 1),
            parse("{1} {0}").argumentIndexes
        )
    }

    @Test
    fun `escapes yield literal delimiter characters`() {
        assertEquals(
            "PRIMARY:[[x]]",
            describe(parse("\\[\\[x\\]\\]"))
        )
        assertEquals(
            "PRIMARY:{0}",
            describe(parse("\\{0\\}"))
        )
        assertEquals(
            "PRIMARY:a\\b",
            describe(parse("a\\\\b"))
        )
    }

    @Test
    fun `empty template is valid`() {
        assertEquals(
            "",
            describe(parse(""))
        )
        assertTrue(parse("").argumentIndexes.isEmpty())
    }

    @Test
    fun `cjk text is preserved`() {
        assertEquals(
            "PRIMARY:语言 |SECONDARY:en|PRIMARY: 已切换",
            describe(parse("语言 [[en]] 已切换"))
        )
    }

    @Test
    fun `unmatched opening is rejected`() {
        failure("a [[b")
    }

    @Test
    fun `unmatched closing is rejected`() {
        failure("a ]] b")
    }

    @Test
    fun `invalid placeholder is rejected`() {
        failure("{x}")
        failure("{0")
        failure("{}")
    }

    @Test
    fun `dangling and unknown escapes are rejected`() {
        failure("a\\")
        failure("a\\b")
    }

}
