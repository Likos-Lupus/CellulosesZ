package top.likoslupus.cellulosesz.administration.config

import kotlinx.serialization.Serializable

/** Caps for world/environment tools: `/firework` and `/burn`. */
@Serializable
public data class WorldToolsSettings(
    /** Largest firework flight power accepted by `/firework fire`. */
    public val maxFireworkPower: Int = 4,
    /** Largest number of rockets one `/firework fire` call may launch. */
    public val maxFireworkAmount: Int = 8,
    /** Largest `/burn` duration, in seconds. */
    public val maxBurnSeconds: Double = 60.0,
    /** Largest distance `/tree` and `/bigtree` may target, in blocks. */
    public val treeCommandRangeLimit: Double = 300.0,
    /** Largest projectile speed accepted by `/fireball`, in blocks per tick. */
    public val maxProjectileSpeed: Double = 8.0,
)
