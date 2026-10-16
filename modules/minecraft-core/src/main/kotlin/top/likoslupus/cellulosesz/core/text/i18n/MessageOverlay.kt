package top.likoslupus.cellulosesz.core.text.i18n

import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.i18n.template.MessageTemplate

/**
 * A read-only overlay consulted before the bundled catalog. It exists so runtime-registered
 * translations (for example ones contributed by an addon) can be rendered through the ordinary
 * per-recipient pipeline without mutating the immutable bootstrap catalog.
 */
public interface MessageOverlay {

    public fun template(language: LanguageId, key: MessageKey): MessageTemplate?

    public companion object {

        public val Empty: MessageOverlay = object : MessageOverlay {

            override fun template(language: LanguageId, key: MessageKey): MessageTemplate? = null

        }

    }

}
