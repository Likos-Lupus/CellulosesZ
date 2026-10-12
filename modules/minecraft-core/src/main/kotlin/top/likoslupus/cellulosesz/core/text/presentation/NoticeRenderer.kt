package top.likoslupus.cellulosesz.core.text.presentation

import net.kyori.adventure.text.Component
import top.likoslupus.cellulosesz.core.text.i18n.LocalizedMessages
import top.likoslupus.cellulosesz.core.text.model.Notice
import top.likoslupus.cellulosesz.core.text.model.NoticeTone
import top.likoslupus.cellulosesz.core.text.theme.TextRole

/**
 * Renders a [Notice] into chat lines: conclusion first, an optional second line, then one line of
 * optional actions. A non-neutral tone colours the conclusion line only; the text itself always
 * carries the meaning, so colour is never the sole signal.
 */
internal class NoticeRenderer(
    private val messages: LocalizedMessages,
) {

    fun render(context: RenderContext, notice: Notice): List<Component> {
        val out = mutableListOf<Component>()
        out += mainLine(context, notice)
        notice.detail?.let { out += messages.render(context.language, it) }
        when (notice.actions.size) {
            0 -> Unit
            1 -> out += ActionRenderer.render(messages, context, notice.actions.single())
            else -> {
                val line = Component.text()
                notice.actions.forEachIndexed { index, action ->
                    if (index > 0) {
                        line.append(Component.text("  "))
                    }
                    line.append(ActionRenderer.render(messages, context, action))
                }
                out += line.build()
            }
        }
        return out
    }

    private fun mainLine(context: RenderContext, notice: Notice): Component {
        val role = toneRole(notice.tone)
            ?: return messages.render(context.language, notice.text)
        return messages.renderColored(
            context.language,
            notice.text,
            context.theme.palette.color(role)
        )
    }

    private fun toneRole(tone: NoticeTone): TextRole? =
        when (tone) {
            NoticeTone.NEUTRAL -> null
            NoticeTone.INFO -> TextRole.BRAND
            NoticeTone.SUCCESS -> TextRole.SUCCESS
            NoticeTone.WARNING -> TextRole.WARNING
            NoticeTone.ERROR -> TextRole.ERROR
        }

}
