package top.likoslupus.cellulosesz.core.text.i18n

import top.likoslupus.cellulosesz.core.text.MessageKey

/** Keys owned by the core text foundation. Features define their own key objects when they migrate. */
public object CoreMessageKeys {

    public val LANGUAGE_CURRENT: MessageKey = MessageKey("core.language.current")
    public val LANGUAGE_CURRENT_INHERITED: MessageKey = MessageKey("core.language.current_inherited")
    public val LANGUAGE_CHANGED: MessageKey = MessageKey("core.language.changed")
    public val LANGUAGE_RESET: MessageKey = MessageKey("core.language.reset")
    public val LANGUAGE_UNKNOWN: MessageKey = MessageKey("core.language.unknown")
    public val LANGUAGE_AVAILABLE: MessageKey = MessageKey("core.language.available")
    public val LANGUAGE_STORAGE_FAILED: MessageKey = MessageKey("core.language.storage_failed")

    public val HELP_USAGE: MessageKey = MessageKey("help.usage")
    public val HELP_PAGE: MessageKey = MessageKey("help.page")
    public val HELP_DOCUMENT_MISSING: MessageKey = MessageKey("help.document.missing")
    public val HELP_DOCUMENT_RESTRICTED: MessageKey = MessageKey("help.document.restricted")
    public val HELP_DOCUMENT_INVALID_PAGE: MessageKey = MessageKey("help.document.invalid_page")

}
