package top.likoslupus.cellulosesz.utility.item

/** Pure validation rules for held-item name/lore editing and the hat eligibility rule. */
internal object ItemEditingRules {

    fun validateName(name: String, maxLength: Int): NameValidation =
        when {
            name.length > maxLength -> NameValidation.TooLong(maxLength)
            else -> NameValidation.Valid(name)
        }

    /**
     * Splits `|`-separated lore text into lines and enforces the configured caps. An empty [raw] is
     * handled by the caller as a clear; this only validates a non-null body.
     */
    fun parseLore(raw: String, maxLines: Int, maxLineLength: Int): LoreValidation {
        val lines = raw
                .split('|')
                .map { it.trim() }
        if (lines.size > maxLines) {
            return LoreValidation.TooManyLines(maxLines)
        }
        return when {
            lines.any { it.length > maxLineLength } -> LoreValidation.LineTooLong(maxLineLength)
            else -> LoreValidation.Valid(lines)
        }
    }

    /** An item may be worn as a hat when any item is allowed, or it has an equipment capability. */
    fun isHatAllowed(
        equipmentCapable: Boolean,
        allowAnyItemAsHat: Boolean
    ): Boolean =
        allowAnyItemAsHat || equipmentCapable

}

internal sealed interface NameValidation {

    data class Valid(val name: String) : NameValidation

    data class TooLong(val limit: Int) : NameValidation

}

internal sealed interface LoreValidation {

    data class Valid(val lines: List<String>) : LoreValidation

    data class TooManyLines(val limit: Int) : LoreValidation

    data class LineTooLong(val limit: Int) : LoreValidation

}
