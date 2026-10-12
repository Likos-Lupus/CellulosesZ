package top.likoslupus.cellulosesz.core.text.document

import net.kyori.adventure.text.format.TextColor
import top.likoslupus.cellulosesz.core.text.MessageTheme
import top.likoslupus.cellulosesz.core.text.theme.TextRole
import top.likoslupus.cellulosesz.core.text.theme.TextTheme

/**
 * Colors for a rendered command document, derived from the semantic palette so a document and a
 * command reply do not drift.
 */
public class DocumentTheme(
    public val body: TextColor,
    public val heading: TextColor,
    public val code: TextColor,
    public val link: TextColor,
    public val divider: TextColor,
    public val tip: TextColor,
    public val note: TextColor,
    public val warning: TextColor,
) {

    public companion object {

        public fun from(theme: TextTheme): DocumentTheme =
            DocumentTheme(
                body = theme.palette.color(TextRole.BODY),
                heading = theme.palette.color(TextRole.HEADING),
                code = theme.palette.color(TextRole.CODE),
                link = theme.palette.color(TextRole.ACTION),
                divider = theme.palette.color(TextRole.DIVIDER),
                tip = theme.palette.color(TextRole.SUCCESS),
                note = theme.palette.color(TextRole.ACTION),
                warning = theme.palette.color(TextRole.WARNING),
            )

        public fun from(theme: MessageTheme): DocumentTheme =
            DocumentTheme(
                body = theme.primary,
                heading = theme.secondary,
                code = theme.secondary,
                link = theme.secondary,
                divider = theme.secondary,
                tip = theme.secondary,
                note = theme.secondary,
                warning = theme.secondary,
            )

    }

}
