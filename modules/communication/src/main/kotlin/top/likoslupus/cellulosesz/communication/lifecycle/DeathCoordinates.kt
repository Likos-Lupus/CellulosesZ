package top.likoslupus.cellulosesz.communication.lifecycle

import kotlin.math.floor

/** Pure formatting rules for the death-coordinate line. */
internal object DeathCoordinates {

    /** Block coordinate of a world position (floor), so a death at y=63.9 reads as 63. */
    fun coordinate(value: Double): String =
        floor(value).toInt().toString()

    /** Resolves `{WORLD}`, `{X}`, `{Y}` and `{Z}` in a death-coordinate template. */
    fun resolve(
        template: String,
        world: String,
        x: Double,
        y: Double,
        z: Double,
    ): String =
        JoinQuitRenderer.resolve(
            template,
            mapOf(
                "WORLD" to world,
                "X" to coordinate(x),
                "Y" to coordinate(y),
                "Z" to coordinate(z),
            ),
        )

}
