package top.likoslupus.cellulosesz.warp

import kotlinx.serialization.Serializable
import top.likoslupus.cellulosesz.teleport.StoredPosition

internal const val WARP_SCHEMA_VERSION: Int = 1

@Serializable
internal data class WarpFile(
    val schemaVersion: Int = WARP_SCHEMA_VERSION,
    val warps: Map<String, StoredPosition> = emptyMap(),
)
