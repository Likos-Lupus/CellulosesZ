package top.likoslupus.cellulosesz.core.text.i18n

import top.likoslupus.cellulosesz.core.text.i18n.preference.PlayerLanguagePreferences
import java.util.*

/**
 * Deterministic, server-owned locale selection: an explicit player override wins, otherwise the
 * configured server default. Console/RCON use the server default. The client locale is ignored.
 */
public class LanguageResolver(
    private val catalog: TranslationCatalog,
    private val preferences: PlayerLanguagePreferences,
    private val settings: () -> LocalizationSettings,
) {

    public fun serverDefault(): LanguageId =
        LanguageId.parse(settings().defaultLanguage)
            ?: error("invalid server default language: ${settings().defaultLanguage}")

    public fun effective(playerId: UUID?): LanguageId {
        val configuredDefault = serverDefault()
        if (playerId == null) {
            return configuredDefault
        }

        val explicit = preferences.explicit(playerId)
        return explicit?.takeIf(catalog::supports)
            ?: configuredDefault
    }

}
