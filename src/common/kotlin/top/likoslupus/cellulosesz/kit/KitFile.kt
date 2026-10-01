package top.likoslupus.cellulosesz.kit

import kotlinx.serialization.Serializable

internal const val KIT_SCHEMA_VERSION: Int = 1

@Serializable
internal data class KitFile(
    val schemaVersion: Int = KIT_SCHEMA_VERSION,
    val kits: Map<String, List<String>> = emptyMap(),
)
