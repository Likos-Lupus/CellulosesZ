package top.likoslupus.cellulosesz.administration.config

import kotlinx.serialization.Serializable

/** Caps for entity tools: `/spawnmob` and `/remove`. */
@Serializable
public data class EntityToolsSettings(
    /** Maximum amount one `/spawnmob` call may spawn. */
    public val maxSpawnAmount: Int = 64,
    /** Maximum radius accepted by `/remove <selector> <radius>`. */
    public val maxRemoveRadius: Double = 128.0,
)
