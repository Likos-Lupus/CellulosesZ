package top.likoslupus.cellulosesz.spawn

import kotlinx.serialization.Serializable
import top.likoslupus.cellulosesz.teleport.StoredPosition

internal const val SPAWN_SCHEMA_VERSION: Int = 1

@Serializable
internal data class SpawnFile(
    val schemaVersion: Int = SPAWN_SCHEMA_VERSION,
    val spawn: StoredPosition? = null,
)
