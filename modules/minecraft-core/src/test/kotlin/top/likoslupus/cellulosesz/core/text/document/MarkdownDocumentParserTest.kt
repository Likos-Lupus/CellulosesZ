package top.likoslupus.cellulosesz.core.text.document

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class MarkdownDocumentParserTest {

    @Test
    fun `parses headings, paragraphs and inline emphasis`() {
        val doc = MarkdownDocumentParser.parse(
            /* language=Markdown */ """
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
    fun `groups a bullet list into a single block`() {
        val doc = MarkdownDocumentParser.parse(
            /* language=Markdown */ """
            - `/tp <target>` teleports you.
            - `/tp <from> <to>` moves a player.
            """.trimIndent()
        )
        val list = doc.blocks.single() as MarkdownBlock.ListBlock
        assertEquals(false, list.ordered)
        assertEquals(2, list.entries.size)
        assertTrue(list.entries.all { it.depth == 0 })
        assertTrue(list.entries.all { it.number == null })
    }

    @Test
    fun `preserves ordered list numbering and nesting depth`() {
        val doc = MarkdownDocumentParser.parse(
            /* language=Markdown */ """
            3. third
            4. fourth
               - nested
            """.trimIndent()
        )
        val list = doc.blocks.single() as MarkdownBlock.ListBlock
        assertTrue(list.ordered)
        assertEquals(listOf(3, 4, null), list.entries.map { it.number })
        assertEquals(listOf(0, 0, 1), list.entries.map { it.depth })
    }

    @Test
    fun `parses code blocks, quotes and dividers`() {
        val doc = MarkdownDocumentParser.parse(
            /* language=Markdown */ """
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
        val doc = MarkdownDocumentParser.parse(/* language=Markdown */ "[docs](https://example.com)")
        val paragraph = doc.blocks.single() as MarkdownBlock.Paragraph
        val link = paragraph.spans.filterIsInstance<MarkdownSpan.Link>().single()
        assertEquals("docs", link.label)
        assertEquals("https://example.com", link.target)
    }

    @Test
    fun `rejects raw html`() {
        assertThrows<MarkdownDocumentException> {
            MarkdownDocumentParser.parse(/* language=Markdown */ "<div>nope</div>")
        }
    }

    @Test
    fun `rejects images`() {
        assertThrows<MarkdownDocumentException> {
            MarkdownDocumentParser.parse(/* language=Markdown */ "![alt](https://example.com/a.png)")
        }
    }

}
