package top.likoslupus.cellulosesz.core.text

import kotlinx.serialization.Serializable
import top.likoslupus.cellulosesz.foundation.config.ValidationError

/** Semantic rich-text colors. Translations express `primary`/`secondary`, never RGB values. */
@Serializable
public data class TextSettings(
    public val colors: TextColorSettings = TextColorSettings(),
)

@Serializable
public data class TextColorSettings(
    public val primary: String = "#5291df",
    public val secondary: String = "#f6c35d",
)

public object TextSettingsValidation {

    public fun validate(settings: TextSettings): List<ValidationError> =
        buildList {
            if (!isValidHexColor(settings.colors.primary)) {
                add(
                    ValidationError(
                        "text.colors.primary",
                        "must be a #RRGGBB color"
                    )
                )
            }
            if (!isValidHexColor(settings.colors.secondary)) {
                add(
                    ValidationError(
                        "text.colors.secondary",
                        "must be a #RRGGBB color"
                    )
                )
            }
        }

}
