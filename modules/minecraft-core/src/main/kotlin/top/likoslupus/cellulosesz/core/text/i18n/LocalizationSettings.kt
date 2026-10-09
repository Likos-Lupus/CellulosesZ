package top.likoslupus.cellulosesz.core.text.i18n

import kotlinx.serialization.Serializable
import top.likoslupus.cellulosesz.foundation.config.ValidationError

/** Server-owned locale policy. The Minecraft client locale is deliberately not consulted. */
@Serializable
public data class LocalizationSettings(
    public val defaultLanguage: String = "en_us",
)

public object LocalizationSettingsValidation {

    public fun validate(settings: LocalizationSettings): List<ValidationError> {
        val language = LanguageId.parse(settings.defaultLanguage)
            ?: return listOf(
                ValidationError(
                    "localization.defaultLanguage",
                    "must be a language id such as en_us"
                )
            )
        if (!BundledTranslations.languages.contains(language)) {
            return listOf(
                ValidationError(
                    "localization.defaultLanguage",
                    "language '${language.value}' is not bundled"
                )
            )
        }
        return emptyList()
    }

}
