package top.likoslupus.cellulosesz.movement.teleport.history

import kotlinx.serialization.Serializable
import top.likoslupus.cellulosesz.movement.teleport.StoredPosition

internal const val TELEPORT_HISTORY_SCHEMA_VERSION: Int = 1

@Serializable
internal data class TeleportHistoryFile(
    val schemaVersion: Int = TELEPORT_HISTORY_SCHEMA_VERSION,
    val previous: StoredPosition? = null,
)
