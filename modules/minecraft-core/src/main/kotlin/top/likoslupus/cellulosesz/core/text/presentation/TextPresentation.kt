package top.likoslupus.cellulosesz.core.text.presentation

import net.kyori.adventure.text.Component
import top.likoslupus.cellulosesz.core.text.i18n.LanguageResolver
import top.likoslupus.cellulosesz.core.text.i18n.LocalizedMessages
import top.likoslupus.cellulosesz.core.text.model.DetailView
import top.likoslupus.cellulosesz.core.text.model.ListPage
import top.likoslupus.cellulosesz.core.text.model.Notice
import top.likoslupus.cellulosesz.core.text.theme.TextTheme
import java.util.*

/**
 * The public presentation seam. Features build semantic values ([Notice], [ListPage], [DetailView])
 * and this renders them for a specific recipient's language. It never mutates business state.
 */
public interface TextPresentation {

    public fun renderFor(playerId: UUID?, notice: Notice): List<Component>

    public fun renderFor(playerId: UUID?, page: ListPage): List<Component>

    public fun renderFor(playerId: UUID?, detail: DetailView): List<Component>

}

/** Default implementation: resolves the recipient language, then delegates to the renderers. */
internal class DefaultTextPresentation(
    private val messages: LocalizedMessages,
    private val languages: LanguageResolver,
    private val theme: () -> TextTheme,
) : TextPresentation {

    private val notices = NoticeRenderer(messages)
    private val collections = CollectionRenderer(messages)
    private val details = DetailRenderer(messages)

    override fun renderFor(playerId: UUID?, notice: Notice): List<Component> =
        notices.render(context(playerId), notice)

    override fun renderFor(playerId: UUID?, page: ListPage): List<Component> =
        collections.render(context(playerId), page)

    override fun renderFor(playerId: UUID?, detail: DetailView): List<Component> =
        details.render(context(playerId), detail)

    private fun context(
        playerId: UUID?,
        channel: DisplayChannel = DisplayChannel.CHAT
    ): RenderContext =
        RenderContext(
            language = languages.effective(playerId),
            theme = theme(),
            channel = channel,
        )

}

/** The composition root's constructor for the presentation facade. */
public fun createTextPresentation(
    messages: LocalizedMessages,
    languages: LanguageResolver,
    theme: () -> TextTheme,
): TextPresentation =
    DefaultTextPresentation(messages, languages, theme)
