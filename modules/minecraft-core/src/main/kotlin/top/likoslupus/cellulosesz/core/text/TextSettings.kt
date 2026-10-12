package top.likoslupus.cellulosesz.core.text

import kotlinx.serialization.Serializable
import top.likoslupus.cellulosesz.core.text.theme.TextThemePresets
import top.likoslupus.cellulosesz.foundation.config.ValidationError

/**
 * Presentation settings: a named preset, a density and optional per-token colour overrides. The
 * concrete palette lives in code ([top.likoslupus.cellulosesz.core.text.theme.TextThemePresets]);
 * configuration only selects and refines it.
 */
@Serializable
public data class TextSettings(
    public val theme: String = TextThemePresets.DEFAULT,
    public val density: String = "comfortable",
    public val colors: TextColorOverrides = TextColorOverrides(),
)

/**
 * Optional colour overrides. Every field is nullable so the selected preset remains authoritative
 * for unspecified tokens.
 */
@Serializable
public data class TextColorOverrides(
    public val body: String? = null,
    public val secondary: String? = null,
    public val muted: String? = null,
    public val brand: String? = null,
    public val heading: String? = null,
    public val value: String? = null,
    public val action: String? = null,
    public val success: String? = null,
    public val warning: String? = null,
    public val error: String? = null,
    public val code: String? = null,
    public val divider: String? = null,
)

public object TextSettingsValidation {

    private val DENSITY = setOf("comfortable", "compact")

    public fun validate(settings: TextSettings): List<ValidationError> =
        buildList {
            val colors = settings.colors

            if (settings.theme !in TextThemePresets.names) {
                add(
                    ValidationError(
                        "text.theme",
                        "must be one of ${TextThemePresets.names.joinToString(", ")}"
                    )
                )
            }
            if (settings.density !in DENSITY) {
                add(
                    ValidationError(
                        "text.density",
                        "must be one of comfortable, compact"
                    )
                )
            }

            addAll(colorErrors("text.colors.body", colors.body))
            addAll(colorErrors("text.colors.secondary", colors.secondary))
            addAll(colorErrors("text.colors.muted", colors.muted))
            addAll(colorErrors("text.colors.brand", colors.brand))
            addAll(colorErrors("text.colors.heading", colors.heading))
            addAll(colorErrors("text.colors.value", colors.value))
            addAll(colorErrors("text.colors.action", colors.action))
            addAll(colorErrors("text.colors.success", colors.success))
            addAll(colorErrors("text.colors.warning", colors.warning))
            addAll(colorErrors("text.colors.error", colors.error))
            addAll(colorErrors("text.colors.code", colors.code))
            addAll(colorErrors("text.colors.divider", colors.divider))
        }

    private fun colorErrors(path: String, value: String?): List<ValidationError> =
        when {
            value != null && !isValidHexColor(value) ->
                listOf(ValidationError(path, "must be a #RRGGBB color"))

            else -> emptyList()
        }

}
