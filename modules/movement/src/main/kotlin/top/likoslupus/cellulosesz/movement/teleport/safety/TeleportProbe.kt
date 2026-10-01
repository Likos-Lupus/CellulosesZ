package top.likoslupus.cellulosesz.movement.teleport.safety

/** Outcome of classifying a single candidate standing space. */
internal enum class SurfaceKind {

    SAFE,
    BLOCKED,
    NO_FLOOR,
    LAVA,
    WATER,
    DANGEROUS,

}

/**
 * Read-only view of the world used by [SafeDestinationResolver]. It exists as an interface so the
 * resolver's ordering and bounds logic can be unit-tested without a running Minecraft server; the
 * production implementation is the only place that touches `ServerLevel`.
 */
internal interface TeleportProbe {

    fun isWithinBorder(x: Double, z: Double): Boolean

    fun isWithinHeight(y: Double): Boolean

    fun isChunkLoaded(blockX: Int, blockZ: Int): Boolean

    fun classify(x: Double, y: Double, z: Double): SurfaceKind

}
