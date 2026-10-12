package top.likoslupus.cellulosesz.core.text.presentation

import top.likoslupus.cellulosesz.core.text.i18n.LanguageId
import top.likoslupus.cellulosesz.core.text.theme.TextTheme

/** Where a rendered message is delivered. Console cannot rely on hover or click. */
public enum class DisplayChannel {

    CHAT,
    ACTION_BAR,
    CONSOLE,

}

/**
 * A per-recipient, per-render value snapshot: the resolved language, an immutable theme and the
 * display channel. It never holds a live Minecraft object, a database handle or a coroutine.
 */
public data class RenderContext(
    public val language: LanguageId,
    public val theme: TextTheme,
    public val channel: DisplayChannel,
)
