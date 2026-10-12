package top.likoslupus.cellulosesz.core.text.i18n

/**
 * One explicitly declared translation resource owner. The project forbids classpath scanning, so an
 * owner is a named class anchor plus the conventional resource path. The composition root lists the
 * anchors of every module that owns translations.
 */
public data class TranslationSource(
    public val name: String,
    public val anchor: Class<*>,
) {

    init {
        require(NAME_PATTERN.matches(name)) {
            "invalid translation source name: $name"
        }
    }

    public fun resource(language: LanguageId): String =
        "/cellulosesz/i18n/$name/messages_${language.value}.properties"

    private companion object {

        private val NAME_PATTERN = Regex("[a-z][a-z0-9_-]*")

    }

}
