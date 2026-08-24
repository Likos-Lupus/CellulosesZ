package top.likoslupus.cellulosesz.api.teleport

@JvmRecord
public data class RandomTeleportSettings(
    public val centerX: Double,
    public val centerZ: Double,
    public val minRadius: Int,
    public val maxRadius: Int
) {

    init {
        require(centerX.isFinite() && centerZ.isFinite()) {
            "Random teleport center must be finite"
        }
        require(minRadius in 0..<maxRadius) {
            "Random teleport radii must satisfy 0 <= min < max"
        }
    }

}
