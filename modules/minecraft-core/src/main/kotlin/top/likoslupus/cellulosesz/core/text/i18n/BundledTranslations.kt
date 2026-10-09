package top.likoslupus.cellulosesz.core.text.i18n

/**
 * The explicit bundled-language manifest. The project forbids classpath scanning, so adding a
 * language is an intentional code + resource change that the catalog validation test enforces.
 */
public object BundledTranslations {

    public val languages: List<LanguageId> = listOf(
        LanguageId.EN_US,
        LanguageId.ZH_CN,
    )

    /** The mandatory baseline; every key must exist here. */
    public val baseline: LanguageId = LanguageId.EN_US

    public fun resource(language: LanguageId): String =
        "/cellulosesz/i18n/messages_${language.value}.properties"

}
