package top.likoslupus.cellulosesz.movement.warp

import kotlinx.serialization.Serializable
import top.likoslupus.cellulosesz.movement.teleport.StoredPosition

internal const val WARP_SCHEMA_VERSION: Int = 1

@Serializable
internal data class WarpFile(
    val schemaVersion: Int = WARP_SCHEMA_VERSION,
    val warps: Map<String, StoredPosition> = emptyMap(),
)
