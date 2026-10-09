package top.likoslupus.cellulosesz.core.text.i18n

import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.i18n.template.MessageTemplate
import top.likoslupus.cellulosesz.core.text.i18n.template.MessageTemplateParser
import top.likoslupus.cellulosesz.core.text.i18n.template.TemplateParseResult
import java.nio.charset.StandardCharsets
import java.util.*

/**
 * Immutable, bootstrap-compiled translation catalog. The render path never re-parses a template and
 * never queries storage. Constructed once by the composition root and injected, never a global.
 */
public class TranslationCatalog private constructor(
    private val translations: Map<LanguageId, Map<MessageKey, MessageTemplate>>,
) {

    public val languages: Set<LanguageId>
        get() = translations.keys

    public fun supports(language: LanguageId): Boolean =
        language in translations

    internal fun template(
        language: LanguageId,
        key: MessageKey,
    ): MessageTemplate? =
        translations[language]?.get(key)

    internal fun keys(language: LanguageId): Set<MessageKey> =
        translations[language]?.keys
            ?: emptySet()

    public companion object {

        public fun loadBundled(): TranslationCatalog =
            load(BundledTranslations.languages)

        /** Loads an explicit language list; tests use this to validate resources directly. */
        internal fun load(languages: List<LanguageId>): TranslationCatalog {
            val compiled = languages.associateWith { language ->
                loadProperties(BundledTranslations.resource(language))
                        .mapValues { (key, raw) -> compile(language, key, raw) }
            }
            return TranslationCatalog(compiled)
        }

        private fun loadProperties(resource: String): Map<MessageKey, String> {
            val stream = TranslationCatalog::class.java.getResourceAsStream(resource)
                ?: error("missing bundled translation resource $resource")

            val properties = Properties()
            stream.bufferedReader(StandardCharsets.UTF_8)
                    .use(properties::load)

            return properties.stringPropertyNames()
                    .associate { name -> MessageKey(name) to properties.getProperty(name) }
        }

        private fun compile(
            language: LanguageId,
            key: MessageKey,
            raw: String,
        ): MessageTemplate =
            when (val result = MessageTemplateParser.parse(raw)) {
                is TemplateParseResult.Success -> result.template
                is TemplateParseResult.Failure -> error(
                    "invalid template for '${key.value}' in '${language.value}' at offset " +
                            "${result.offset}: ${result.reason}"
                )
            }

    }

}
