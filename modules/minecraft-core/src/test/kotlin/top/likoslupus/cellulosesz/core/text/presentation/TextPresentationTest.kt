package top.likoslupus.cellulosesz.core.text.presentation

import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.event.ClickEvent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.TextSettings
import top.likoslupus.cellulosesz.core.text.i18n.*
import top.likoslupus.cellulosesz.core.text.i18n.preference.PlayerLanguagePreferenceRepository
import top.likoslupus.cellulosesz.core.text.i18n.preference.PlayerLanguagePreferences
import top.likoslupus.cellulosesz.core.text.model.*
import top.likoslupus.cellulosesz.core.text.theme.TextThemeResolver
import java.util.*

class TextPresentationTest {

    private class NoPreferences : PlayerLanguagePreferenceRepository {

        override suspend fun loadAll(): Map<UUID, LanguageId> =
            emptyMap()

        override suspend fun set(playerId: UUID, language: LanguageId) =
            Unit

        override suspend fun remove(playerId: UUID) =
            Unit

    }

    private val catalog = TranslationCatalog.load(
        listOf(
            TranslationSource(
                "core",
                BundledTranslations::class.java
            )
        )
    )
    private val resolver = LanguageResolver(
        catalog,
        PlayerLanguagePreferences(NoPreferences())
    ) {
        LocalizationSettings("en_us")
    }
    private val theme = TextThemeResolver.resolve(TextSettings())
    private val messages = LocalizedMessages(
        catalog,
        resolver
    ) { theme }
    private val presentation = DefaultTextPresentation(
        messages,
        resolver
    ) { theme }

    @Test
    fun `a notice renders one line with no prefix`() {
        val lines = presentation.renderFor(
            null,
            Notice(
                LocalizedText(
                    MessageKey("core.language.changed"),
                    listOf(TextArgument.Literal("en_us"))
                )
            ),
        )

        assertEquals(1, lines.size)
        val rendered = lines.single() as TextComponent
        assertTrue(rendered.toString().isNotEmpty())
        assertTrue(!rendered.toString().contains("[CellulosesZ]"))
    }

    @Test
    fun `a literal argument is never re-parsed`() {
        val lines = presentation.renderFor(
            null,
            Notice(
                LocalizedText(
                    MessageKey("core.language.changed"),
                    listOf(TextArgument.Literal("[[oops]] {1}")),
                )
            ),
        )

        assertTrue(lines.single().toString().contains("[[oops]] {1}"))
    }

    @Test
    fun `a suggest action uses suggestCommand, never run`() {
        val lines = presentation.renderFor(
            null,
            Notice(
                text = LocalizedText(
                    MessageKey("core.language.changed"),
                    listOf(TextArgument.Literal("x"))
                ),
                actions = listOf(
                    UiAction.SuggestCommand(
                        label = LocalizedText(MessageKey("help.usage")),
                        command = "/home",
                    )
                ),
            ),
        )

        val action = lines.last()
        val click = action.clickEvent()
        assertNotNull(click)
        assertEquals(ClickEvent.Action.SUGGEST_COMMAND, click?.action())
    }

    @Test
    fun `an empty list page renders an empty state`() {
        val lines = presentation.renderFor(
            null,
            ListPage(
                title = LocalizedText(MessageKey("help.usage")),
                rows = emptyList(),
                emptyText = LocalizedText(
                    MessageKey("help.document.missing"),
                    listOf(TextArgument.Literal("x"))
                ),
            ),
        )

        assertTrue(lines.size >= 2)
    }

    @Test
    fun `rows render one line each`() {
        val lines = presentation.renderFor(
            null,
            ListPage(
                title = LocalizedText(MessageKey("help.usage")),
                rows = listOf(
                    ListRow(
                        LocalizedText(
                            MessageKey("core.language.changed"),
                            listOf(TextArgument.Literal("a"))
                        )
                    ),
                    ListRow(
                        LocalizedText(
                            MessageKey("core.language.changed"),
                            listOf(TextArgument.Literal("b"))
                        )
                    ),
                ),
                emptyText = LocalizedText(MessageKey("help.usage")),
            ),
        )

        assertEquals(3, lines.size)
    }

    @Test
    fun `comfortable density inserts a blank line before an optional summary`() {
        val page = ListPage(
            title = LocalizedText(MessageKey("help.usage")),
            rows = emptyList(),
            emptyText = LocalizedText(MessageKey("help.document.restricted")),
            summary = LocalizedText(
                MessageKey("help.document.missing"),
                listOf(TextArgument.Literal("x"))
            ),
        )
        val compact = DefaultTextPresentation(messages, resolver) {
            TextThemeResolver.resolve(TextSettings(density = "compact"))
        }

        assertEquals(
            compact.renderFor(null, page).size + 1,
            presentation.renderFor(null, page).size,
        )
    }

}
