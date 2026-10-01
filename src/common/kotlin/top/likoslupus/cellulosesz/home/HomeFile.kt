package top.likoslupus.cellulosesz.home

import kotlinx.serialization.Serializable
import top.likoslupus.cellulosesz.teleport.StoredPosition

internal const val HOME_SCHEMA_VERSION: Int = 1

@Serializable
internal data class HomeFile(
    val schemaVersion: Int = HOME_SCHEMA_VERSION,
    val homes: Map<String, StoredPosition> = emptyMap(),
)
