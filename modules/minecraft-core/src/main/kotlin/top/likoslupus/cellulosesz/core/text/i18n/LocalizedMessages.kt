package top.likoslupus.cellulosesz.core.text.i18n

import net.kyori.adventure.text.Component
import org.slf4j.LoggerFactory
import top.likoslupus.cellulosesz.core.text.MessageArgument
import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.MessageTheme
import top.likoslupus.cellulosesz.core.text.Messages
import top.likoslupus.cellulosesz.core.text.i18n.template.MessagePart
import top.likoslupus.cellulosesz.core.text.i18n.template.MessageTemplate
import java.util.*

/**
 * Renders compiled translations to Adventure components. It never parses raw template text, never
 * touches JDBC, and never re-interprets argument values: arguments are literal text bound to the
 * role active at the placeholder.
 */
public class LocalizedMessages(
    private val catalog: TranslationCatalog,
    private val languages: LanguageResolver,
    private val theme: () -> MessageTheme,
) {

    public fun renderFor(
        playerId: UUID?,
        key: MessageKey,
        arguments: List<MessageArgument> = emptyList(),
    ): Component {
        val language = languages.effective(playerId)
        val template = templateFor(language, key)
        if (template == null) {
            LOGGER.warn(
                "missing CellulosesZ translation key '{}' for '{}'",
                key.value,
                language.value
            )
            return Component.text(
                "<missing:${key.value}>",
                theme().secondary
            )
        }
        return render(template, arguments, key)
    }

    public fun renderPrefixedFor(
        playerId: UUID?,
        key: MessageKey,
        arguments: List<MessageArgument> = emptyList(),
    ): Component =
        Component.text()
                .append(Component.text(Messages.PREFIX, theme().secondary))
                .append(renderFor(playerId, key, arguments))
                .build()

    private fun templateFor(language: LanguageId, key: MessageKey): MessageTemplate? =
        catalog.template(language, key)
            ?: catalog.template(languages.serverDefault(), key)
            ?: catalog.template(BundledTranslations.baseline, key)

    private fun render(
        template: MessageTemplate,
        arguments: List<MessageArgument>,
        key: MessageKey,
    ): Component {
        val expected = (template.argumentIndexes.maxOrNull() ?: -1) + 1
        require(arguments.size == expected) {
            "message '${key.value}' expects $expected arguments but got ${arguments.size}"
        }

        val currentTheme = theme()
        val builder = Component.text()
        template.parts.forEach { part ->
            when (part) {
                is MessagePart.Text ->
                    builder.append(
                        Component.text(
                            part.value,
                            currentTheme.color(part.role)
                        )
                    )

                is MessagePart.Argument ->
                    builder.append(
                        Component.text(
                            arguments[part.index].value,
                            currentTheme.color(part.role)
                        )
                    )
            }
        }
        return builder.build()
    }

    public companion object {

        private val LOGGER = LoggerFactory.getLogger(LocalizedMessages::class.java)

    }

}

/** Ergonomic overload: `messages.renderFor(playerId, key, "value")`. */
public fun LocalizedMessages.renderFor(
    playerId: UUID?,
    key: MessageKey,
    vararg arguments: String,
): Component =
    renderFor(
        playerId,
        key,
        arguments.map(::MessageArgument)
    )
