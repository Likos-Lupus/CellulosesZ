package top.likoslupus.cellulosesz.api.teleport

import top.likoslupus.cellulosesz.api.validation.requireNonNegative

@JvmRecord
public data class TeleportOptions(
    public val safe: Boolean,
    public val rememberBack: Boolean,
    public val allowCrossWorld: Boolean,
    public val keepVehicle: Boolean,
    public val warmupSeconds: Int
) {

    init {
        warmupSeconds.requireNonNegative { "warmupSeconds" }
    }

    public fun withoutBackMemory(): TeleportOptions =
        TeleportOptions(
            safe,
            false,
            allowCrossWorld,
            keepVehicle,
            warmupSeconds
        )

    public fun withWarmup(seconds: Int): TeleportOptions =
        TeleportOptions(
            safe,
            rememberBack,
            allowCrossWorld,
            keepVehicle,
            seconds
        )

    public fun withSafe(value: Boolean): TeleportOptions =
        TeleportOptions(
            value,
            rememberBack,
            allowCrossWorld,
            keepVehicle,
            warmupSeconds
        )

    public companion object {

        @JvmStatic
        public fun defaults(): TeleportOptions =
            TeleportOptions(
                safe = true,
                rememberBack = true,
                allowCrossWorld = true,
                keepVehicle = false,
                warmupSeconds = 0
            )

    }

}
