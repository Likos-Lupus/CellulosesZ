package top.likoslupus.cellulosesz.core.text.presentation

import top.likoslupus.cellulosesz.core.text.MessageKey

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import top.likoslupus.cellulosesz.core.text.i18n.LocalizedMessages
import top.likoslupus.cellulosesz.core.text.model.DetailView
import top.likoslupus.cellulosesz.core.text.model.LocalizedText
import top.likoslupus.cellulosesz.core.text.theme.TextRole

/** Renders a [DetailView] as `label: value` lines with no fixed-width alignment. */
internal class DetailRenderer(
    private val messages: LocalizedMessages,
) {

    fun render(context: RenderContext, detail: DetailView): List<Component> {
        val palette = context.theme.palette
        val out = mutableListOf<Component>()
        out += messages
                .render(context.language, detail.title)
                .decorate(TextDecoration.BOLD)
                .color(palette.color(TextRole.HEADING))

        detail.fields.forEach { field ->
            val label = messages
                    .render(context.language, field.label)
                    .color(palette.color(TextRole.SECONDARY))
            val separator = messages
                    .renderColored(
                        context.language,
                        LocalizedText(MessageKey("core.detail.separator")),
                        palette.color(TextRole.SECONDARY),
                    )
            val value = messages
                    .render(context.language, field.value)
            out += label
                    .append(separator)
                    .append(Component.text(" "))
                    .append(value)
        }

        detail.footer?.let {
            out += messages.render(
                context.language,
                it
            )
        }
        return out
    }

}
