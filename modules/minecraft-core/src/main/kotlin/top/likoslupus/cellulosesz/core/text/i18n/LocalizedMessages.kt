package top.likoslupus.cellulosesz.core.text.i18n

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import top.likoslupus.cellulosesz.core.text.MessageArgument
import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.i18n.template.MessagePart
import top.likoslupus.cellulosesz.core.text.i18n.template.MessageRole
import top.likoslupus.cellulosesz.core.text.i18n.template.MessageTemplate
import top.likoslupus.cellulosesz.core.text.model.LocalizedText
import top.likoslupus.cellulosesz.core.text.model.TextArgument
import top.likoslupus.cellulosesz.core.text.theme.TextRole
import top.likoslupus.cellulosesz.core.text.theme.TextTheme
import java.util.*
import kotlin.time.Duration

/**
 * Renders compiled translations to Adventure components. It never parses raw template text, never
 * touches JDBC, and never re-interprets argument values: arguments are literal text bound to the
 * role active at the placeholder. Every translatable word — including duration units and structural
 * markers — comes from the catalog; only formatting logic stays in code.
 */
public class LocalizedMessages(
    private val catalog: TranslationCatalog,
    private val languages: LanguageResolver,
    private val theme: () -> TextTheme,
    private val overlay: MessageOverlay = MessageOverlay.Empty,
) {

    public fun renderFor(
        playerId: UUID?,
        key: MessageKey,
        arguments: List<MessageArgument> = emptyList(),
    ): Component =
        render(
            languages.effective(playerId),
            LocalizedText(
                key,
                arguments.map { TextArgument.Literal(it.value) }
            ),
        )

    /** Renders a [LocalizedText] for an explicit language (no player lookup). */
    public fun render(
        language: LanguageId,
        text: LocalizedText,
    ): Component {
        val resolved = resolve(language)
        val template = templateFor(resolved, text.key)
            ?: return missing(text.key)
        requireArity(template, text)
        val palette = theme().palette
        return build(
            template,
            text.arguments,
            resolved
        ) { palette.color(it.toTextRole()) }
    }

    /** Renders a [LocalizedText] forcing one colour on every part (used for markers and units). */
    internal fun renderColored(
        language: LanguageId,
        text: LocalizedText,
        color: TextColor,
    ): Component {
        val resolved = resolve(language)
        val template = templateFor(resolved, text.key)
            ?: return missing(text.key)
        requireArity(template, text)
        return build(
            template,
            text.arguments,
            resolved
        ) { color }
    }

    private fun build(
        template: MessageTemplate,
        arguments: List<TextArgument>,
        language: LanguageId,
        colorFor: (MessageRole) -> TextColor,
    ): Component {
        val builder = Component.text()
        template.parts.forEach { part ->
            when (part) {
                is MessagePart.Text ->
                    builder.append(
                        Component.text(
                            part.value,
                            colorFor(part.role)
                        )
                    )

                is MessagePart.Argument ->
                    builder.append(
                        renderArgument(
                            arguments[part.index],
                            language,
                            colorFor(part.role)
                        )
                    )
            }
        }
        return builder.build()
    }

    private fun renderArgument(
        argument: TextArgument,
        language: LanguageId,
        color: TextColor,
    ): Component =
        when (argument) {
            is TextArgument.Literal -> Component.text(argument.value, color)
            is TextArgument.PlayerName -> Component.text(argument.value, color)
            is TextArgument.Identifier -> Component.text(argument.value, color)
            is TextArgument.Command -> Component.text(argument.value, color)
            is TextArgument.Count -> Component.text(argument.value.toString(), color)
            is TextArgument.Elapsed -> durationComponent(argument.value, language, color)
            is TextArgument.Position -> positionComponent(argument, language, color)
        }

    private fun durationComponent(
        duration: Duration,
        language: LanguageId,
        color: TextColor,
    ): Component {
        val total = duration.inWholeSeconds.coerceAtLeast(0)
        val units = listOf(
            MessageKey("core.duration.days") to total / 86_400,
            MessageKey("core.duration.hours") to (total % 86_400) / 3_600,
            MessageKey("core.duration.minutes") to (total % 3_600) / 60,
            MessageKey("core.duration.seconds") to total % 60,
        )
        val selected = units
                .filter { (_, value) -> value > 0L }
                .take(2)
                .ifEmpty { listOf(MessageKey("core.duration.seconds") to 0L) }

        val builder = Component.text()
        selected.forEachIndexed { index, (key, value) ->
            if (index > 0) {
                builder.append(Component.text(" ", color))
            }
            builder.append(
                renderColored(
                    language,
                    LocalizedText(key, listOf(TextArgument.Count(value))),
                    color
                )
            )
        }
        return builder.build()
    }

    private fun positionComponent(
        position: TextArgument.Position,
        language: LanguageId,
        color: TextColor,
    ): Component =
        if (position.dimension == null) {
            renderColored(
                language,
                LocalizedText(
                    MessageKey("core.position"),
                    listOf(
                        TextArgument.Count(position.x.toLong()),
                        TextArgument.Count(position.y.toLong()),
                        TextArgument.Count(position.z.toLong()),
                    ),
                ),
                color,
            )
        } else {
            renderColored(
                language,
                LocalizedText(
                    MessageKey("core.position.dimension"),
                    listOf(
                        TextArgument.Count(position.x.toLong()),
                        TextArgument.Count(position.y.toLong()),
                        TextArgument.Count(position.z.toLong()),
                        TextArgument.Literal(position.dimension),
                    ),
                ),
                color,
            )
        }

    private fun resolve(language: LanguageId): LanguageId =
        language.takeIf(catalog::supports)
            ?: languages.serverDefault().takeIf(catalog::supports)
            ?: BundledTranslations.baseline

    private fun requireArity(template: MessageTemplate, text: LocalizedText) {
        val expected = (template.argumentIndexes.maxOrNull() ?: -1) + 1
        require(text.arguments.size == expected) {
            "message '${text.key.value}' expects $expected arguments but got ${text.arguments.size}"
        }
    }

    private fun missing(key: MessageKey): Component =
        Component.text(
            "<missing:${key.value}>",
            theme().palette.color(TextRole.ERROR)
        )

    private fun templateFor(language: LanguageId, key: MessageKey): MessageTemplate? =
        overlay.template(language, key)
            ?: catalog.template(language, key)
            ?: overlay.template(languages.serverDefault(), key)
            ?: catalog.template(languages.serverDefault(), key)
            ?: overlay.template(BundledTranslations.baseline, key)
            ?: catalog.template(BundledTranslations.baseline, key)

}

internal fun MessageRole.toTextRole(): TextRole =
    when (this) {
        MessageRole.PRIMARY -> TextRole.BODY
        MessageRole.SECONDARY -> TextRole.VALUE
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
