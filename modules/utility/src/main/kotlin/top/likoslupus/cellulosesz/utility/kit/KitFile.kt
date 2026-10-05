package top.likoslupus.cellulosesz.utility.kit

import kotlinx.serialization.Serializable

internal const val KIT_SCHEMA_VERSION: Int = 2

/** Machine-data safety caps, independent of the (admin-tunable) config limits. */
internal const val MAX_STORED_KITS: Int = 10_000
internal const val MAX_STORED_ITEMS_PER_KIT: Int = 1_000
internal const val MAX_ITEM_PAYLOAD_CHARS: Int = 1_048_576

@Serializable
internal enum class KitReuseMode {

    ALWAYS,
    ONCE,
    COOLDOWN,

}

/**
 * File representation of a reuse policy. The sealed domain [KitReusePolicy] is not serialized
 * directly; validating this flat shape keeps machine data stable.
 */
@Serializable
internal data class KitDefinitionFile(
    val id: String,
    val reuse: KitReuseMode = KitReuseMode.ALWAYS,
    val cooldownSeconds: Long? = null,
    val items: List<String> = emptyList(),
)

@Serializable
internal data class KitFile(
    val schemaVersion: Int = KIT_SCHEMA_VERSION,
    val kits: Map<String, KitDefinitionFile> = emptyMap(),
)

/** Legacy v1 layout, retained only for one-way migration into [KitFile]. */
@Serializable
internal data class LegacyKitFile(
    val schemaVersion: Int = 1,
    val kits: Map<String, List<String>> = emptyMap(),
)
