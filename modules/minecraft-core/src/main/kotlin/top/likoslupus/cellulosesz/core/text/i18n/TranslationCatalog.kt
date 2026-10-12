package top.likoslupus.cellulosesz.core.text.i18n

import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.i18n.template.MessageTemplate
import top.likoslupus.cellulosesz.core.text.i18n.template.MessageTemplateParser
import top.likoslupus.cellulosesz.core.text.i18n.template.TemplateParseResult
import java.nio.charset.StandardCharsets
import java.util.*

/**
 * Immutable, bootstrap-compiled translation catalog assembled from explicit per-module sources. The
 * render path never re-parses a template, never scans the classpath and never queries storage.
 *
 * Construction fails fast: a missing en_us/zh_cn file, a duplicate key across sources, or an argument
 * placeholder set that differs between languages is a bootstrap error, never a runtime blank page.
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
        translations[language]?.keys ?: emptySet()

    public companion object {

        /** Loads the core-owned sources only; the composition root loads every module's sources. */
        public fun load(
            sources: List<TranslationSource>,
            languages: List<LanguageId> = BundledTranslations.languages,
        ): TranslationCatalog {
            require(languages.isNotEmpty()) {
                "at least one language is required"
            }

            val perLanguage = languages.associateWith { language ->
                mergeSources(sources, language)
            }

            validatePlaceholderParity(perLanguage)
            return TranslationCatalog(perLanguage)
        }

        private fun mergeSources(
            sources: List<TranslationSource>,
            language: LanguageId,
        ): Map<MessageKey, MessageTemplate> {
            val merged = LinkedHashMap<MessageKey, MessageTemplate>()
            sources.forEach { source ->
                val resource = source.resource(language)
                val properties = readProperties(source.anchor, resource)
                    ?: error("missing translation resource $resource (source '${source.name}')")
                properties.forEach { (key, raw) ->
                    if (merged.containsKey(key)) {
                        error("duplicate translation key '${key.value}' for language '${language.value}'")
                    }
                    merged[key] = compile(language, key, raw)
                }
            }
            return merged
        }

        private fun validatePlaceholderParity(
            perLanguage: Map<LanguageId, Map<MessageKey, MessageTemplate>>,
        ) {
            val allKeys = perLanguage.values
                    .flatMapTo(linkedSetOf()) {
                        it.keys
                    }
            allKeys.forEach {
                val expected = perLanguage.entries.first().value[it]?.argumentIndexes
                    ?: error("key '${it.value}' missing from language '${perLanguage.keys.first().value}'")
                perLanguage.forEach { (language, templates) ->
                    val template = templates[it]
                        ?: error("key '${it.value}' missing from language '${language.value}'")
                    if (template.argumentIndexes != expected) {
                        error(
                            "key '${it.value}' has placeholders ${template.argumentIndexes} in " +
                                    "'${language.value}' but $expected in the baseline"
                        )
                    }
                }
            }
        }

        private fun readProperties(
            anchor: Class<*>,
            resource: String,
        ): Map<MessageKey, String>? {
            val stream = anchor.getResourceAsStream(resource)
                ?: return null
            val properties = Properties()
            stream.bufferedReader(StandardCharsets.UTF_8)
                    .use(properties::load)
            return properties.stringPropertyNames()
                    .associate {
                        MessageKey(it) to properties.getProperty(it)
                    }
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
