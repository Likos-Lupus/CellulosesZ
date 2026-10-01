package top.likoslupus.cellulosesz.movement.spawn

import kotlinx.serialization.Serializable
import top.likoslupus.cellulosesz.movement.teleport.StoredPosition

internal const val SPAWN_SCHEMA_VERSION: Int = 1

@Serializable
internal data class SpawnFile(
    val schemaVersion: Int = SPAWN_SCHEMA_VERSION,
    val spawn: StoredPosition? = null,
)
