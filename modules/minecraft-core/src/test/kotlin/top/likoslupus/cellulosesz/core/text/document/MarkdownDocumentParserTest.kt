package top.likoslupus.cellulosesz.core.text.document

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class MarkdownDocumentParserTest {

    @Test
    fun `parses headings, paragraphs and inline emphasis`() {
        val doc = MarkdownDocumentParser.parse(
            """
            # /home

            Teleport to a **saved** home with `name`.
            """.trimIndent()
        )
        assertEquals(2, doc.blocks.size)
        val heading = doc.blocks[0] as MarkdownBlock.Heading
        assertEquals(1, heading.level)
        val paragraph = doc.blocks[1] as MarkdownBlock.Paragraph
        assertTrue(paragraph.spans.any { it is MarkdownSpan.Code && it.value == "name" })
        assertTrue(
            paragraph.spans.any {
                it is MarkdownSpan.Text && it.bold && it.value.contains("saved")
            }
        )
    }

    @Test
    fun `parses bullet lists whose code spans contain angle brackets`() {
        val doc = MarkdownDocumentParser.parse(
            """
            - `/tp <target>` teleports you.
            - `/tp <from> <to>` moves a player.
            """.trimIndent()
        )
        assertEquals(2, doc.blocks.size)
        assertTrue(doc.blocks.all { it is MarkdownBlock.BulletItem })
    }

    @Test
    fun `parses code blocks, quotes and dividers`() {
        val doc = MarkdownDocumentParser.parse(
            """
            ## Example

            ```
            /home base
            ```

            > note

            ---
            """.trimIndent()
        )
        assertTrue(doc.blocks.any { it is MarkdownBlock.CodeBlock })
        assertTrue(doc.blocks.any { it is MarkdownBlock.Quote })
        assertTrue(doc.blocks.contains(MarkdownBlock.Divider))
    }

    @Test
    fun `parses links`() {
        val doc = MarkdownDocumentParser.parse("[docs](https://example.com)")
        val paragraph = doc.blocks.single() as MarkdownBlock.Paragraph
        val link = paragraph.spans.filterIsInstance<MarkdownSpan.Link>().single()
        assertEquals("docs", link.label)
        assertEquals("https://example.com", link.target)
    }

    @Test
    fun `rejects raw html`() {
        assertThrows<MarkdownDocumentException> {
            MarkdownDocumentParser.parse("<div>nope</div>")
        }
    }

    @Test
    fun `rejects images`() {
        assertThrows<MarkdownDocumentException> {
            MarkdownDocumentParser.parse("![alt](https://example.com/a.png)")
        }
    }

}
