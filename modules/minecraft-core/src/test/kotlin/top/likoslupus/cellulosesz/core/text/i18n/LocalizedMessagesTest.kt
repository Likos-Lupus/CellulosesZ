package top.likoslupus.cellulosesz.core.text.i18n

import kotlinx.coroutines.runBlocking
import net.kyori.adventure.text.TextComponent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import top.likoslupus.cellulosesz.core.text.MessageArgument
import top.likoslupus.cellulosesz.core.text.MessageTheme
import top.likoslupus.cellulosesz.core.text.Messages
import top.likoslupus.cellulosesz.core.text.i18n.preference.PlayerLanguagePreferenceRepository
import top.likoslupus.cellulosesz.core.text.i18n.preference.PlayerLanguagePreferences
import top.likoslupus.cellulosesz.core.text.parseHexColor
import java.util.*

class LocalizedMessagesTest {

    private class FakeRepository : PlayerLanguagePreferenceRepository {

        val stored = mutableMapOf<UUID, LanguageId>()

        override suspend fun loadAll(): Map<UUID, LanguageId> =
            stored.toMap()

        override suspend fun set(playerId: UUID, language: LanguageId) {
            stored[playerId] = language
        }

        override suspend fun remove(playerId: UUID) {
            stored.remove(playerId)
        }

    }

    private val catalog = TranslationCatalog.loadBundled()
    private val preferences = PlayerLanguagePreferences(FakeRepository())
    private var defaultLanguage = "en_us"
    private val resolver = LanguageResolver(catalog, preferences) {
        LocalizationSettings(defaultLanguage)
    }
    private val theme = MessageTheme(
        primary = requireNotNull(parseHexColor("#111111")),
        secondary = requireNotNull(parseHexColor("#222222")),
    )
    private val messages = LocalizedMessages(catalog, resolver) { theme }

    @Test
    fun `primary and secondary roles receive their colors`() {
        val rendered = messages.renderFor(
            null,
            CoreMessageKeys.LANGUAGE_CURRENT,
            listOf(MessageArgument("en_us")),
        )

        val children = rendered.children()
        assertEquals(3, children.size)

        val label = children[0] as TextComponent
        assertEquals("Current language: ", label.content())
        assertEquals(theme.primary, label.color())

        val value = children[1] as TextComponent
        assertEquals("en_us", value.content())
        assertEquals(theme.secondary, value.color())
    }

    @Test
    fun `prefix is structural and secondary`() {
        val rendered = messages.renderPrefixedFor(
            null,
            CoreMessageKeys.LANGUAGE_CHANGED,
            listOf(MessageArgument("en_us")),
        )

        val prefix = rendered.children()[0] as TextComponent
        assertEquals(Messages.PREFIX, prefix.content())
        assertEquals(theme.secondary, prefix.color())
    }

    @Test
    fun `argument content is literal and never parsed`() {
        val rendered = messages.renderFor(
            null,
            CoreMessageKeys.LANGUAGE_UNKNOWN,
            listOf(MessageArgument("[[oops]] {1}")),
        )

        val argument = rendered.children()[1] as TextComponent
        assertEquals("[[oops]] {1}", argument.content())
    }

    @Test
    fun `player override wins over the server default`() =
        runBlocking {
            val player = UUID.randomUUID()
            preferences.set(player, LanguageId.ZH_CN)

            val rendered = messages.renderFor(
                player,
                CoreMessageKeys.LANGUAGE_CHANGED,
                listOf(MessageArgument("zh_cn")),
            )

            assertEquals(
                "语言已切换为 ",
                (rendered.children()[0] as TextComponent).content()
            )
        }

    @Test
    fun `server default is used without an override`() {
        defaultLanguage = "zh_cn"

        val rendered = messages.renderFor(
            UUID.randomUUID(),
            CoreMessageKeys.LANGUAGE_CHANGED,
            listOf(MessageArgument("zh_cn")),
        )

        assertEquals(
            "语言已切换为 ",
            (rendered.children()[0] as TextComponent).content()
        )
    }

    @Test
    fun `console uses the server default`() {
        defaultLanguage = "zh_cn"

        val rendered = messages.renderFor(
            null,
            CoreMessageKeys.LANGUAGE_CHANGED,
            listOf(MessageArgument("zh_cn")),
        )

        assertEquals(
            "语言已切换为 ",
            (rendered.children()[0] as TextComponent).content()
        )
    }

}
