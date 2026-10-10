package top.likoslupus.cellulosesz.core.text.document

import net.kyori.adventure.text.format.TextColor
import top.likoslupus.cellulosesz.core.text.MessageTheme

/**
 * Colors for a rendered command document. It is derived from the existing message theme so a command
 * document and a command reply do not drift; no extra TOML keys are introduced for this phase.
 */
public class DocumentTheme(
    public val body: TextColor,
    public val heading: TextColor,
    public val code: TextColor,
    public val link: TextColor,
    public val divider: TextColor,
) {

    public companion object {

        public fun from(theme: MessageTheme): DocumentTheme =
            DocumentTheme(
                body = theme.primary,
                heading = theme.secondary,
                code = theme.secondary,
                link = theme.secondary,
                divider = theme.secondary,
            )

    }

}
