package top.likoslupus.cellulosesz.core.text.i18n

import kotlinx.coroutines.runBlocking
import net.kyori.adventure.text.TextComponent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import top.likoslupus.cellulosesz.core.text.MessageArgument
import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.TextColorOverrides
import top.likoslupus.cellulosesz.core.text.TextSettings
import top.likoslupus.cellulosesz.core.text.i18n.preference.PlayerLanguagePreferenceRepository
import top.likoslupus.cellulosesz.core.text.i18n.preference.PlayerLanguagePreferences
import top.likoslupus.cellulosesz.core.text.theme.TextThemeResolver
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

    private val catalog = TranslationCatalog.load(
        listOf(
            TranslationSource(
                "core",
                BundledTranslations::class.java
            )
        )
    )
    private val preferences = PlayerLanguagePreferences(FakeRepository())
    private var defaultLanguage = "en_us"
    private val resolver = LanguageResolver(catalog, preferences) {
        LocalizationSettings(defaultLanguage)
    }
    private val theme = TextThemeResolver.resolve(
        TextSettings(
            colors = TextColorOverrides(
                body = "#111111",
                value = "#222222"
            )
        )
    )
    private val messages = LocalizedMessages(
        catalog,
        resolver,
        theme = { theme },
    )

    @Test
    fun `primary and secondary roles receive their colors`() {
        val rendered = messages.renderFor(
            null,
            MessageKey("core.language.current"),
            listOf(MessageArgument("en_us")),
        )

        val children = rendered.children()
        assertEquals(3, children.size)

        val label = children[0] as TextComponent
        assertEquals("Language: ", label.content())
        assertEquals(theme.palette.body, label.color())

        val value = children[1] as TextComponent
        assertEquals("en_us", value.content())
        assertEquals(theme.palette.value, value.color())
    }

    @Test
    fun `argument content is literal and never parsed`() {
        val rendered = messages.renderFor(
            null,
            MessageKey("core.language.unknown"),
            listOf(MessageArgument("[[oops]] {1}")),
        )

        val argument = rendered.children()[0] as TextComponent
        assertEquals("[[oops]] {1}", argument.content())
    }

    @Test
    fun `player override wins over the server default`() =
        runBlocking {
            val player = UUID.randomUUID()
            preferences.set(player, LanguageId.ZH_CN)

            val rendered = messages.renderFor(
                player,
                MessageKey("core.language.changed"),
                listOf(MessageArgument("zh_cn")),
            )

            assertEquals(
                "已切换为 ",
                (rendered.children()[0] as TextComponent).content()
            )
        }

    @Test
    fun `server default is used without an override`() {
        defaultLanguage = "zh_cn"

        val rendered = messages.renderFor(
            UUID.randomUUID(),
            MessageKey("core.language.changed"),
            listOf(MessageArgument("zh_cn")),
        )

        assertEquals(
            "已切换为 ",
            (rendered.children()[0] as TextComponent).content()
        )
    }

    @Test
    fun `console uses the server default`() {
        defaultLanguage = "zh_cn"

        val rendered = messages.renderFor(
            null,
            MessageKey("core.language.changed"),
            listOf(MessageArgument("zh_cn")),
        )

        assertEquals(
            "已切换为 ",
            (rendered.children()[0] as TextComponent).content()
        )
    }

}
