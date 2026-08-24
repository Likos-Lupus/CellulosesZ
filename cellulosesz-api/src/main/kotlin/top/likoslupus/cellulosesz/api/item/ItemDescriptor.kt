package top.likoslupus.cellulosesz.api.item

import top.likoslupus.cellulosesz.api.validation.requireNonBlank
import top.likoslupus.cellulosesz.api.validation.requirePositive
import java.util.*

/** Registry-validated item input together with the requested business count. */
@JvmRecord
public data class ItemDescriptor(
    public val item: String,
    public val count: Int,
    public val argument: String
) {

    public constructor(item: String, count: Int) : this(
        normalizeItem(item),
        count,
        normalizeItem(item)
    )

    init {
        count.requirePositive { "count" }
    }

    public fun normalizedItem(): String = item

    public fun normalizedArgument(): String = argument

    public companion object {

        @JvmStatic
        public fun normalizeItem(value: String): String {
            val normalized = value
                    .trim()
                    .lowercase(Locale.ROOT)
                    .requireNonBlank { "item" }
            return if (normalized.indexOf(':') < 0) {
                "minecraft:$normalized"
            } else {
                normalized
            }
        }

    }

}
