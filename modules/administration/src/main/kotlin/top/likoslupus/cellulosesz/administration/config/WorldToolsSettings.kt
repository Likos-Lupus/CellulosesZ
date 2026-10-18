package top.likoslupus.cellulosesz.administration.config

import kotlinx.serialization.Serializable

/** Caps for world/environment tools: `/nuke`, `/firework` and `/burn`. */
@Serializable
public data class WorldToolsSettings(
    /** Largest `/nuke` explosion radius, in blocks (each roughly 4x TNT power). */
    public val maxNukeRadius: Double = 16.0,
    /** Largest firework flight power accepted by `/firework fire`. */
    public val maxFireworkPower: Int = 4,
    /** Largest number of rockets one `/firework fire` call may launch. */
    public val maxFireworkAmount: Int = 8,
    /** Largest `/burn` duration, in seconds. */
    public val maxBurnSeconds: Double = 60.0,
)
